package fr.graynaud.alerterappel.api.service.source.rappelconso;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.config.properties.RappelConsoProperties;
import fr.graynaud.alerterappel.api.service.source.explore21.Explore21Service;
import fr.graynaud.alerterappel.api.service.source.rappelconso.dto.RappelConsoData;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RappelConsoService extends Explore21Service<RappelConsoData> {

    public RappelConsoService(RestClient.Builder restClientBuilder, RappelConsoProperties properties, DataProperties dataProperties,
                              JsonMapper jsonMapper, TaskScheduler taskScheduler) throws IOException {
        super(restClientBuilder, properties, dataProperties, jsonMapper, taskScheduler, "RappelConso", RappelConsoData.class, "date_publication");
    }

    @Override
    protected void handleNewData(OffsetDateTime since) throws IOException {
        // TODO
    }
}
