package fr.graynaud.alerterappel.api.service.source.rappelconso.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record RappelConsoRappel(
        @JsonProperty("id") Long id,
        @JsonProperty("rappel_guid") String rappelGuid,
        @JsonProperty("numero_fiche") String numeroFiche,
        @JsonProperty("numero_version") Integer numeroVersion,
        @JsonProperty("libelle") String libelle,
        @JsonProperty("date_publication") OffsetDateTime datePublication,
        @JsonProperty("nature_juridique_rappel") String natureJuridiqueRappel,
        @JsonProperty("categorie_produit") String categorieProduit,
        @JsonProperty("sous_categorie_produit") String sousCategoreProduit,
        @JsonProperty("marque_produit") String marqueProduit,
        @JsonProperty("modeles_ou_references") String modelesOuReferences,
        @JsonProperty("identification_produits") List<String> identificationProduits,
        @JsonProperty("conditionnements") String conditionnements,
        @JsonProperty("date_debut_commercialisation") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateDebutCommercialisation,
        @JsonProperty("date_date_fin_commercialisation") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateDateFinCommercialisation,
        @JsonProperty("temperature_conservation") String temperatureConservation,
        @JsonProperty("marque_salubrite") String marqueSalubrite,
        @JsonProperty("informations_complementaires") String informationsComplementaires,
        @JsonProperty("zone_geographique_de_vente") String zoneGeographiqueDeVente,
        @JsonProperty("distributeurs") String distributeurs,
        @JsonProperty("motif_rappel") String motifRappel,
        @JsonProperty("risques_encourus") String risquesEncourus,
        @JsonProperty("preconisations_sanitaires") String preconisationsSanitaires,
        @JsonProperty("description_complementaire_risque") String descriptionComplementaireRisque,
        @JsonProperty("conduites_a_tenir_par_le_consommateur") String conduitesATenirParLeConsommateur,
        @JsonProperty("numero_contact") String numeroContact,
        @JsonProperty("modalites_de_compensation") String modalitesDeCompensation,
        @JsonProperty("date_de_fin_de_la_procedure_de_rappel") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateDeLaProcedureDeRappel,
        @JsonProperty("informations_complementaires_publiques") String informationsComplementairesPubliques,
        @JsonProperty("liens_vers_les_images") String liensVersLesImages,
        @JsonProperty("lien_vers_la_liste_des_produits") String lienVersLaListeDesProduits,
        @JsonProperty("lien_vers_la_liste_des_distributeurs") String lienVersLaListeDesDistributeurs,
        @JsonProperty("lien_vers_affichette_pdf") String lienVersAffichettePdf,
        @JsonProperty("lien_vers_la_fiche_rappel") String lienVersLaFicheRappel
) {}
