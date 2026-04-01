package fr.graynaud.alerterappel.api.service.source.explore21;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.config.properties.Explore21Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public abstract class Explore21Service<D extends Explore21Source> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String HEADER_RATELIMIT_LIMIT = "x-ratelimit-limit";

    private static final String HEADER_RATELIMIT_REMAINING = "x-ratelimit-remaining";

    private static final String HEADER_RATELIMIT_RESET = "x-ratelimit-reset";

    private static final DateTimeFormatter RESET_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxxx");

    private static final DateTimeFormatter RESET_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    protected final RestClient updateClient;

    protected final Path dataPath;

    private final Path lockPath;

    protected final JsonMapper jsonMapper;

    protected final String sourceName;

    protected final Class<D> dataClass;

    protected final String dateField;

    private final ReentrantLock processLock = new ReentrantLock();

    protected final Environment environment;

    protected Explore21Service(RestClient.Builder restClientBuilder, Explore21Properties properties, DataProperties dataProperties,
                               JsonMapper jsonMapper, TaskScheduler taskScheduler, String sourceName, Class<D> dataClass, String dateField,
                               Environment environment) throws IOException {
        this.environment = environment;
        this.updateClient = properties.restClientBuilder(restClientBuilder)
                                      .requestInterceptor((request, body, execution) -> {
                                          ClientHttpResponse response = execution.execute(request, body);
                                          logRateLimitHeaders(response.getHeaders());
                                          return response;
                                      })
                                      .build();
        this.dataPath = dataProperties.getSourcePath(properties);
        this.lockPath = this.dataPath.resolveSibling(this.dataPath.getFileName() + ".lock");
        this.jsonMapper = jsonMapper;
        this.sourceName = sourceName;
        this.dataClass = dataClass;
        this.dateField = dateField;
        taskScheduler.schedule(this::checkNewData, new CronTrigger(properties.getCron()));
    }

    public void checkNewData() {
        this.processLock.lock();
        try (FileChannel channel = FileChannel.open(this.lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.tryLock()) {
            if (lock == null) {
                this.logger.info("Skipping checkNewData for {} — another instance holds the lock", this.sourceName);
                return;
            }
            doCheckNewData();
        } catch (IOException e) {
            this.logger.error("Failed to acquire lock for {}", this.sourceName, e);
        } finally {
            this.processLock.unlock();
        }
    }

    private void doCheckNewData() {
        try {
            D data = this.jsonMapper.readValue(this.dataPath.toFile(), this.dataClass);
            OffsetDateTime lastDate = data.getLastDate();

            if (this.environment.matchesProfiles("local")) {
                if (lastDate == null || lastDate.isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusYears(1))) {
                    lastDate = OffsetDateTime.now(ZoneOffset.UTC).minusYears(1);
                }
            }

            Explore21Response<?> response = fetchLatestSince(lastDate);

            if (response != null && response.totalCount() != null && response.totalCount() > 0) {
                this.logger.info("New data for {}: {} record(s) since {}", this.sourceName, response.totalCount(), lastDate);
                handleNewData(lastDate, data);
            } else {
                this.logger.info("No new data for {} since {}", this.sourceName, lastDate);
            }
        } catch (Exception e) {
            this.logger.error("Error while checking new data for {}", this.sourceName, e);
        }
    }

    private Explore21Response<?> fetchLatestSince(OffsetDateTime lastDate) {
        String where = lastDate == null ? this.dateField + " is not null" : this.dateField + " > \"" + lastDate + "\"";

        return this.updateClient.get()
                                .uri(b -> b.path("/records")
                                           .queryParam("where", where)
                                           .queryParam("limit", 0)
                                           .build())
                                .retrieve()
                                .body(new ParameterizedTypeReference<>() {});
    }

    private void logRateLimitHeaders(HttpHeaders headers) {
        String limitStr = headers.getFirst(HEADER_RATELIMIT_LIMIT);
        String remainingStr = headers.getFirst(HEADER_RATELIMIT_REMAINING);
        String resetStr = headers.getFirst(HEADER_RATELIMIT_RESET);

        if (limitStr == null || remainingStr == null) {
            return;
        }

        try {
            long used = Long.parseLong(limitStr.trim()) - Long.parseLong(remainingStr.trim());
            this.logger.info("[{}] Rate limit: {} remaining (used {} / {}) — resets {}",
                             this.sourceName,
                             formatNumber(remainingStr),
                             String.format(Locale.ENGLISH, "%,d", used),
                             formatNumber(limitStr),
                             formatReset(resetStr));
        } catch (NumberFormatException e) {
            this.logger.info("[{}] Rate limit: remaining={}, limit={}, resets={}", this.sourceName, remainingStr, limitStr, resetStr);
        }
    }

    private String formatNumber(String value) {
        if (value == null) {
            return "?";
        }

        try {
            return String.format(Locale.ENGLISH, "%,d", Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private String formatReset(String value) {
        if (value == null) {
            return "?";
        }

        try {
            OffsetDateTime reset = OffsetDateTime.parse(value.trim(), RESET_PARSER);
            Duration until = Duration.between(OffsetDateTime.now(ZoneOffset.UTC), reset);
            String resetFormatted = reset.atZoneSameInstant(ZoneId.of("UTC")).format(RESET_DISPLAY);
            if (until.isNegative()) {
                return "overdue (%s)".formatted(resetFormatted);
            }
            return "in %dh %02dm (%s)".formatted(until.toHours(), until.toMinutesPart(), resetFormatted);
        } catch (Exception e) {
            return value;
        }
    }

    protected abstract void handleNewData(OffsetDateTime since, D data);

    protected void persist(D data) {
        if (this.processLock.isHeldByCurrentThread()) {
            this.jsonMapper.writeValue(this.dataPath.toFile(), data);
        } else {
            this.processLock.lock();
            try (FileChannel channel = FileChannel.open(this.lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock lock = channel.lock()) {
                this.jsonMapper.writeValue(this.dataPath.toFile(), data);
            } catch (IOException e) {
                throw new RuntimeException("Failed to acquire lock for persist on " + this.sourceName, e);
            } finally {
                this.processLock.unlock();
            }
        }
    }
}
