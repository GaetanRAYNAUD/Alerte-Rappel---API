package fr.graynaud.alerterappel.api.service.source.rappelconso.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdentificationProduitsParserTest {

    @Test
    void vide() {
        IdentificationProduits result = IdentificationProduitsParser.parse(List.of(), "alimentation");
        assertEquals("vide", result.pattern());
        assertTrue(result.blocs().isEmpty());
    }

    @Test
    void videNull() {
        IdentificationProduits result = IdentificationProduitsParser.parse(null, "alimentation");
        assertEquals("vide", result.pattern());
        assertTrue(result.blocs().isEmpty());
    }

    @Test
    void texteLibreAuto() {
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("14.08.2024 - 14.04.2025"), "automobiles et moyens de déplacement");
        assertEquals("texte_libre", result.pattern());
        assertEquals(1, result.blocs().size());
        ProduitIdentifie bloc = result.blocs().getFirst();
        assertNull(bloc.gtin());
        assertEquals("14.08.2024 - 14.04.2025", bloc.lot());
        assertNull(bloc.typeDate());
        assertNull(bloc.dateDebut());
        assertNull(bloc.dateFin());
    }

    @Test
    void singleBlocComplet() {
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("3271620030518", "07326", "date limite de consommation", "2026-07-12"), "alimentation");
        assertEquals("single_bloc", result.pattern());
        assertEquals(1, result.blocs().size());
        ProduitIdentifie bloc = result.blocs().getFirst();
        assertEquals("3271620030518", bloc.gtin());
        assertEquals("07326", bloc.lot());
        assertEquals("date limite de consommation", bloc.typeDate());
        assertEquals("2026-07-12", bloc.dateDebut());
        assertNull(bloc.dateFin());
    }

    @Test
    void singleBlocAvecDateFin() {
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("3760205420719", "tous les lots", "date de durabilité minimale", "2026-04-14", "2026-07-17"), "alimentation");
        assertEquals("single_bloc", result.pattern());
        assertEquals(1, result.blocs().size());
        ProduitIdentifie bloc = result.blocs().getFirst();
        assertEquals("3760205420719", bloc.gtin());
        assertEquals("tous les lots", bloc.lot());
        assertEquals("date de durabilité minimale", bloc.typeDate());
        assertEquals("2026-04-14", bloc.dateDebut());
        assertEquals("2026-07-17", bloc.dateFin());
    }

    @Test
    void multiBlocs() {
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("3760205423185", "lot A", "date de durabilité minimale", "2026-04-14", "2026-07-17",
                        "3760205423208", "lot B", "date de durabilité minimale", "2026-04-14", "2026-07-17"),
                "alimentation");
        assertEquals("multi_blocs", result.pattern());
        assertEquals(2, result.blocs().size());
        assertEquals("3760205423185", result.blocs().get(0).gtin());
        assertEquals("lot A", result.blocs().get(0).lot());
        assertEquals("3760205423208", result.blocs().get(1).gtin());
        assertEquals("lot B", result.blocs().get(1).lot());
    }

    @Test
    void sansGtin() {
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("lot 2302", "date limite de consommation", "2026-05-03"), "alimentation");
        assertEquals("single_bloc", result.pattern());
        assertEquals(1, result.blocs().size());
        ProduitIdentifie bloc = result.blocs().getFirst();
        assertNull(bloc.gtin());
        assertEquals("lot 2302", bloc.lot());
        assertEquals("date limite de consommation", bloc.typeDate());
        assertEquals("2026-05-03", bloc.dateDebut());
    }

    @Test
    void nonConcerne() {
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("3614513483976", "non concerné"), "hygiène-beauté");
        assertEquals("single_bloc", result.pattern());
        assertEquals(1, result.blocs().size());
        ProduitIdentifie bloc = result.blocs().getFirst();
        assertEquals("3614513483976", bloc.gtin());
        assertNull(bloc.lot());
        assertEquals("non concerné", bloc.typeDate());
        assertNull(bloc.dateDebut());
    }

    @Test
    void nonConcerneAvecLot() {
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("7908024942296", "tous les lots", "non concerné"), "bébés-enfants (hors alimentaire)");
        assertEquals("single_bloc", result.pattern());
        ProduitIdentifie bloc = result.blocs().getFirst();
        assertEquals("7908024942296", bloc.gtin());
        assertEquals("tous les lots", bloc.lot());
        assertEquals("non concerné", bloc.typeDate());
    }

    @Test
    void avecSepPipe() {
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("d60760286", "date limite de consommation", "2026-03-25",
                        "|", "d60780361", "date limite de consommation", "2026-03-27"),
                "alimentation");
        assertEquals("multi_blocs", result.pattern());
        assertEquals(2, result.blocs().size());
        assertEquals("d60760286", result.blocs().get(0).lot());
        assertEquals("2026-03-25", result.blocs().get(0).dateDebut());
        assertEquals("d60780361", result.blocs().get(1).lot());
        assertEquals("2026-03-27", result.blocs().get(1).dateDebut());
    }

    @Test
    void gtin14ChiffresDegenereTreateCommeLot() {
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("32599330020173", "tous les lots", "date limite de consommation", "2026-03-13", "2026-04-03"),
                "alimentation");
        assertEquals(1, result.blocs().size());
        ProduitIdentifie bloc = result.blocs().getFirst();
        assertEquals("32599330020173", bloc.gtin());
        assertEquals("tous les lots", bloc.lot());
        assertEquals("date limite de consommation", bloc.typeDate());
        assertEquals("2026-03-13", bloc.dateDebut());
        assertEquals("2026-04-03", bloc.dateFin());
    }

    @Test
    void startingWithTypeDateNoGtinNoLot() {
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("date limite de consommation", "2026-05-03"), "alimentation");
        assertEquals("single_bloc", result.pattern());
        assertEquals(1, result.blocs().size());
        ProduitIdentifie bloc = result.blocs().getFirst();
        assertNull(bloc.gtin());
        assertNull(bloc.lot());
        assertEquals("date limite de consommation", bloc.typeDate());
        assertEquals("2026-05-03", bloc.dateDebut());
    }

    @Test
    void singleBlocLotOnly() {
        // Single bloc with only a GTIN and a LOT, no date → detectPattern should return texte_libre or incomplet
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("3271620030518", "lot ABC"), "jouets");
        assertEquals(1, result.blocs().size());
        ProduitIdentifie bloc = result.blocs().getFirst();
        assertEquals("3271620030518", bloc.gtin());
        assertEquals("lot ABC", bloc.lot());
        assertNull(bloc.typeDate());
        assertNull(bloc.dateDebut());
    }

    @Test
    void lotConcatenationWhenTypeDateAlreadySet() {
        // GTIN, lot, typeDate, then another LOT → should flush to new bloc
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("3271620030518", "lot A", "date limite de consommation", "2026-05-01",
                        "lot B", "date limite de consommation", "2026-06-01"),
                "alimentation");
        assertEquals("multi_blocs", result.pattern());
        assertEquals(2, result.blocs().size());
        assertEquals("lot A", result.blocs().get(0).lot());
        assertEquals("lot B", result.blocs().get(1).lot());
    }

    @Test
    void lotAppendedWhenNoTypeDateYet() {
        // GTIN then two consecutive LOT tokens (no typeDate set yet) → should concatenate
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("3271620030518", "lot A", "suite lot", "date limite de consommation", "2026-05-01"),
                "alimentation");
        assertEquals("single_bloc", result.pattern());
        assertEquals(1, result.blocs().size());
        assertEquals("lot A suite lot", result.blocs().getFirst().lot());
    }

    @Test
    void pipeColleAuToken() {
        IdentificationProduits result = IdentificationProduitsParser.parse(
                List.of("|3666085407515", "tous les lots", "non concerné"), "maison");
        assertEquals("single_bloc", result.pattern());
        ProduitIdentifie bloc = result.blocs().getFirst();
        assertEquals("3666085407515", bloc.gtin());
        assertEquals("tous les lots", bloc.lot());
        assertEquals("non concerné", bloc.typeDate());
    }
}
