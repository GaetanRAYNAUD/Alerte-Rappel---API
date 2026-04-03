package fr.graynaud.alerterappel.api.service.alert;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class AlertRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertRepository.class);

    private final Path path;

    private final Path lockPath;

    private final JsonMapper jsonMapper;

    public AlertRepository(DataProperties dataProperties, JsonMapper jsonMapper) throws IOException {
        this.path = dataProperties.getDataPath();
        this.lockPath = this.path.resolveSibling(this.path.getFileName() + ".lock");
        this.jsonMapper = jsonMapper;
    }

    public Map<String, Alert> loadAll() throws Exception {
        LOGGER.info("Loading alerts from {}", this.path);
        try (FileChannel channel = FileChannel.open(this.lockPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock lock = channel.lock(0, Long.MAX_VALUE, true)) {
            Map<String, Alert> loaded = new HashMap<>();
            List<Alert> alerts = this.jsonMapper.readValue(this.path.toFile(), new TypeReference<List<Alert>>() {});
            for (Alert alert : alerts) {
                loaded.put(alert.alertNumber(), alert);
            }
            return loaded;
        }
    }

    public void saveAll(Collection<Alert> alerts) throws IOException {
        try (FileChannel channel = FileChannel.open(this.lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {
            this.jsonMapper.writeValue(this.path.toFile(), alerts);
            LOGGER.info("Persisted {} alerts to {}", alerts.size(), this.path);
        }
    }

    public Path getPath() {
        return path;
    }
}
