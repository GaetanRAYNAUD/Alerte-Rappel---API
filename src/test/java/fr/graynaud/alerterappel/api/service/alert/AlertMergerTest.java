package fr.graynaud.alerterappel.api.service.alert;

import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertCommercialization;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMeasureItem;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMeasures;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMedia;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadata;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadataSource;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertProduct;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlertMergerTest {

    private final AlertMerger merger = new AlertMerger();

    // --- same origin, version-based ---

    @Test
    void sameOriginKeepsHigherVersion() {
        Alert v1 = rapex("SR/001/26", "Poupée v1", 1);
        Alert v2 = rapex("SR/001/26", "Poupée v2", 2);

        Alert result = this.merger.mergeAlerts(v1, v2);
        assertEquals("Poupée v2", result.product().specificName());
    }

    @Test
    void sameOriginKeepsExistingIfHigherVersion() {
        Alert v3 = rapex("SR/001/26", "Poupée v3", 3);
        Alert v2 = rapex("SR/001/26", "Poupée v2", 2);

        Alert result = this.merger.mergeAlerts(v3, v2);
        assertEquals("Poupée v3", result.product().specificName());
    }

    @Test
    void sameOriginNullVersionsKeepsIncoming() {
        Alert existing = rapex("SR/001/26", "Old", null);
        Alert incoming = rapex("SR/001/26", "New", null);

        Alert result = this.merger.mergeAlerts(existing, incoming);
        assertEquals("New", result.product().specificName());
    }

    // --- cross-source merge ---

    @Test
    void crossSourceMergeUsesRapexAlertNumber() {
        Alert rapex = rapex("SR/001/26", "Toy", 1);
        Alert rc = rappelConso("SR/001/26", "Jouet", 1);

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertEquals("SR/001/26", result.alertNumber());
    }

    @Test
    void crossSourceRapexFieldsTakePriority() {
        Alert rapex = rapexFull("SR/001/26", "Rapex Name", "RapexBrand", "Rapex risk");
        Alert rc = rappelConsoFull("SR/001/26", "RC Name", "RC Brand", "RC risk");

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertEquals("Rapex Name", result.product().specificName());
        assertEquals("RapexBrand", result.product().brand());
        assertEquals("Rapex risk", result.riskDescription());
    }

    @Test
    void crossSourceFillsNullFieldsFromOther() {
        Alert rapex = rapexFull("SR/001/26", "Rapex Name", null, null);
        Alert rc = rappelConsoFull("SR/001/26", null, "RC Brand", "RC risk");

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertEquals("Rapex Name", result.product().specificName());
        assertEquals("RC Brand", result.product().brand());
        assertEquals("RC risk", result.riskDescription());
    }

    @Test
    void crossSourceMergesMetadataSources() {
        Alert rapex = rapex("SR/001/26", "Toy", 1);
        Alert rc = rappelConso("SR/001/26", "Jouet", 1);

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertEquals(2, result.metadata().sources().size());
        assertTrue(result.metadata().sources().stream().anyMatch(s -> "Rapex".equals(s.origin())));
        assertTrue(result.metadata().sources().stream().anyMatch(s -> "RappelConso".equals(s.origin())));
    }

    @Test
    void crossSourceDeduplicatesBarcodesNormalized() {
        AlertProduct rapexProduct = product("Toy", "BRAND", List.of("3271620030518", "1234567890123"), null);
        AlertProduct rcProduct = product("Jouet", "BRAND", List.of("3271620030518"), null);

        Alert rapex = alertWith("SR/001/26", "Rapex", rapexProduct, null);
        Alert rc = alertWith("SR/001/26", "RappelConso", rcProduct, null);

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertEquals(2, result.product().barcodes().size());
        assertTrue(result.product().barcodes().contains("3271620030518"));
        assertTrue(result.product().barcodes().contains("1234567890123"));
    }

    @Test
    void crossSourceDeduplicatesRisks() {
        Alert rapex = alertWithRisks("SR/001/26", "Rapex", List.of("riskType.choking", "riskType.injuries"));
        Alert rc = alertWithRisks("SR/001/26", "RappelConso", List.of("riskType.choking", "riskType.chemical"));

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertEquals(3, result.risks().size());
    }

    @Test
    void crossSourceMergesMedia() {
        AlertMedia rapexMedia = new AlertMedia(List.of("rapex-photo.jpg"), null);
        AlertMedia rcMedia = new AlertMedia(List.of("rc-photo.jpg"), "https://rappelconso.fr/fiche/123");

        Alert rapex = alertWithMedia("SR/001/26", "Rapex", rapexMedia);
        Alert rc = alertWithMedia("SR/001/26", "RappelConso", rcMedia);

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertEquals(2, result.media().photos().size());
        // recallSheetUrl: other (RappelConso) takes precedence in mergeMedia
        assertEquals("https://rappelconso.fr/fiche/123", result.media().recallSheetUrl());
    }

    @Test
    void crossSourceMergesMeasureItems() {
        AlertMeasureItem rapexMeasure = new AlertMeasureItem("measure.category.recall.of.product.from.consumers", null, "measure.type.voluntary", null);
        AlertMeasureItem rcMeasure = new AlertMeasureItem("measure.category.withdrawal.of.product.from.market", null, "volontaire", null);

        AlertMeasures rapexMeasures = new AlertMeasures(true, List.of(rapexMeasure), null, null, null, null);
        AlertMeasures rcMeasures = new AlertMeasures(null, List.of(rcMeasure), null, List.of("Ramener le produit"), "Remboursement", null);

        Alert rapex = alertWithMeasures("SR/001/26", "Rapex", rapexMeasures);
        Alert rc = alertWithMeasures("SR/001/26", "RappelConso", rcMeasures);

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertEquals(2, result.measures().measuresList().size());
        assertTrue(result.measures().recallPublishedOnline());
        assertEquals(List.of("Ramener le produit"), result.measures().consumerActions());
        assertEquals("Remboursement", result.measures().compensationTerms());
    }

    @Test
    void crossSourceDeduplicatesMeasuresByNormalizedCategory() {
        AlertMeasureItem m1 = new AlertMeasureItem("measure.category.recall", null, null, null);
        AlertMeasureItem m2 = new AlertMeasureItem("Measure Category Recall", null, null, null);

        AlertMeasures measures1 = new AlertMeasures(null, List.of(m1), null, null, null, null);
        AlertMeasures measures2 = new AlertMeasures(null, List.of(m2), null, null, null, null);

        Alert rapex = alertWithMeasures("SR/001/26", "Rapex", measures1);
        Alert rc = alertWithMeasures("SR/001/26", "RappelConso", measures2);

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertEquals(1, result.measures().measuresList().size());
    }

    @Test
    void crossSourceMergesCommercialization() {
        AlertCommercialization rapexComm = new AlertCommercialization("China", "France", List.of("FR", "DE"), true, null, null, null);
        AlertCommercialization rcComm = new AlertCommercialization(null, "France", List.of("FR"), null,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 1), "Carrefour");

        Alert rapex = alertWithCommercialization("SR/001/26", "Rapex", rapexComm);
        Alert rc = alertWithCommercialization("SR/001/26", "RappelConso", rcComm);

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertEquals("China", result.commercialization().originCountryName());
        assertEquals(2, result.commercialization().reactingCountries().size());
        assertTrue(result.commercialization().soldOnline());
        assertEquals(LocalDate.of(2025, 1, 1), result.commercialization().marketingStartDate());
        assertEquals("Carrefour", result.commercialization().distributors());
    }

    @Test
    void crossSourcePreservesRappelconsoGuid() {
        Alert rapex = rapex("SR/001/26", "Toy", 1);
        AlertMetadata rcMeta = new AlertMetadata(List.of(new AlertMetadataSource("RappelConso", 49410L, null, OffsetDateTime.now(), 1)), "guid-123");
        Alert rc = new Alert(rcMeta, "SR/001/26", OffsetDateTime.now(), null, null, null, null, null, null, null, null);

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertEquals("guid-123", result.metadata().rappelconsoGuid());
    }

    @Test
    void metadataSourceVersionIsUpdated() {
        Alert rc1 = rappelConso("SR/001/26", "Jouet v1", 1);
        Alert rc2 = rappelConso("SR/001/26", "Jouet v2", 3);

        // First merge same-origin keeps v3
        Alert result = this.merger.mergeAlerts(rc1, rc2);
        assertEquals(3, result.metadata().sources().getFirst().versionNumber());
    }

    // --- edge cases ---

    @Test
    void bothNonRapexKeepsIncoming() {
        Alert rc1 = rappelConso("SR/001/26", "Old", 1);
        AlertMetadata otherMeta = new AlertMetadata(List.of(new AlertMetadataSource("Other", 1L, null, OffsetDateTime.now(), 1)), null);
        Alert other = new Alert(otherMeta, "SR/001/26", OffsetDateTime.now(), null, null, null,
                new AlertProduct("New", null, null, null, null, null, null, null, null, null, null, null),
                null, null, null, null);

        Alert result = this.merger.mergeAlerts(rc1, other);
        assertEquals("New", result.product().specificName());
    }

    @Test
    void nullProductOnOneSide() {
        Alert rapex = alertWith("SR/001/26", "Rapex", null, null);
        Alert rc = alertWith("SR/001/26", "RappelConso",
                new AlertProduct("Jouet", null, null, "RC Brand", null, null, null, null, null, null, null, null), null);

        Alert result = this.merger.mergeAlerts(rapex, rc);
        assertNotNull(result.product());
        assertEquals("Jouet", result.product().specificName());
    }

    // --- helpers ---

    private static Alert rapex(String alertNumber, String productName, Integer version) {
        AlertMetadataSource source = new AlertMetadataSource("Rapex", 1L, null, OffsetDateTime.now(), version);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        AlertProduct product = new AlertProduct(productName, null, null, null, null, null, null, null, null, null, null, null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null, product, null, null, null, null);
    }

    private static Alert rappelConso(String alertNumber, String productName, Integer version) {
        AlertMetadataSource source = new AlertMetadataSource("RappelConso", 1L, null, OffsetDateTime.now(), version);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        AlertProduct product = new AlertProduct(productName, null, null, null, null, null, null, null, null, null, null, null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null, product, null, null, null, null);
    }

    private static Alert rapexFull(String alertNumber, String productName, String brand, String riskDescription) {
        AlertMetadataSource source = new AlertMetadataSource("Rapex", 1L, null, OffsetDateTime.now(), 1);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        AlertProduct product = new AlertProduct(productName, null, null, brand, null, null, null, null, null, null, null, null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, riskDescription, null, product, null, null, null, null);
    }

    private static Alert rappelConsoFull(String alertNumber, String productName, String brand, String riskDescription) {
        AlertMetadataSource source = new AlertMetadataSource("RappelConso", 1L, null, OffsetDateTime.now(), 1);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        AlertProduct product = new AlertProduct(productName, null, null, brand, null, null, null, null, null, null, null, null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, riskDescription, null, product, null, null, null, null);
    }

    private static AlertProduct product(String name, String brand, List<String> barcodes, List<String> batchNumbers) {
        return new AlertProduct(name, null, null, brand, null, null, null, barcodes, batchNumbers, null, null, null);
    }

    private static Alert alertWith(String alertNumber, String origin, AlertProduct product, AlertCommercialization commercialization) {
        AlertMetadataSource source = new AlertMetadataSource(origin, 1L, null, OffsetDateTime.now(), 1);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null, product, commercialization, null, null, null);
    }

    private static Alert alertWithRisks(String alertNumber, String origin, List<String> risks) {
        AlertMetadataSource source = new AlertMetadataSource(origin, 1L, null, OffsetDateTime.now(), 1);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), risks, null, null, null, null, null, null, null);
    }

    private static Alert alertWithMedia(String alertNumber, String origin, AlertMedia media) {
        AlertMetadataSource source = new AlertMetadataSource(origin, 1L, null, OffsetDateTime.now(), 1);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null, null, null, null, media, null);
    }

    private static Alert alertWithMeasures(String alertNumber, String origin, AlertMeasures measures) {
        AlertMetadataSource source = new AlertMetadataSource(origin, 1L, null, OffsetDateTime.now(), 1);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null, null, null, measures, null, null);
    }

    private static Alert alertWithCommercialization(String alertNumber, String origin, AlertCommercialization commercialization) {
        AlertMetadataSource source = new AlertMetadataSource(origin, 1L, null, OffsetDateTime.now(), 1);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);
        return new Alert(metadata, alertNumber, OffsetDateTime.now(), null, null, null, null, commercialization, null, null, null);
    }
}
