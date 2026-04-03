package fr.graynaud.alerterappel.api.service.alert;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadata;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadataSource;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertRepositoryTest {

    @TempDir
    Path tempDir;

    private AlertRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataProperties dataProperties = new DataProperties();
        dataProperties.setPath(this.tempDir.toString());
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
        this.repository = new AlertRepository(dataProperties, jsonMapper);
    }

    @Test
    void saveAllWritesJsonFile() throws Exception {
        List<Alert> alerts = List.of(
                alert("SR/00001/26", "Poupée", "JOURDAIN"),
                alert("SR/00002/26", "Voiture", "BMW")
        );

        this.repository.saveAll(alerts);

        Path dataFile = this.tempDir.resolve("data.json");
        assertTrue(Files.exists(dataFile));
        String content = Files.readString(dataFile);
        assertTrue(content.contains("SR/00001/26"));
        assertTrue(content.contains("SR/00002/26"));
        assertTrue(content.contains("JOURDAIN"));
        assertTrue(content.contains("BMW"));
    }

    @Test
    void saveAllThenLoadAllRoundTrip() throws Exception {
        List<Alert> alerts = List.of(
                alert("SR/00001/26", "Poupée en plastique", "JOURDAIN"),
                alert("SR/00002/26", "Voiture électrique", "BMW")
        );

        this.repository.saveAll(alerts);
        Map<String, Alert> loaded = this.repository.loadAll();

        assertEquals(2, loaded.size());
        assertTrue(loaded.containsKey("SR/00001/26"));
        assertTrue(loaded.containsKey("SR/00002/26"));
        assertEquals("Poupée en plastique", loaded.get("SR/00001/26").product().specificName());
        assertEquals("BMW", loaded.get("SR/00002/26").product().brand());
    }

    @Test
    void saveAllOverwritesPreviousContent() throws Exception {
        this.repository.saveAll(List.of(alert("SR/00001/26", "Ancien", "OLD")));
        this.repository.saveAll(List.of(alert("SR/00002/26", "Nouveau", "NEW")));

        Map<String, Alert> loaded = this.repository.loadAll();
        assertEquals(1, loaded.size());
        assertFalse(loaded.containsKey("SR/00001/26"));
        assertTrue(loaded.containsKey("SR/00002/26"));
    }

    @Test
    void saveAllEmptyList() throws Exception {
        this.repository.saveAll(List.of(alert("SR/00001/26", "Test", "BRAND")));
        this.repository.saveAll(List.of());

        Map<String, Alert> loaded = this.repository.loadAll();
        assertTrue(loaded.isEmpty());
    }

    @Test
    void saveAllPreservesAllFields() throws Exception {
        AlertMetadataSource source = new AlertMetadataSource("Rapex", 123L, "https://example.com", OffsetDateTime.parse("2026-01-15T10:00:00Z"), 2);
        AlertMetadata metadata = new AlertMetadata(List.of(source), "guid-abc");
        AlertProduct product = new AlertProduct("Nom", "Type", "Description", "Marque", "Famille", "Catégorie", true,
                List.of("3271620030518"), List.of("LOT-A"), List.of("REF-1"), "Emballage", "2025-01-01");
        Alert alert = new Alert(metadata, "SR/00099/26", OffsetDateTime.parse("2026-03-01T12:00:00Z"),
                List.of("riskType.choking"), "Risk desc", "Supplementary", product, null, null, null, "Additional info");

        this.repository.saveAll(List.of(alert));
        Map<String, Alert> loaded = this.repository.loadAll();

        Alert result = loaded.get("SR/00099/26");
        assertNotNull(result);
        assertEquals("guid-abc", result.metadata().rappelconsoGuid());
        assertEquals(123L, result.metadata().sources().getFirst().sourceId());
        assertEquals(2, result.metadata().sources().getFirst().versionNumber());
        assertEquals("Nom", result.product().specificName());
        assertEquals(List.of("3271620030518"), result.product().barcodes());
        assertEquals(List.of("riskType.choking"), result.risks());
        assertEquals("Additional info", result.additionalInformation());
    }

    @Test
    void lockFileIsCreated() throws Exception {
        this.repository.saveAll(List.of(alert("SR/00001/26", "Test", "BRAND")));

        Path lockFile = this.tempDir.resolve("data.json.lock");
        assertTrue(Files.exists(lockFile));
    }

    private static Alert alert(String alertNumber, String productName, String brand) {
        AlertMetadataSource source = new AlertMetadataSource("Rapex", 1L, null, OffsetDateTime.now(), 1);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        AlertProduct product = new AlertProduct(productName, null, null, brand, null, null, null, null, null, null, null, null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null, product, null, null, null, null);
    }
}
