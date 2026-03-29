package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexReactingCountry(
        @JsonProperty("id") Long id,
        @JsonProperty("country") RapexCountry country
) {}
