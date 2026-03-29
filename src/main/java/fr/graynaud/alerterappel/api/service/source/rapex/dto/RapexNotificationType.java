package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexNotificationType(
        @JsonProperty("key") String key,
        @JsonProperty("code") String code,
        @JsonProperty("name") String name
) {}
