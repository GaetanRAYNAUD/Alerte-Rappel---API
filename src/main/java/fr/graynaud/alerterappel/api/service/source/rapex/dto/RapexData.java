package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import fr.graynaud.alerterappel.api.service.source.explore21.Explore21Source;
import java.time.OffsetDateTime;

public class RapexData implements Explore21Source {

    private OffsetDateTime lastModificationDate;

    public OffsetDateTime getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(OffsetDateTime lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    @Override
    public OffsetDateTime getLastDate() {
        return lastModificationDate;
    }
}
