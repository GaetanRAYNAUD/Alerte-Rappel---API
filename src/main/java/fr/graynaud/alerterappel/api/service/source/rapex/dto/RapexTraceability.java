package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexTraceability(
        @JsonProperty("id") Long id,
        @JsonProperty("countryOrigin") RapexCountry countryOrigin,
        @JsonProperty("isSoldOnline") RapexKeyName isSoldOnline,
        @JsonProperty("onlineTrader") String onlineTrader
) {}
