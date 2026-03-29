package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexCompanyRecall(
        @JsonProperty("id") Long id,
        @JsonProperty("link") String link,
        @JsonProperty("language") RapexKeyName language
) {}
