package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RapexMeasureTaken(
        @JsonProperty("id") Long id,
        @JsonProperty("hasPublishedRecallOnline") RapexKeyName hasPublishedRecallOnline,
        @JsonProperty("measures") List<RapexMeasure> measures,
        @JsonProperty("companyRecalls") List<RapexCompanyRecall> companyRecalls
) {}
