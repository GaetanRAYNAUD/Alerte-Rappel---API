package fr.graynaud.alerterappel.api.service.alert.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * Source ayant contribué à un enregistrement unifié.
 *
 * @param origin        Identifiant de la source. Valeurs : {@code rapex}, {@code rappelconso}
 * @param sourceId      Identifiant interne à la source.
 *                      RAPEX : id notification (ex. {@code 10098375}), RappelConso : id (ex. {@code 49410})
 * @param url           URL vers la fiche source originale
 * @param importDate    Date à laquelle l'enregistrement a été importé depuis cette source
 * @param versionNumber Numéro de version de la fiche dans cette source (source : RappelConso {@code numero_version})
 */
public record AlertMetadataSource(
        @JsonProperty("origin") String origin,
        @JsonProperty("source_id") Long sourceId,
        @JsonProperty("url") String url,
        @JsonProperty("import_date") OffsetDateTime importDate,
        @JsonProperty("version_number") Integer versionNumber
) {}
