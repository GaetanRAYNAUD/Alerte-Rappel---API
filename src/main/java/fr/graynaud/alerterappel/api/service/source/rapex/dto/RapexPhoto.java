package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexPhoto(
        @JsonProperty("id") Long id,
        @JsonProperty("mainPicture") Boolean mainPicture,
        @JsonProperty("fileName") String fileName,
        @JsonProperty("path") String path
) {}
