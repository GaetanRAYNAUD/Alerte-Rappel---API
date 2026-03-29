package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexProductVersion(
        @JsonProperty("id") Long id,
        @JsonProperty("language") RapexKeyName language,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("packageDescription") String packageDescription,
        @JsonProperty("productCategoryOther") String productCategoryOther
) {}
