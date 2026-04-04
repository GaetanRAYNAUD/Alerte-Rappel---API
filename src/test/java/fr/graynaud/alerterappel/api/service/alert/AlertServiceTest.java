package fr.graynaud.alerterappel.api.service.alert;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.controller.dto.PageResponse;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadata;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadataSource;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AlertServiceTest {

    @TempDir
    Path tempDir;

    private AlertService service;

    @BeforeEach
    void setUp() throws Exception {
        DataProperties dataProperties = new DataProperties();
        dataProperties.setPath(this.tempDir.toString());
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
        AlertRepository repository = new AlertRepository(dataProperties, jsonMapper);
        AlertMerger merger = new AlertMerger();
        AlertSearchIndex searchIndex = new AlertSearchIndex();
        this.service = new AlertService(repository, merger, searchIndex);
    }

    // --- addAlerts ---

    @Test
    void addAlertsAddsNewAlerts() {
        this.service.addAlerts(List.of(
                alert("SR/001/26", "Poupée", "JOURDAIN", "RappelConso", 1),
                alert("SR/002/26", "Voiture", "BMW", "RappelConso", 1)
        ));

        assertTrue(this.service.findByAlertNumber("SR/001/26").isPresent());
        assertTrue(this.service.findByAlertNumber("SR/002/26").isPresent());
    }

    @Test
    void addAlertsEmptyListDoesNothing() {
        this.service.addAlerts(List.of(alert("SR/001/26", "Poupée", "JOURDAIN", "RappelConso", 1)));

        this.service.addAlerts(List.of());
        this.service.addAlerts(null);

        assertTrue(this.service.findByAlertNumber("SR/001/26").isPresent());
    }

    @Test
    void addAlertsMergesExistingAlert() {
        this.service.addAlerts(List.of(alert("SR/001/26", "Poupée", "JOURDAIN", "RappelConso", 1)));
        this.service.addAlerts(List.of(alert("SR/001/26", "Poupée v2", "JOURDAIN", "RappelConso", 2)));

        Alert result = this.service.findByAlertNumber("SR/001/26").orElseThrow();
        assertEquals("Poupée v2", result.product().specificName());
        assertEquals(2, result.metadata().sources().getFirst().versionNumber());
    }

    @Test
    void addAlertsCrossSourceMerge() {
        this.service.addAlerts(List.of(alert("SR/001/26", "Toy", null, "Rapex", 1)));
        this.service.addAlerts(List.of(alert("SR/001/26", "Jouet", "JOURDAIN", "RappelConso", 1)));

        Alert result = this.service.findByAlertNumber("SR/001/26").orElseThrow();
        assertEquals(2, result.metadata().sources().size());
        assertEquals("Toy", result.product().specificName());
        assertEquals("JOURDAIN", result.product().brand());
    }

    @Test
    void addAlertsPersistsToFile() throws Exception {
        this.service.addAlerts(List.of(alert("SR/001/26", "Poupée", "JOURDAIN", "RappelConso", 1)));

        // Create a new service from the same directory to verify persistence
        DataProperties dataProperties = new DataProperties();
        dataProperties.setPath(this.tempDir.toString());
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
        AlertRepository newRepository = new AlertRepository(dataProperties, jsonMapper);

        assertTrue(newRepository.loadAll().containsKey("SR/001/26"));
    }

    @Test
    void addAlertsBuildsBarcodeIndex() {
        AlertProduct product = new AlertProduct("Fromage", null, null, "MARQUE", null, null, null,
                List.of("3271620030518"), null, null, null, null);
        Alert alert = alertWithProduct("SR/001/26", "RappelConso", product);
        this.service.addAlerts(List.of(alert));

        Optional<Alert> result = this.service.findByBarcode("3271620030518");
        assertTrue(result.isPresent());
        assertEquals("SR/001/26", result.get().alertNumber());
    }

    @Test
    void addAlertsBarcodeIndexKeepsMostRecent() {
        OffsetDateTime older = OffsetDateTime.parse("2025-01-01T00:00:00Z");
        OffsetDateTime newer = OffsetDateTime.parse("2026-01-01T00:00:00Z");

        AlertProduct product1 = new AlertProduct("Ancien", null, null, null, null, null, null,
                List.of("3271620030518"), null, null, null, null);
        AlertProduct product2 = new AlertProduct("Nouveau", null, null, null, null, null, null,
                List.of("3271620030518"), null, null, null, null);

        Alert alert1 = alertWithProductAndDate("SR/001/26", "RappelConso", product1, older);
        Alert alert2 = alertWithProductAndDate("SR/002/26", "RappelConso", product2, newer);

        this.service.addAlerts(List.of(alert1, alert2));

        Alert result = this.service.findByBarcode("3271620030518").orElseThrow();
        assertEquals("SR/002/26", result.alertNumber());
    }

    @Test
    void addAlertsBuildsSortedByDateIndex() {
        OffsetDateTime date1 = OffsetDateTime.parse("2025-01-01T00:00:00Z");
        OffsetDateTime date2 = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        OffsetDateTime date3 = OffsetDateTime.parse("2026-01-01T00:00:00Z");

        this.service.addAlerts(List.of(
                alertWithDate("SR/001/26", "Poupée", date1),
                alertWithDate("SR/002/26", "Voiture", date2),
                alertWithDate("SR/003/26", "Casserole", date3)
        ));

        PageResponse<Alert> latest = this.service.getLatest(0, 10);
        assertEquals(3, latest.totalElements());
        assertEquals("SR/002/26", latest.content().get(0).alertNumber());
        assertEquals("SR/003/26", latest.content().get(1).alertNumber());
        assertEquals("SR/001/26", latest.content().get(2).alertNumber());
    }

    // --- search ---

    @Test
    void searchNullQuery() {
        this.service.addAlerts(List.of(alert("SR/001/26", "Poupée", "JOURDAIN", "RappelConso", 1)));

        PageResponse<Alert> result = this.service.search(null, 0, 10);
        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    @Test
    void searchBlankQuery() {
        this.service.addAlerts(List.of(alert("SR/001/26", "Poupée", "JOURDAIN", "RappelConso", 1)));

        PageResponse<Alert> result = this.service.search("   ", 0, 10);
        assertTrue(result.content().isEmpty());
    }

    @Test
    void searchByProductName() {
        this.service.addAlerts(List.of(
                alert("SR/001/26", "Poupée artisanale", "JOURDAIN", "RappelConso", 1),
                alert("SR/002/26", "Voiture électrique", "BMW", "RappelConso", 1)
        ));

        PageResponse<Alert> result = this.service.search("poupée", 0, 10);
        assertEquals(1, result.totalElements());
        assertEquals("SR/001/26", result.content().getFirst().alertNumber());
    }

    @Test
    void searchByBrand() {
        this.service.addAlerts(List.of(
                alert("SR/001/26", "Jouet", "JOURDAIN", "RappelConso", 1),
                alert("SR/002/26", "Jouet", "HASBRO", "RappelConso", 1)
        ));

        PageResponse<Alert> result = this.service.search("JOURDAIN", 0, 10);
        assertEquals(1, result.totalElements());
        assertEquals("SR/001/26", result.content().getFirst().alertNumber());
    }

    @Test
    void searchReturnsFullAlertObjects() {
        this.service.addAlerts(List.of(alert("SR/001/26", "Poupée", "JOURDAIN", "RappelConso", 1)));

        PageResponse<Alert> result = this.service.search("poupée", 0, 10);
        Alert alert = result.content().getFirst();
        assertEquals("Poupée", alert.product().specificName());
        assertEquals("JOURDAIN", alert.product().brand());
        assertNotNull(alert.metadata());
    }

    @Test
    void searchPagination() {
        this.service.addAlerts(List.of(
                alert("SR/001/26", "Jouet plastique", "MARQUE", "RappelConso", 1),
                alert("SR/002/26", "Jouet bois", "MARQUE", "RappelConso", 1),
                alert("SR/003/26", "Jouet métal", "MARQUE", "RappelConso", 1)
        ));

        PageResponse<Alert> page0 = this.service.search("jouet", 0, 2);
        assertEquals(2, page0.content().size());
        assertEquals(3, page0.totalElements());

        PageResponse<Alert> page1 = this.service.search("jouet", 1, 2);
        assertEquals(1, page1.content().size());
    }

    // --- findByAlertNumber ---

    @Test
    void findByAlertNumberCaseInsensitive() {
        this.service.addAlerts(List.of(alert("SR/001/26", "Poupée", "JOURDAIN", "RappelConso", 1)));

        assertTrue(this.service.findByAlertNumber("sr/001/26").isPresent());
        assertTrue(this.service.findByAlertNumber("SR/001/26").isPresent());
    }

    @Test
    void findByAlertNumberNotFound() {
        assertTrue(this.service.findByAlertNumber("SR/999/26").isEmpty());
    }

    // --- helpers ---

    private static Alert alert(String alertNumber, String productName, String brand, String origin, Integer version) {
        AlertMetadataSource source = new AlertMetadataSource(origin, 1L, null, OffsetDateTime.now(), version);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        AlertProduct product = new AlertProduct(productName, null, null, brand, null, null, null, null, null, null, null, null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null, product, null, null, null, null);
    }

    private static Alert alertWithProduct(String alertNumber, String origin, AlertProduct product) {
        AlertMetadataSource source = new AlertMetadataSource(origin, 1L, null, OffsetDateTime.now(), 1);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null, product, null, null, null, null);
    }

    private static Alert alertWithProductAndDate(String alertNumber, String origin, AlertProduct product, OffsetDateTime date) {
        AlertMetadataSource source = new AlertMetadataSource(origin, 1L, null, OffsetDateTime.now(), 1);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        return new Alert(metadata, alertNumber, date, null, null, null, product, null, null, null, null);
    }

    private static Alert alertWithDate(String alertNumber, String productName, OffsetDateTime date) {
        AlertMetadataSource source = new AlertMetadataSource("RappelConso", 1L, null, OffsetDateTime.now(), 1);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        AlertProduct product = new AlertProduct(productName, null, null, null, null, null, null, null, null, null, null, null);
        return new Alert(metadata, alertNumber, date, null, null, null, product, null, null, null, null);
    }
}
