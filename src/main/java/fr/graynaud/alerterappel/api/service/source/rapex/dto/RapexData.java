package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import java.time.OffsetDateTime;

public class RapexData {

    private OffsetDateTime lastModificationDate;

    public OffsetDateTime getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(OffsetDateTime lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }
}
