package fr.graynaud.alerterappel.api.service.source.rappelconso.dto;

import java.time.OffsetDateTime;

public class RappelConsoData {

    private OffsetDateTime lastPublishData;

    public OffsetDateTime getLastPublishData() {
        return lastPublishData;
    }

    public void setLastPublishData(OffsetDateTime lastPublishData) {
        this.lastPublishData = lastPublishData;
    }
}
