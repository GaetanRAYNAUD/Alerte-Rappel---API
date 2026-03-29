package fr.graynaud.alerterappel.api.service.alert.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Informations sur le produit concerné par le rappel.
 *
 * @param specificName         Nom commercial précis du produit rappelé.
 *                             Exemples : {@code HAPPY LUTINS FARCEURS SAPERLI & POPETTE}, {@code Grande Panda, 600}.
 *                             RAPEX : {@code product.nameSpecific}, RappelConso : {@code libelle}
 * @param type                 Nom générique ou type du produit (source : RAPEX {@code product.versions.name}).
 *                             Exemples : {@code Voiture particulière}, {@code Jouet souple}, {@code Poupée}
 * @param description          Description détaillée.
 *                             RAPEX : {@code product.versions.description}, RappelConso : {@code modeles_ou_references}
 * @param brand                Marque(s) du produit.
 *                             Exemples : {@code Bmw}, {@code JOURDAIN}.
 *                             RAPEX : {@code product.brands[0].brand}, RappelConso : {@code marque_produit}
 * @param family               Famille du produit (source : RappelConso {@code categorie_produit}).
 *                             Exemple : {@code bébés-enfants (hors alimentaire)}
 * @param category             Catégorie du produit.
 *                             Les valeurs RAPEX sont des clés structurées (ex. {@code product.category.toys}),
 *                             RappelConso retourne du texte libre (ex. {@code automobiles, motos, scooters}).
 *                             RAPEX : {@code product.productCategory.key}, RappelConso : {@code sous_categorie_produit}
 * @param counterfeit          Le produit est-il une contrefaçon ? (source : RAPEX {@code product.isCounterfeit.key})
 * @param barcodes             Codes-barres EAN associés au produit.
 *                             Exemples : {@code ["760258386219"]}, {@code ["3385533625584"]}.
 *                             RAPEX : {@code product.barcodes[].barcode}, RappelConso : {@code identification_produits}
 * @param batchNumbers         Numéros de lot concernés.
 *                             Exemples : {@code ["25-D0069"]}, {@code ["09.11.1998 - 19.12.2003"]}.
 *                             RAPEX : {@code product.batchNumbers[].batchNumber}, RappelConso : {@code identification_produits}
 * @param modelReferences      Références de modèles ou homologations type-approval.
 *                             RAPEX : {@code product.modelTypes[].modelType}, RappelConso : {@code informations_complementaires}
 * @param packagingDescription Description de l'emballage.
 *                             Exemples : {@code Sac en plastique.}, {@code Support en carton.}.
 *                             RAPEX : {@code product.versions.packageDescription}, RappelConso : {@code conditionnements}
 * @param productionDates      Dates ou plages de production. Peut contenir plusieurs lignes si plusieurs variantes sont concernées.
 *                             Source : RappelConso {@code identification_produits}
 */
public record AlertProduct(
        @JsonProperty("specific_name") String specificName,
        @JsonProperty("type") String type,
        @JsonProperty("description") String description,
        @JsonProperty("brand") String brand,
        @JsonProperty("family") String family,
        @JsonProperty("category") String category,
        @JsonProperty("counterfeit") Boolean counterfeit,
        @JsonProperty("barcodes") List<String> barcodes,
        @JsonProperty("batch_numbers") List<String> batchNumbers,
        @JsonProperty("model_references") List<String> modelReferences,
        @JsonProperty("packaging_description") String packagingDescription,
        @JsonProperty("production_dates") String productionDates
) {}
