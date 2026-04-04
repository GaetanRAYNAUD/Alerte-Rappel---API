package fr.graynaud.alerterappel.api.config.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DataPropertiesTest {

    @TempDir
    Path tempDir;

    @Test
    void setPathCreatesDirectories() throws Exception {
        Path nested = this.tempDir.resolve("a").resolve("b");
        DataProperties properties = new DataProperties();

        properties.setPath(nested.toString());

        assertEquals(nested.toAbsolutePath(), properties.getPath());
        assertTrue(Files.isDirectory(nested));
        assertTrue(Files.isDirectory(nested.resolve("source")));
    }

    @Test
    void setPathCreatesDataJsonIfMissing() throws Exception {
        DataProperties properties = new DataProperties();

        properties.setPath(this.tempDir.toString());

        Path dataJson = this.tempDir.resolve("data.json");
        assertTrue(Files.exists(dataJson));
        assertEquals("[]", Files.readString(dataJson));
    }

    @Test
    void getDataPathCreatesFileWhenMissing() throws Exception {
        DataProperties properties = new DataProperties();
        properties.setPath(this.tempDir.toString());

        Path dataJson = this.tempDir.resolve("data.json");
        Files.delete(dataJson);
        assertFalse(Files.exists(dataJson));

        Path result = properties.getDataPath();

        assertTrue(Files.exists(result));
        assertEquals("[]", Files.readString(result));
    }

    @Test
    void getDataPathDoesNotOverwriteExistingFile() throws Exception {
        Path dataJson = this.tempDir.resolve("data.json");
        Files.writeString(dataJson, "[{\"id\":1}]");
        DataProperties properties = new DataProperties();
        properties.setPath(this.tempDir.toString());

        Path result = properties.getDataPath();

        assertEquals(dataJson.toAbsolutePath(), result);
        assertEquals("[{\"id\":1}]", Files.readString(result));
    }

    @Test
    void getSourcePathReturnsSourceSubdirectory() throws Exception {
        DataProperties properties = new DataProperties();
        properties.setPath(this.tempDir.toString());

        Path sourcePath = properties.getSourcePath();

        assertEquals(this.tempDir.toAbsolutePath().resolve("source"), sourcePath);
    }

    @Test
    void getSourcePathWithPropertiesSanitizesName() throws Exception {
        DataProperties properties = new DataProperties();
        properties.setPath(this.tempDir.toString());

        SourceProperties sourceProperties = createSourceProperties("Rappel Conso");
        Path result = properties.getSourcePath(sourceProperties);

        assertEquals(this.tempDir.toAbsolutePath().resolve("source").resolve("rappel_conso.json"), result);
        assertTrue(Files.exists(result));
        assertEquals("{}", Files.readString(result));
    }

    @Test
    void getSourcePathWithPropertiesStripsAccents() throws Exception {
        DataProperties properties = new DataProperties();
        properties.setPath(this.tempDir.toString());

        SourceProperties sourceProperties = createSourceProperties("Données élémentaires");
        Path result = properties.getSourcePath(sourceProperties);

        assertEquals(this.tempDir.toAbsolutePath().resolve("source").resolve("donnees_elementaires.json"), result);
    }

    @Test
    void getSourcePathWithPropertiesStripsSpecialCharacters() throws Exception {
        DataProperties properties = new DataProperties();
        properties.setPath(this.tempDir.toString());

        SourceProperties sourceProperties = createSourceProperties("source@v2!test");
        Path result = properties.getSourcePath(sourceProperties);

        assertEquals(this.tempDir.toAbsolutePath().resolve("source").resolve("source_v2_test.json"), result);
    }

    @Test
    void getSourcePathWithPropertiesCreatesFileWhenMissing() throws Exception {
        DataProperties properties = new DataProperties();
        properties.setPath(this.tempDir.toString());

        SourceProperties sourceProperties = createSourceProperties("NewSource");
        Path expected = this.tempDir.toAbsolutePath().resolve("source").resolve("newsource.json");
        assertFalse(Files.exists(expected));

        Path result = properties.getSourcePath(sourceProperties);

        assertTrue(Files.exists(result));
        assertEquals("{}", Files.readString(result));
    }

    @Test
    void getSourcePathWithPropertiesDoesNotOverwriteExistingFile() throws Exception {
        DataProperties properties = new DataProperties();
        properties.setPath(this.tempDir.toString());

        Path existing = this.tempDir.resolve("source").resolve("my_source.json");
        Files.writeString(existing, "{\"lastDate\":\"2026-01-01\"}");

        SourceProperties sourceProperties = createSourceProperties("My Source");
        Path result = properties.getSourcePath(sourceProperties);

        assertEquals("{\"lastDate\":\"2026-01-01\"}", Files.readString(result));
    }

    @Test
    void getSourcePathWithPropertiesPreservesDotAndHyphen() throws Exception {
        DataProperties properties = new DataProperties();
        properties.setPath(this.tempDir.toString());

        SourceProperties sourceProperties = createSourceProperties("my-source.v2");
        Path result = properties.getSourcePath(sourceProperties);

        assertEquals(this.tempDir.toAbsolutePath().resolve("source").resolve("my-source.v2.json"), result);
    }

    private static SourceProperties createSourceProperties(String name) {
        SourceProperties sourceProperties = new SourceProperties() {};
        sourceProperties.setName(name);
        return sourceProperties;
    }
}
