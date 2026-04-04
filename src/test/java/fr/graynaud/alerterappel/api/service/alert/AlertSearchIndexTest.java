package fr.graynaud.alerterappel.api.service.alert;

import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertCommercialization;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadata;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertProduct;
import fr.graynaud.alerterappel.api.service.alert.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class AlertSearchIndexTest {

    private AlertSearchIndex searchIndex;

    @BeforeEach
    void setUp() {
        this.searchIndex = new AlertSearchIndex();
    }

    @Test
    void searchOnEmptyIndex() {
        SearchResult results = this.searchIndex.search("test", 0, 10);
        assertTrue(results.alertNumbers().isEmpty());
        assertEquals(0, results.totalHits());
    }

    @Test
    void searchByAlertNumber() {
        Map<String, Alert> alerts = Map.of("SR/00842/26", alert("SR/00842/26", "Poupée", "JOURDAIN", null, null, null));
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("SR/00842/26", 0, 10);
        assertEquals(1, results.alertNumbers().size());
        assertEquals(1, results.totalHits());
        assertEquals("SR/00842/26", results.alertNumbers().getFirst());
    }

    @Test
    void searchByProductName() {
        Map<String, Alert> alerts = Map.of(
                "A1", alert("A1", "Poupée en plastique", "JOURDAIN", null, null, null),
                "A2", alert("A2", "Voiture électrique", "BMW", null, null, null)
        );
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("poupée", 0, 10);
        assertEquals(1, results.alertNumbers().size());
        assertEquals(1, results.totalHits());
        assertEquals("A1", results.alertNumbers().getFirst());
    }

    @Test
    void searchByBrand() {
        Map<String, Alert> alerts = Map.of(
                "A1", alert("A1", "Jouet", "JOURDAIN", null, null, null),
                "A2", alert("A2", "Jouet", "HASBRO", null, null, null)
        );
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("JOURDAIN", 0, 10);
        assertEquals(1, results.alertNumbers().size());
        assertEquals(1, results.totalHits());
        assertEquals("A1", results.alertNumbers().getFirst());
    }

    @Test
    void searchByBarcode() {
        Map<String, Alert> alerts = Map.of(
                "A1", alert("A1", "Jouet", "MARQUE", List.of("3271620030518"), null, null),
                "A2", alert("A2", "Autre", "MARQUE", List.of("9876543210123"), null, null)
        );
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("3271620030518", 0, 10);
        assertEquals(1, results.alertNumbers().size());
        assertEquals(1, results.totalHits());
        assertEquals("A1", results.alertNumbers().getFirst());
    }

    @Test
    void searchByBatchNumber() {
        Map<String, Alert> alerts = Map.of(
                "A1", alert("A1", "Jouet", "MARQUE", null, List.of("LOT-2025-A"), null)
        );
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("LOT-2025-A", 0, 10);
        assertFalse(results.alertNumbers().isEmpty());
        assertEquals(1, results.totalHits());
        assertEquals("A1", results.alertNumbers().getFirst());
    }

    @Test
    void searchByDistributor() {
        Map<String, Alert> alerts = Map.of(
                "A1", alertWithDistributor("A1", "Jouet", "Carrefour, Leclerc"),
                "A2", alertWithDistributor("A2", "Autre", "Auchan")
        );
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("Carrefour", 0, 10);
        assertEquals(1, results.alertNumbers().size());
        assertEquals(1, results.totalHits());
        assertEquals("A1", results.alertNumbers().getFirst());
    }

    @Test
    void searchByProductDescription() {
        Map<String, Alert> alerts = new ConcurrentHashMap<>();
        alerts.put("A1", alertWithDescription("A1", "Risque étouffement par les petites pièces"));
        alerts.put("A2", alertWithDescription("A2", "Risque de coupure"));
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("étouffement", 0, 10);
        assertEquals(1, results.alertNumbers().size());
        assertEquals(1, results.totalHits());
        assertEquals("A1", results.alertNumbers().getFirst());
    }

    @Test
    void searchIsCaseInsensitive() {
        Map<String, Alert> alerts = Map.of("A1", alert("A1", "Poupée ROSE", "JOURDAIN", null, null, null));
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("poupée rose", 0, 10);
        assertFalse(results.alertNumbers().isEmpty());
        assertEquals(1, results.totalHits());
        assertEquals("A1", results.alertNumbers().getFirst());
    }

    @Test
    void searchOrLogic() {
        Map<String, Alert> alerts = Map.of(
                "A1", alert("A1", "Poupée", "JOURDAIN", null, null, null),
                "A2", alert("A2", "Voiture", "BMW", null, null, null),
                "A3", alert("A3", "Casserole", "TEFAL", null, null, null)
        );
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("poupée voiture", 0, 10);
        assertEquals(2, results.alertNumbers().size());
        assertEquals(2, results.totalHits());
        assertTrue(results.alertNumbers().contains("A1"));
        assertTrue(results.alertNumbers().contains("A2"));
    }

    @Test
    void fuzzyMatchOnLongToken() {
        Map<String, Alert> alerts = Map.of("A1", alert("A1", "Voiture électrique", "BMW", null, null, null));
        this.searchIndex.rebuild(alerts);

        // "voitur" instead of "voiture" — 1 edit distance
        SearchResult results = this.searchIndex.search("voitur", 0, 10);
        assertFalse(results.alertNumbers().isEmpty(), "Fuzzy search should match 'voiture' with 'voitur'");
        assertEquals(1, results.totalHits());
        assertEquals("A1", results.alertNumbers().getFirst());
    }

    @Test
    void noFuzzyOnShortToken() {
        Map<String, Alert> alerts = Map.of(
                "A1", alert("A1", "AB produit", "MARQUE", null, null, null),
                "A2", alert("A2", "AC produit", "MARQUE", null, null, null)
        );
        this.searchIndex.rebuild(alerts);

        // "AB" is 2 chars, no fuzzy: should not match "AC"
        SearchResult results = this.searchIndex.search("AB", 0, 10);
        assertTrue(results.alertNumbers().size() <= 1);
        if (!results.alertNumbers().isEmpty()) {
            assertEquals("A1", results.alertNumbers().getFirst());
        }
    }

    @Test
    void productNameBoostRanksHigher() {
        Map<String, Alert> alerts = new ConcurrentHashMap<>();
        // A1: "poupée" is in the product name (boosted x2)
        alerts.put("A1", alert("A1", "Poupée artisanale", "ARTISAN", null, null, null));
        // A2: "poupée" is only in the product description (boost x0.1)
        alerts.put("A2", alertWithDescription("A2", "Risque lié à une poupée contrefaite"));
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("poupée", 0, 10);
        assertEquals(2, results.alertNumbers().size());
        assertEquals(2, results.totalHits());
        assertEquals("A1", results.alertNumbers().getFirst(), "Product name match should rank higher due to boost");
    }

    @Test
    void searchEmptyQuery() {
        Map<String, Alert> alerts = Map.of("A1", alert("A1", "Jouet", "MARQUE", null, null, null));
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("", 0, 10);
        assertTrue(results.alertNumbers().isEmpty());
        assertEquals(0, results.totalHits());
    }

    @Test
    void searchBlankQuery() {
        Map<String, Alert> alerts = Map.of("A1", alert("A1", "Jouet", "MARQUE", null, null, null));
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("   ", 0, 10);
        assertTrue(results.alertNumbers().isEmpty());
        assertEquals(0, results.totalHits());
    }

    @Test
    void rebuildReplacesIndex() {
        Map<String, Alert> alerts1 = Map.of("A1", alert("A1", "Poupée", "JOURDAIN", null, null, null));
        this.searchIndex.rebuild(alerts1);
        assertEquals(1, this.searchIndex.search("poupée", 0, 10).alertNumbers().size());

        Map<String, Alert> alerts2 = Map.of("A2", alert("A2", "Voiture", "BMW", null, null, null));
        this.searchIndex.rebuild(alerts2);

        assertTrue(this.searchIndex.search("poupée", 0, 10).alertNumbers().isEmpty(), "Old data should no longer be searchable");
        assertEquals(1, this.searchIndex.search("voiture", 0, 10).alertNumbers().size());
    }

    @Test
    void maxResultsIsRespected() {
        Map<String, Alert> alerts = new ConcurrentHashMap<>();
        for (int i = 0; i < 20; i++) {
            String num = "A" + i;
            alerts.put(num, alert(num, "Jouet plastique", "MARQUE", null, null, null));
        }
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("jouet", 0, 5);
        assertEquals(5, results.alertNumbers().size());
        assertEquals(20, results.totalHits());
    }

    @Test
    void searchByModelReference() {
        Map<String, Alert> alerts = Map.of(
                "A1", alert("A1", "Voiture", "BMW", null, null, List.of("e1*2007/46*0362*14"))
        );
        this.searchIndex.rebuild(alerts);

        SearchResult results = this.searchIndex.search("e1", 0, 10);
        assertFalse(results.alertNumbers().isEmpty());
        assertEquals(1, results.totalHits());
        assertEquals("A1", results.alertNumbers().getFirst());
    }

    // --- helpers ---

    private static Alert alert(String alertNumber, String productName, String brand,
                               List<String> barcodes, List<String> batchNumbers, List<String> modelReferences) {
        AlertProduct product = new AlertProduct(productName, null, null, brand, null, null, null,
                barcodes, batchNumbers, modelReferences, null, null);
        AlertMetadata metadata = new AlertMetadata(List.of(), null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null,
                product, null, null, null, null);
    }

    private static Alert alertWithDistributor(String alertNumber, String productName, String distributors) {
        AlertProduct product = new AlertProduct(productName, null, null, null, null, null, null,
                null, null, null, null, null);
        AlertCommercialization commercialization = new AlertCommercialization(null, null, null, null, null, null, distributors);
        AlertMetadata metadata = new AlertMetadata(List.of(), null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null,
                product, commercialization, null, null, null);
    }

    private static Alert alertWithDescription(String alertNumber, String description) {
        AlertProduct product = new AlertProduct(null, null, description, null, null, null, null,
                null, null, null, null, null);
        AlertMetadata metadata = new AlertMetadata(List.of(), null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null,
                product, null, null, null, null);
    }
}
