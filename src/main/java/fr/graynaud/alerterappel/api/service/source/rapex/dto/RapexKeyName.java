package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexKeyName(
        @JsonProperty("key") String key,
        @JsonProperty("name") String name
) {}
