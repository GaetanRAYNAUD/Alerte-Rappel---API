package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexRiskVersion(
        @JsonProperty("id") Long id,
        @JsonProperty("language") RapexKeyName language,
        @JsonProperty("riskDescription") String riskDescription,
        @JsonProperty("legalProvision") String legalProvision,
        @JsonProperty("riskTypeOther") String riskTypeOther
) {}
