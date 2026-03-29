package fr.graynaud.alerterappel.api.service.alert.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lien vers une page de rappel publiée par l'entreprise elle-même.
 * Source : RAPEX {@code measureTaken.companyRecalls[]}
 *
 * @param url      URL de la page de rappel entreprise
 * @param language Code langue de la page. Exemple : {@code FR}
 */
public record AlertCompanyRecall(
        @JsonProperty("url") String url,
        @JsonProperty("language") String language
) {}
