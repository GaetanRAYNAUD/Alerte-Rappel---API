package fr.graynaud.alerterappel.api.service.source.rappelconso.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertCommercialization;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMeasureItem;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMeasures;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMedia;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadata;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadataSource;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertProduct;
import fr.graynaud.alerterappel.api.service.source.rappelconso.RappelConsoService;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
) {

    public Alert toAlert() {
        AlertMetadataSource source = new AlertMetadataSource(RappelConsoService.SOURCE_NAME, this.id, this.lienVersLaFicheRappel, OffsetDateTime.now(), this.numeroVersion);
        AlertMetadata metadata = new AlertMetadata(List.of(source), this.rappelGuid);

        List<String> risks = this.risquesEncourus == null ? null
                                                          : Arrays.stream(this.risquesEncourus.split(","))
                                                                  .map(StringUtils::trimToNull)
                                                                  .filter(StringUtils::isNotBlank)
                                                                  .toList();

        IdentificationProduits identification = IdentificationProduitsParser.parse(this.identificationProduits, this.categorieProduit);
        List<ProduitIdentifie> blocs = identification.blocs();

        List<String> barcodes = blocs.stream().map(ProduitIdentifie::gtin).filter(Objects::nonNull).toList();
        List<String> batchNumbers = blocs.stream().map(ProduitIdentifie::lot).filter(Objects::nonNull).toList();
        String productionDates = blocs.stream()
                                      .filter(b -> b.dateDebut() != null)
                                      .map(b -> {
                                          String line = b.lot() != null ? b.lot() + ": " : "";
                                          line += b.dateDebut();
                                          if (b.dateFin() != null) {
                                              line += " - " + b.dateFin();
                                          }
                                          return line;
                                      })
                                      .collect(Collectors.joining("\n"));

        AlertProduct product = new AlertProduct(
                this.libelle,
                null,
                this.modelesOuReferences,
                this.marqueProduit,
                this.categorieProduit,
                this.sousCategoreProduit,
                null,
                barcodes.isEmpty() ? null : barcodes,
                batchNumbers.isEmpty() ? null : batchNumbers,
                this.informationsComplementaires == null ? null : List.of(this.informationsComplementaires),
                this.conditionnements,
                productionDates.isEmpty() ? null : productionDates
        );

        AlertCommercialization commercialization = new AlertCommercialization(
                null,
                "France",
                this.zoneGeographiqueDeVente == null ? null : List.of(this.zoneGeographiqueDeVente),
                null,
                this.dateDebutCommercialisation,
                this.dateDateFinCommercialisation,
                this.distributeurs
        );

        AlertMeasures measures = new AlertMeasures(
                null,
                this.natureJuridiqueRappel == null ? null : List.of(new AlertMeasureItem(null, null, this.natureJuridiqueRappel, null)),
                null,
                this.conduitesATenirParLeConsommateur,
                this.modalitesDeCompensation,
                this.dateDeLaProcedureDeRappel
        );

        List<String> photos = this.liensVersLesImages == null ? null
                                                              : Arrays.stream(this.liensVersLesImages.split("\\|"))
                                                                      .map(String::trim)
                                                                      .filter(s -> !s.isEmpty())
                                                                      .toList();
        AlertMedia media = new AlertMedia(photos, this.lienVersLaFicheRappel);

        return new Alert(
                metadata,
                this.numeroFiche == null ? null : this.numeroFiche.toUpperCase(),
                this.datePublication,
                risks,
                this.motifRappel,
                this.descriptionComplementaireRisque,
                product,
                commercialization,
                measures,
                media,
                this.informationsComplementairesPubliques
        );
    }
}
