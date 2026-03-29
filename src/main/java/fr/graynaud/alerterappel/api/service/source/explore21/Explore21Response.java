package fr.graynaud.alerterappel.api.service.source.explore21;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Explore21Response<T> {

    @JsonProperty("total_count")
    private final Long totalCount;

    @JsonProperty("results")
    private final List<T> results;

    public Explore21Response(@JsonProperty("total_count") Long totalCount, @JsonProperty("results") List<T> results) {
        this.totalCount = totalCount;
        this.results = results;
    }

    @JsonProperty("total_count")
    public Long totalCount() {return totalCount;}

    @JsonProperty("results")
    public List<T> results() {return results;}
}
