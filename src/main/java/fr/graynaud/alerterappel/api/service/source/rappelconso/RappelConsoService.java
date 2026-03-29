package fr.graynaud.alerterappel.api.service.source.rappelconso;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.config.properties.RappelConsoProperties;
import fr.graynaud.alerterappel.api.service.source.explore21.Explore21Response;
import fr.graynaud.alerterappel.api.service.source.rappelconso.dto.RappelConsoData;
import fr.graynaud.alerterappel.api.service.source.rappelconso.dto.RappelConsoRappel;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RappelConsoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RappelConsoService.class);

    private final RestClient restClient;

    private final Path dataPath;

    private final JsonMapper jsonMapper;

    public RappelConsoService(RestClient.Builder restClientBuilder, RappelConsoProperties properties, DataProperties dataProperties, JsonMapper jsonMapper) throws IOException {
        this.restClient = properties.restClientBuilder(restClientBuilder).build();
        this.dataPath = dataProperties.getSourcePath(properties);
        this.jsonMapper = jsonMapper;
        checkNewData();
    }

    public void checkNewData() {
        RappelConsoData data = this.jsonMapper.readValue(this.dataPath.toFile(), RappelConsoData.class);

        try {
            Explore21Response<RappelConsoRappel> response =
                    this.restClient.get()
                                   .uri(b -> b.path("/records")
                                              .queryParam("where",
                                                          "date_publication " +
                                                          (data.getLastPublishData() == null ? "is not null" : "> \"" + data.getLastPublishData() + "\""))
                                              .queryParam("order_by", "date_publication DESC")
                                              .queryParam("limit", 0)
                                              .build())
                                   .retrieve()
                                   .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.totalCount() != null && response.totalCount() > 0) {
                LOGGER.info("New data for RappelConso, exporting {} data since {}", response.totalCount(), data.getLastPublishData());
            }
        } catch (Exception e) {
            LOGGER.error("Error while checking new data for RappelConso", e);
        }
    }
}
