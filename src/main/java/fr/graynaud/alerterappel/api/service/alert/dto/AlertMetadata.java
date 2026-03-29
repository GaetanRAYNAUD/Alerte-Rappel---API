package fr.graynaud.alerterappel.api.service.alert.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Métadonnées techniques sur l'enregistrement unifié.
 *
 * @param sources         Liste des sources ayant contribué à cet enregistrement
 * @param rappelconsoGuid GUID unique RappelConso (champ {@code rappel_guid})
 */
public record AlertMetadata(
        @JsonProperty("sources") List<AlertMetadataSource> sources,
        @JsonProperty("rappelconso_guid") String rappelconsoGuid
) {}
