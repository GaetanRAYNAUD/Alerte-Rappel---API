package fr.graynaud.alerterappel.api.service.source.rappelconso.dto;

import fr.graynaud.alerterappel.api.service.source.explore21.Explore21Source;
import java.time.OffsetDateTime;

public class RappelConsoData implements Explore21Source {

    private OffsetDateTime lastPublishData;

    public OffsetDateTime getLastPublishData() {
        return lastPublishData;
    }

    public void setLastPublishData(OffsetDateTime lastPublishData) {
        this.lastPublishData = lastPublishData;
    }

    @Override
    public OffsetDateTime getLastDate() {
        return lastPublishData;
    }
}
