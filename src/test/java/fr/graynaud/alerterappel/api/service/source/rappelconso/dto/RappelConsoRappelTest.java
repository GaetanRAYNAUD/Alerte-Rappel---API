package fr.graynaud.alerterappel.api.service.source.rappelconso.dto;

import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RappelConsoRappelTest {

    @Test
    void toAlertBasicFields() {
        RappelConsoRappel rappel = rappel(
                42L, "guid-abc", "SR/00001/26", 2,
                "Poupée articulée", OffsetDateTime.parse("2026-01-15T10:00:00Z"),
                "rappel consommateur", "jouets", "poupées",
                "JOURDAIN", "REF-123", null,
                "Emballage carton", null, null, null, null,
                "Ne pas utiliser", null, null,
                "risque d'étouffement", "petites pièces",
                null, "contacter le fabricant",
                "Ne plus utiliser le produit|Rapporter le produit",
                "01 23 45 67 89", "Remboursement",
                null, null,
                "https://img1.jpg|https://img2.jpg", null, null,
                "https://rappelconso.fr/fiche/SR-00001-26"
        );

        Alert alert = rappel.toAlert();

        assertEquals("SR/00001/26", alert.alertNumber());
        assertEquals(OffsetDateTime.parse("2026-01-15T10:00:00Z"), alert.publicationDate());
        assertEquals("Poupée articulée", alert.product().specificName());
        assertEquals("JOURDAIN", alert.product().brand());
        assertEquals("REF-123", alert.product().description());
        assertEquals("Jouets", alert.product().family());
        assertEquals("Poupées", alert.product().category());
        assertEquals("Emballage carton", alert.product().packagingDescription());

        assertEquals(List.of("Petites pièces"), alert.risks());
        assertEquals("Risque d'étouffement", alert.riskDescription());

        assertEquals("France", alert.commercialization().alertCountryName());

        assertEquals("Remboursement", alert.measures().compensationTerms());
        assertEquals(List.of("Ne plus utiliser le produit", "Rapporter le produit"), alert.measures().consumerActions());

        assertEquals(List.of("https://img1.jpg", "https://img2.jpg"), alert.media().photos());
        assertEquals("https://rappelconso.fr/fiche/test", alert.media().recallSheetUrl());

        assertEquals("RappelConso", alert.metadata().sources().getFirst().origin());
        assertEquals(42L, alert.metadata().sources().getFirst().sourceId());
        assertEquals(2, alert.metadata().sources().getFirst().versionNumber());
        assertEquals("guid-abc", alert.metadata().rappelconsoGuid());
    }

    @Test
    void toAlertUppercasesNumeroFiche() {
        RappelConsoRappel rappel = rappel(
                1L, null, "sr/00001/26", 1,
                "test", OffsetDateTime.now(),
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null
        );

        Alert alert = rappel.toAlert();
        assertEquals("SR/00001/26", alert.alertNumber());
    }

    @Test
    void toAlertNullNumeroFiche() {
        RappelConsoRappel rappel = rappel(
                1L, null, null, 1,
                "test", OffsetDateTime.now(),
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null
        );

        Alert alert = rappel.toAlert();
        assertNull(alert.alertNumber());
    }

    @Test
    void toAlertWithBarcodesAndLots() {
        RappelConsoRappel rappel = rappel(
                1L, null, "SR/001/26", 1,
                "Fromage", OffsetDateTime.now(),
                null, "alimentation", null, null, null,
                List.of("3271620030518", "lot A", "date limite de consommation", "2026-07-12"),
                null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null
        );

        Alert alert = rappel.toAlert();
        assertEquals(List.of("3271620030518"), alert.product().barcodes());
        assertEquals(List.of("Lot A"), alert.product().batchNumbers());
        assertNotNull(alert.product().productionDates());
    }

    @Test
    void toAlertNullRisquesEncourus() {
        RappelConsoRappel rappel = rappel(
                1L, null, "SR/001/26", 1,
                "test", OffsetDateTime.now(),
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null
        );

        Alert alert = rappel.toAlert();
        assertNull(alert.risks());
    }

    @Test
    void toAlertNullFieldsProduceNullSubFields() {
        RappelConsoRappel rappel = rappel(
                1L, null, "SR/001/26", 1,
                null, OffsetDateTime.now(),
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null
        );

        Alert alert = rappel.toAlert();
        assertNull(alert.product().specificName());
        assertNull(alert.product().barcodes());
        assertNull(alert.product().batchNumbers());
        assertNull(alert.product().productionDates());
        assertNull(alert.commercialization().reactingCountries());
        assertNull(alert.measures().measuresList());
        assertNull(alert.measures().consumerActions());
        assertNull(alert.media().photos());
    }

    @Test
    void toAlertCommercialization() {
        RappelConsoRappel rappel = rappel(
                1L, null, "SR/001/26", 1,
                "test", OffsetDateTime.now(),
                null, null, null, null, null, null, null,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 1), null, null,
                null, "France entière", "Carrefour", null, null, null, null, null, null, null,
                null, null, null, null, null, null
        );

        Alert alert = rappel.toAlert();
        assertEquals(List.of("France entière"), alert.commercialization().reactingCountries());
        assertEquals(LocalDate.of(2025, 1, 1), alert.commercialization().marketingStartDate());
        assertEquals(LocalDate.of(2025, 6, 1), alert.commercialization().marketingEndDate());
        assertEquals("Carrefour", alert.commercialization().distributors());
    }

    @Test
    void toAlertMeasuresWithNatureJuridique() {
        RappelConsoRappel rappel = rappel(
                1L, null, "SR/001/26", 1,
                "test", OffsetDateTime.now(),
                "rappel volontaire", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                LocalDate.of(2026, 12, 31), null, null, null, null, null
        );

        Alert alert = rappel.toAlert();
        assertNotNull(alert.measures().measuresList());
        assertEquals(1, alert.measures().measuresList().size());
        assertEquals("Rappel volontaire", alert.measures().measuresList().getFirst().type());
        assertEquals(LocalDate.of(2026, 12, 31), alert.measures().procedureEndDate());
    }

    // Helper to create RappelConsoRappel with all fields
    private static RappelConsoRappel rappel(
            Long id, String rappelGuid, String numeroFiche, Integer numeroVersion,
            String libelle, OffsetDateTime datePublication,
            String natureJuridiqueRappel, String categorieProduit, String sousCategoreProduit,
            String marqueProduit, String modelesOuReferences, List<String> identificationProduits,
            String conditionnements, LocalDate dateDebutCommercialisation, LocalDate dateDateFinCommercialisation,
            String temperatureConservation, String marqueSalubrite,
            String informationsComplementaires, String zoneGeographiqueDeVente, String distributeurs,
            String motifRappel, String risquesEncourus, String preconisationsSanitaires,
            String descriptionComplementaireRisque, String conduitesATenirParLeConsommateur,
            String numeroContact, String modalitesDeCompensation,
            LocalDate dateDeLaProcedureDeRappel, String informationsComplementairesPubliques,
            String liensVersLesImages, String lienVersLaListeDesProduits,
            String lienVersLaListeDesDistributeurs, String lienVersAffichettePdf
    ) {
        return new RappelConsoRappel(
                id, rappelGuid, numeroFiche, numeroVersion,
                libelle, datePublication,
                natureJuridiqueRappel, categorieProduit, sousCategoreProduit,
                marqueProduit, modelesOuReferences, identificationProduits,
                conditionnements, dateDebutCommercialisation, dateDateFinCommercialisation,
                temperatureConservation, marqueSalubrite,
                informationsComplementaires, zoneGeographiqueDeVente, distributeurs,
                motifRappel, risquesEncourus, preconisationsSanitaires,
                descriptionComplementaireRisque, conduitesATenirParLeConsommateur,
                numeroContact, modalitesDeCompensation,
                dateDeLaProcedureDeRappel, informationsComplementairesPubliques,
                liensVersLesImages, lienVersLaListeDesProduits,
                lienVersLaListeDesDistributeurs, lienVersAffichettePdf,
                "https://rappelconso.fr/fiche/test"
        );
    }
}
