package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record RapexNotificationSummary(
        @JsonProperty("modification_date") OffsetDateTime modificationDate,
        @JsonProperty("rapex_url") String rapexUrl
) {}
