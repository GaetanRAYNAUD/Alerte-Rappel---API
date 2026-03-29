package fr.graynaud.alerterappel.api.service.source.rapex;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.config.properties.RapexProperties;
import fr.graynaud.alerterappel.api.service.source.explore21.Explore21Service;
import fr.graynaud.alerterappel.api.service.source.rapex.dto.RapexData;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RapexService extends Explore21Service<RapexData> {

    private final RestClient getClient;

    public RapexService(RestClient.Builder restClientBuilder, RapexProperties properties, DataProperties dataProperties,
                        JsonMapper jsonMapper, TaskScheduler taskScheduler) throws IOException {
        super(restClientBuilder, properties, dataProperties, jsonMapper, taskScheduler, "Rapex", RapexData.class, "modification_date");
        this.getClient = restClientBuilder.clone().baseUrl(properties.getGetBaseUrl()).build();
    }

    @Override
    protected void handleNewData(OffsetDateTime since) throws IOException {
        // TODO
    }
}
