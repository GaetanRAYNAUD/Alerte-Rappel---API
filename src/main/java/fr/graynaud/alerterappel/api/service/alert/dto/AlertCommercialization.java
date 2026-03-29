package fr.graynaud.alerterappel.api.service.alert.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

/**
 * Informations sur la commercialisation et la traçabilité du produit.
 *
 * @param originCountryName  Nom du pays de fabrication du produit (source : RAPEX {@code traceability.countryOrigin.name}).
 *                           Exemples : {@code People's Republic of China}, {@code Germany}
 * @param alertCountryName   Nom du pays ayant émis l'alerte.
 *                           Exemple : {@code France}.
 *                           RAPEX : {@code country.name}, RappelConso : toujours {@code France}
 * @param reactingCountries  Pays ayant réagi à l'alerte (codes ISO) ou zone géographique de vente.
 *                           Exemple : {@code ["BG", "DE", "HR", "HU", "LU", "PT", "SE", "SI", "SK"]}.
 *                           RAPEX : {@code reactingCountries[].country.key}, RappelConso : {@code zone_geographique_de_vente}
 * @param soldOnline         Le produit était-il vendu en ligne ? (source : RAPEX {@code traceability.isSoldOnline.key}).
 *                           Valeurs possibles : {@code option.yes}, {@code option.no}, {@code option.unknown}
 * @param marketingStartDate Date de début de commercialisation (source : RappelConso)
 * @param marketingEndDate   Date de fin de commercialisation (source : RappelConso)
 * @param distributors       Liste textuelle des distributeurs concernés (source : RappelConso)
 */
public record AlertCommercialization(
        @JsonProperty("origin_country_name") String originCountryName,
        @JsonProperty("alert_country_name") String alertCountryName,
        @JsonProperty("reacting_countries") List<String> reactingCountries,
        @JsonProperty("sold_online") Boolean soldOnline,
        @JsonProperty("marketing_start_date") LocalDate marketingStartDate,
        @JsonProperty("marketing_end_date") LocalDate marketingEndDate,
        @JsonProperty("distributors") String distributors
) {}
