package fr.graynaud.alerterappel.api.service.alert;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertService implements ApplicationListener<ApplicationStartedEvent>, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertService.class);

    private final Path path;

    private final Path lockPath;

    private volatile Map<String, Alert> alerts = new ConcurrentHashMap<>();

    private final JsonMapper jsonMapper;

    private final WatchService watchService;

    public AlertService(DataProperties dataProperties, JsonMapper jsonMapper) throws IOException {
        this.path = dataProperties.getDataPath();
        this.lockPath = this.path.resolveSibling(this.path.getFileName() + ".lock");
        this.jsonMapper = jsonMapper;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.path.getParent().register(this.watchService, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        loadAlerts();
        startFileWatcher();
    }

    @Override
    public boolean supportsAsyncExecution() {
        return false;
    }

    @Override
    public void destroy() throws Exception {
        this.watchService.close();
    }

    public void addAlerts(List<Alert> alerts) {
        if (CollectionUtils.isEmpty(alerts)) {
            return;
        }

        for (Alert alert : alerts) {
            this.alerts.put(alert.alertNumber(), alert);
        }
        LOGGER.info("Added {} alerts, total: {}", alerts.size(), this.alerts.size());

        persist();
    }

    private void loadAlerts() {
        LOGGER.info("Loading alerts from {}", this.path);
        try (FileChannel channel = FileChannel.open(this.lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.lock(0, Long.MAX_VALUE, true)) {
            Map<String, Alert> loaded = new HashMap<>();

            for (Alert alert : this.jsonMapper.readValue(this.path, new TypeReference<List<Alert>>() {})) {
                loaded.put(alert.alertNumber(), alert);
            }

            this.alerts = new ConcurrentHashMap<>(loaded);
            LOGGER.info("Loaded {} alerts", this.alerts.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load alerts from {}", this.path, e);
        }
    }

    private void persist() {
        try (FileChannel channel = FileChannel.open(this.lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {
            this.jsonMapper.writerWithDefaultPrettyPrinter().writeValue(this.path.toFile(), this.alerts.values());
            LOGGER.info("Persisted {} alerts to {}", this.alerts.size(), this.path);
        } catch (IOException e) {
            LOGGER.error("Failed to persist alerts", e);
        }
    }

    private void startFileWatcher() {
        String fileName = this.path.getFileName().toString();

        Thread.ofVirtual().name("alert-file-watcher").start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = this.watchService.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Path changed = (Path) event.context();
                    if (fileName.equals(changed.toString())) {
                        LOGGER.info("Detected change on {}, reloading alerts", this.path);
                        loadAlerts();
                    }
                }

                if (!key.reset()) {
                    LOGGER.warn("Watch key for {} is no longer valid", this.path.getParent());
                    break;
                }
            }
        });
    }
}
