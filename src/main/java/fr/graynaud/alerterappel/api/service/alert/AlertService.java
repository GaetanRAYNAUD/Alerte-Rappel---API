package fr.graynaud.alerterappel.api.service.alert;

import fr.graynaud.alerterappel.api.controller.dto.PageResponse;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.alert.dto.SearchResult;
import fr.graynaud.alerterappel.api.service.alert.dto.SearchSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AlertService implements ApplicationListener<ApplicationStartedEvent>, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository repository;

    private final AlertMerger merger;

    private final AlertSearchIndex searchIndex;

    private final SuggestionIndex suggestionIndex;

    private final WatchService watchService;

    private final AtomicBoolean selfWrite = new AtomicBoolean(false);

    private volatile Map<String, Alert> alerts = new ConcurrentHashMap<>();

    private volatile Map<String, Alert> barcodeIndex = new ConcurrentHashMap<>();

    private volatile List<Alert> sortedByDate = List.of();

    public AlertService(AlertRepository repository, AlertMerger merger, AlertSearchIndex searchIndex, SuggestionIndex suggestionIndex) throws IOException {
        this.repository = repository;
        this.merger = merger;
        this.searchIndex = searchIndex;
        this.suggestionIndex = suggestionIndex;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.repository.getPath().getParent().register(this.watchService, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        loadAndIndex();
        startFileWatcher();
    }

    @Override
    public void destroy() throws Exception {
        this.watchService.close();
    }

    public Optional<Alert> findByAlertNumber(String alertNumber) {
        return Optional.ofNullable(this.alerts.get(alertNumber.toUpperCase()));
    }

    public Optional<Alert> findByBarcode(String barcode) {
        return Optional.ofNullable(this.barcodeIndex.get(barcode));
    }

    public PageResponse<Alert> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }

        SearchResult result = this.searchIndex.search(query, page, size);
        List<Alert> content = new ArrayList<>();
        for (String alertNumber : result.alertNumbers()) {
            Alert alert = this.alerts.get(alertNumber);
            if (alert != null) {
                content.add(alert);
            }
        }

        return PageResponse.from(content, page, size, result.totalHits());
    }

    public PageResponse<Alert> getLatest(int page, int size) {
        return PageResponse.from(this.sortedByDate, page, size);
    }

    public List<SearchSuggestion> suggest(String query) {
        return this.suggestionIndex.suggest(query);
    }

    public synchronized void addAlerts(List<Alert> alerts) {
        if (CollectionUtils.isEmpty(alerts)) {
            return;
        }

        int merged = 0;
        for (Alert alert : alerts) {
            Alert existing = this.alerts.get(alert.alertNumber());
            if (existing != null) {
                this.alerts.put(alert.alertNumber(), this.merger.mergeAlerts(existing, alert));
                merged++;
            } else {
                this.alerts.put(alert.alertNumber(), alert);
            }
        }
        LOGGER.info("Added {} alerts ({} merged), total: {}", alerts.size(), merged, this.alerts.size());

        rebuildIndexes();
        persist();
    }

    private void loadAndIndex() {
        try {
            this.alerts = new ConcurrentHashMap<>(repository.loadAll());
            rebuildIndexes();
            LOGGER.info("Loaded {} alerts", this.alerts.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load alerts", e);
        }
    }

    private void rebuildIndexes() {
        rebuildBarcodeIndex();
        rebuildSortedByDate();
        rebuildSearchIndex();
        rebuildSuggestionIndex();
    }

    private void rebuildSortedByDate() {
        List<Alert> sorted = new ArrayList<>(this.alerts.values());
        sorted.sort(Comparator.comparing(Alert::publicationDate, Comparator.nullsLast(Comparator.reverseOrder())));
        this.sortedByDate = Collections.unmodifiableList(sorted);
    }

    private void rebuildSearchIndex() {
        this.searchIndex.rebuild(this.alerts);
    }

    private void rebuildSuggestionIndex() {
        this.suggestionIndex.rebuild(this.alerts);
    }

    private void rebuildBarcodeIndex() {
        Map<String, Alert> index = new ConcurrentHashMap<>();
        for (Alert alert : this.alerts.values()) {
            if (alert.product() != null && !CollectionUtils.isEmpty(alert.product().barcodes())) {
                for (String barcode : alert.product().barcodes()) {
                    Alert existing = index.get(barcode);
                    if (existing == null || (alert.publicationDate() != null &&
                                             (existing.publicationDate() == null || alert.publicationDate().isAfter(existing.publicationDate())))) {
                        index.put(barcode, alert);
                    }
                }
            }
        }
        this.barcodeIndex = index;
    }

    private void persist() {
        this.selfWrite.set(true);
        try {
            this.repository.saveAll(this.alerts.values());
        } catch (IOException e) {
            LOGGER.error("Failed to persist alerts", e);
        } finally {
            this.selfWrite.set(false);
        }
    }

    private void startFileWatcher() {
        String fileName = this.repository.getPath().getFileName().toString();
        Thread.ofVirtual().name("alert-file-watcher").start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try { key = this.watchService.take(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                    Path changed = (Path) event.context();
                    if (fileName.equals(changed.toString())) {
                        if (this.selfWrite.get()) continue;
                        LOGGER.info("Detected change on {}, reloading alerts", this.repository.getPath());
                        loadAndIndex();
                    }
                }
                if (!key.reset()) break;
            }
        });
    }
}
