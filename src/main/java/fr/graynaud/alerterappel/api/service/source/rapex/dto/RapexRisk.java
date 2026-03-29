package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RapexRisk(
        @JsonProperty("id") Long id,
        @JsonProperty("riskType") List<RapexKeyName> riskType,
        @JsonProperty("versions") List<RapexRiskVersion> versions
) {}
