package fr.graynaud.alerterappel.api.service.source.explore21;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Explore21Response<T>(@JsonProperty("total_count") Long totalCount, @JsonProperty("results") List<T> results) {
}
