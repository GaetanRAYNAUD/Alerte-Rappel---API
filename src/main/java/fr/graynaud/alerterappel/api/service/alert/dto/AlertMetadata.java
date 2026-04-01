package fr.graynaud.alerterappel.api.service.alert.dto;


import java.util.List;

/**
 * Métadonnées techniques sur l'enregistrement unifié.
 *
 * @param sources         Liste des sources ayant contribué à cet enregistrement
 * @param rappelconsoGuid GUID unique RappelConso (champ {@code rappel_guid})
 */
public record AlertMetadata(List<AlertMetadataSource> sources, String rappelconsoGuid) {}
