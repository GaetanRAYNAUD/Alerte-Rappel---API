package fr.graynaud.alerterappel.api.service.alert.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Schéma unifié pour les rappels de produits issus de Safety Gate (RAPEX) et RappelConso.
 *
 * @param metadata                     Métadonnées techniques sur l'enregistrement unifié
 * @param alertNumber                  Numéro d'alerte commun aux deux sources (clé de jointure). Toujours normalisé en majuscules.
 *                                     Exemples : {@code SR/00842/26}, {@code SR/00939/26}.
 *                                     RAPEX : {@code reference}, RappelConso : {@code numero_fiche}
 * @param versionNumber                Numéro de version de la fiche (source : RappelConso {@code numero_version})
 * @param publicationDate              Date de publication officielle de l'alerte.
 *                                     RAPEX : {@code publicationDate}, RappelConso : {@code date_publication}
 * @param risks                        Types de risques identifiés, normalisés en tableau de clés.
 *                                     Exemples : {@code ["riskType.choking"]}, {@code ["riskType.injuries"]}, {@code ["riskType.chemical"]}.
 *                                     RAPEX : {@code risk.riskType[].key}, RappelConso : {@code risques_encourus} (splitté sur virgule)
 * @param riskDescription              Description narrative du risque.
 *                                     RAPEX : {@code risk.versions.riskDescription}, RappelConso : {@code motif_rappel}
 * @param supplementaryRiskDescription Informations complémentaires sur le risque (source : RappelConso {@code description_complementaire_risque})
 * @param product                      Informations sur le produit concerné
 * @param commercialization            Informations sur la commercialisation et la traçabilité
 * @param measures                     Mesures prises et consignes
 * @param media                        Images et documents associés
 * @param additionalInformation        Informations complémentaires publiques (source : RappelConso {@code informations_complementaires_publiques})
 */
public record Alert(
        @JsonProperty("_metadata") AlertMetadata metadata,
        @JsonProperty("alert_number") String alertNumber,
        @JsonProperty("version_number") Integer versionNumber,
        @JsonProperty("publication_date") OffsetDateTime publicationDate,
        @JsonProperty("risks") List<String> risks,
        @JsonProperty("risk_description") String riskDescription,
        @JsonProperty("supplementary_risk_description") String supplementaryRiskDescription,
        @JsonProperty("product") AlertProduct product,
        @JsonProperty("commercialization") AlertCommercialization commercialization,
        @JsonProperty("measures") AlertMeasures measures,
        @JsonProperty("media") AlertMedia media,
        @JsonProperty("additional_information") String additionalInformation
) {}
