package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexCountry(
        @JsonProperty("key") String key,
        @JsonProperty("propertyKey") String propertyKey,
        @JsonProperty("name") String name,
        @JsonProperty("orderIndex") Integer orderIndex,
        @JsonProperty("euCountry") Boolean euCountry,
        @JsonProperty("eeaCountry") Boolean eeaCountry
) {}
