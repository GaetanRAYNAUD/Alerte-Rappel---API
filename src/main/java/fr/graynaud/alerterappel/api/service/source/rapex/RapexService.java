package fr.graynaud.alerterappel.api.service.source.rapex;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.config.properties.RapexProperties;
import fr.graynaud.alerterappel.api.service.source.explore21.Explore21Response;
import fr.graynaud.alerterappel.api.service.source.rapex.dto.RapexData;
import fr.graynaud.alerterappel.api.service.source.rapex.dto.RapexNotificationSummary;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RapexService {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RapexService.class);

    private final RestClient updateClient;

    private final RestClient getClient;

    private final Path dataPath;

    private final JsonMapper jsonMapper;

    public RapexService(RestClient.Builder restClientBuilder, RapexProperties properties, DataProperties dataProperties, JsonMapper jsonMapper) throws IOException {
        this.updateClient = properties.restClientBuilder(restClientBuilder).build();
        this.getClient = restClientBuilder.baseUrl(properties.getGetBaseUrl()).build();
        this.dataPath = dataProperties.getSourcePath(properties);
        this.jsonMapper = jsonMapper;
        checkNewData();
    }

    public void checkNewData() {
        RapexData data = this.jsonMapper.readValue(this.dataPath.toFile(), RapexData.class);

        try {
            Explore21Response<RapexNotificationSummary> response =
                    this.updateClient.get()
                                     .uri(b -> b.path("/records")
                                             .queryParam("select", "rapex_url, modification_date")
                                                .queryParam("where",
                                                            "modification_date " +
                                                            (data.getLastModificationDate() == null ? "is not null" : ">=" + data.getLastModificationDate()))
                                                .queryParam("order_by", "modification_date DESC")
                                                .queryParam("limit", 1)
                                                .build())
                                     .retrieve()
                                     .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.totalCount() != null && response.totalCount() > 0) {
                LOGGER.info("New data for Rapex, exporting {} data since {}", response.totalCount(), data.getLastModificationDate());
            }
        } catch (Exception e) {
            LOGGER.error("Error while checking new data for Rapex", e);
        }
    }
}
