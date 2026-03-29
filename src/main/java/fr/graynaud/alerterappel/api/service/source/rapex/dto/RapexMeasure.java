package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record RapexMeasure(
        @JsonProperty("id") Long id,
        @JsonProperty("entryIntoForceDateKnown") Boolean entryIntoForceDateKnown,
        @JsonProperty("entryIntoForceDate") OffsetDateTime entryIntoForceDate,
        @JsonProperty("measureDuration") String measureDuration,
        @JsonProperty("measureScope") String measureScope,
        @JsonProperty("measureCategory") RapexKeyName measureCategory,
        @JsonProperty("measureType") RapexKeyName measureType,
        @JsonProperty("measureVoluntaryEconomicOperator") RapexKeyName measureVoluntaryEconomicOperator,
        @JsonProperty("measureVoluntaryAuthority") RapexKeyName measureVoluntaryAuthority,
        @JsonProperty("measureCompulsoryEconomicOperator") RapexKeyName measureCompulsoryEconomicOperator,
        @JsonProperty("measureCompulsoryAuthority") RapexKeyName measureCompulsoryAuthority,
        @JsonProperty("versions") List<RapexMeasureVersion> versions
) {}
