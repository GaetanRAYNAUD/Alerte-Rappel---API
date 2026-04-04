package fr.graynaud.alerterappel.api.service.alert.dto;


import java.util.List;

/**
 * Images et documents associés au rappel.
 *
 * @param photos         URLs des photos du produit. RAPEX : {@code product.photos[].id} préfixé par
 *                       {@code https://ec.europa.eu/safety-gate-alerts/public/api/notification/image/}. RappelConso : {@code liens_vers_les_images} (URLs
 *                       séparées par {@code |})
 * @param recallSheetUrl Lien vers la fiche officielle du rappel. RAPEX : {@code reference} préfixé par
 *                       {@code https://ec.europa.eu/safety-gate-alerts/screen/webReport/alertDetail/}. RappelConso : {@code lien_vers_la_fiche_rappel}
 * @param recallPdfUrl   Lien vers l'affichette PDF du rappel. RappelConso uniquement : {@code lien_vers_affichette_pdf}
 */
public record AlertMedia(List<String> photos, String recallSheetUrl, String recallPdfUrl) {}
