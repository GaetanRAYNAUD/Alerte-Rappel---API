package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexMeasureVersion(
        @JsonProperty("id") Long id,
        @JsonProperty("language") RapexKeyName language,
        @JsonProperty("measureCategoryOther") String measureCategoryOther
) {}
