package fr.graynaud.alerterappel.api.service.alert.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

/**
 * Mesures prises et consignes à destination du consommateur.
 *
 * @param recallPublishedOnline L'entreprise a-t-elle publié un rappel sur son site internet ?
 *                              Source : RAPEX {@code measureTaken.hasPublishedRecallOnline.key == option.yes}
 * @param measuresList          Liste des mesures détaillées ordonnées (source : RAPEX {@code measureTaken.measures[]})
 * @param companyRecalls        Liens vers les pages de rappel publiées par l'entreprise (source : RAPEX {@code measureTaken.companyRecalls[]})
 * @param consumerActions       Consignes à destination du consommateur : que faire du produit, comment obtenir un remboursement ou une réparation.
 *                              Source : RappelConso {@code conduites_a_tenir_par_le_consommateur}
 * @param compensationTerms     Modalités de compensation proposées (source : RappelConso {@code modalites_de_compensation})
 * @param procedureEndDate      Date limite de la procédure de rappel (source : RappelConso {@code date_de_fin_de_la_procedure_de_rappel})
 */
public record AlertMeasures(
        @JsonProperty("recall_published_online") Boolean recallPublishedOnline,
        @JsonProperty("measures_list") List<AlertMeasureItem> measuresList,
        @JsonProperty("company_recalls") List<AlertCompanyRecall> companyRecalls,
        @JsonProperty("consumer_actions") String consumerActions,
        @JsonProperty("compensation_terms") String compensationTerms,
        @JsonProperty("procedure_end_date") LocalDate procedureEndDate
) {}
