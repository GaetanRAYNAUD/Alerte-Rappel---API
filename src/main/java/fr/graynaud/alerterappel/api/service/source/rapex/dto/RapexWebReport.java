package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record RapexWebReport(
        @JsonProperty("id") Long id,
        @JsonProperty("code") String code,
        @JsonProperty("internalComments") String internalComments,
        @JsonProperty("corrigendum") String corrigendum,
        @JsonProperty("publicationDate") OffsetDateTime publicationDate,
        @JsonProperty("lastPublicationDate") OffsetDateTime lastPublicationDate,
        @JsonProperty("notifications") Object notifications,
        @JsonProperty("highlighted") Boolean highlighted,
        @JsonProperty("status") RapexKeyName status
) {}
