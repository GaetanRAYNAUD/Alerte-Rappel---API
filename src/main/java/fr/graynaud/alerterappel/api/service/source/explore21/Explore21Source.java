package fr.graynaud.alerterappel.api.service.source.explore21;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;

public interface Explore21Source {

    @JsonIgnore
    OffsetDateTime getLastDate();
}
