package fr.graynaud.alerterappel.api.config.properties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import org.apache.commons.io.file.PathUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "data")
public class DataProperties {

    private Path path;

    public Path getPath() {
        return path;
    }

    public void setPath(String path) throws IOException {
        this.path = Path.of(path).toAbsolutePath();
        Files.createDirectories(this.path);
        Files.createDirectories(getSourcePath());
    }

    public Path getSourcePath() {
        return this.path.resolve("source");
    }

    public Path getSourcePath(SourceProperties sourceProperties) throws IOException {
        String sanitized = Normalizer.normalize(sourceProperties.getName(), Normalizer.Form.NFD)
                                     .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
                                     .replaceAll("[^a-zA-Z0-9._-]", "_")
                                     .toLowerCase();
        Path path = getSourcePath().resolve(sanitized + ".json");

        if (!Files.exists(path)) {
            Files.writeString(path, "{}");
        }

        return path;
    }
}
