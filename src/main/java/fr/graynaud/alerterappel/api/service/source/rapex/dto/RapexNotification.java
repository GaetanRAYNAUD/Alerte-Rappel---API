package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record RapexNotification(
        @JsonProperty("id") Long id,
        @JsonProperty("notificationType") RapexNotificationType notificationType,
        @JsonProperty("reference") String reference,
        @JsonProperty("corrigendum") String corrigendum,
        @JsonProperty("country") RapexCountry country,
        @JsonProperty("creationDate") OffsetDateTime creationDate,
        @JsonProperty("publicationDate") OffsetDateTime publicationDate,
        @JsonProperty("modificationDate") OffsetDateTime modificationDate,
        @JsonProperty("isImmediatePublication") Boolean isImmediatePublication,
        @JsonProperty("singlePublication") Boolean singlePublication,
        @JsonProperty("forInfo") Boolean forInfo,
        @JsonProperty("product") RapexProduct product,
        @JsonProperty("risk") RapexRisk risk,
        @JsonProperty("measureTaken") RapexMeasureTaken measureTaken,
        @JsonProperty("traceability") RapexTraceability traceability,
        @JsonProperty("reactingCountries") List<RapexReactingCountry> reactingCountries,
        @JsonProperty("webReport") RapexWebReport webReport,
        @JsonProperty("versions") List<RapexNotificationVersion> versions
) {}
