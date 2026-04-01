package fr.graynaud.alerterappel.api.service.alert.dto;


import java.time.LocalDate;

/**
 * Mesure individuelle prise dans le cadre d'un rappel produit.
 *
 * @param category      Catégorie de la mesure (source : RAPEX {@code measureTaken.measures[].measureCategory.key}). Valeurs :
 *                      {@code measure.category.recall.of.product.from.consumers}, {@code measure.category.withdrawal.of.product.from.market},
 *                      {@code measure.category.removal.from.online.marketplace}, {@code measure.category.warning.consumers.of.risks},
 *                      {@code measure.category.other}
 * @param otherCategory Précision textuelle si la catégorie est {@code other} (source : RAPEX {@code versions.measureCategoryOther}). Exemple :
 *                      {@code Demande d'arrêt de la conduite}
 * @param type          Caractère volontaire ou obligatoire de la mesure. RAPEX : {@code measureTaken.measures[].measureType.key} (valeurs :
 *                      {@code measure.type.voluntary}, {@code measure.type.compulsory}), RappelConso : {@code nature_juridique_rappel} (valeurs :
 *                      {@code volontaire}, {@code imposé par arrêté préfectoral})
 * @param effectiveDate Date d'entrée en vigueur de la mesure (source : RAPEX {@code entryIntoForceDate})
 */
public record AlertMeasureItem(String category, String otherCategory, String type, LocalDate effectiveDate) {}
