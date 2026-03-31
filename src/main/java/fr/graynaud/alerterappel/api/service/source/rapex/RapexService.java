package fr.graynaud.alerterappel.api.service.source.rapex;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.config.properties.RapexProperties;
import fr.graynaud.alerterappel.api.service.alert.AlertService;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.source.explore21.Explore21Response;
import fr.graynaud.alerterappel.api.service.source.explore21.Explore21Service;
import fr.graynaud.alerterappel.api.service.source.rapex.dto.RapexData;
import fr.graynaud.alerterappel.api.service.source.rapex.dto.RapexNotification;
import fr.graynaud.alerterappel.api.service.source.rapex.dto.RapexNotificationSummary;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RapexService extends Explore21Service<RapexData> {

    public static final String SOURCE_NAME = "Rapex";

    private static final int MAX_PAGE = 1;

    private final RestClient getClient;

    private final AlertService alertService;

    private final RapexProperties properties;

    public RapexService(RestClient.Builder restClientBuilder, RapexProperties properties, DataProperties dataProperties,
                        JsonMapper jsonMapper, TaskScheduler taskScheduler, AlertService alertService) throws IOException {
        super(restClientBuilder, properties, dataProperties, jsonMapper, taskScheduler, SOURCE_NAME, RapexData.class, "modification_date");
        this.alertService = alertService;
        this.getClient = restClientBuilder.clone().baseUrl(properties.getGetBaseUrl()).build();
        this.properties = properties;
    }

    @EventListener
    public void checkNewData(ApplicationReadyEvent event) {
        super.checkNewData();
    }

    @Override
    protected void handleNewData(OffsetDateTime since, RapexData data) {
        Map<String, String> translations = this.getClient.get()
                                                         .uri(b -> b.pathSegment(this.properties.getTranslationPath(), "fr").build())
                                                         .retrieve()
                                                         .body(Map.class);

        Explore21Response<RapexNotificationSummary> response;
        int page = 0;

        do {
            page++;
            String where = since == null ? this.dateField + " is not null" : this.dateField + " > \"" + since + "\"";
            response = this.updateClient.get()
                                        .uri(b -> b.path("/records")
                                                   .queryParam("where", where)
                                                   .queryParam("select", "modification_date, rapex_url")
                                                   .queryParam("limit", 100)
                                                   .queryParam("order_by", this.dateField + " ASC")
                                                   .build())
                                        .retrieve()
                                        .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.results() == null || response.results().isEmpty() || response.totalCount() == null || response.totalCount() <= 0) {
                break;
            }

            List<Alert> alerts = response.results()
                                         .stream()
                                         .map(RapexNotificationSummary::rapexUrl)
                                         .map(s -> StringUtils.substringAfterLast(s, '/'))
                                         .map(id -> fetchAlert(id, translations))
                                         .filter(Objects::nonNull)
                                         .toList();
            this.alertService.addAlerts(alerts);
            since = response.results().stream().map(RapexNotificationSummary::modificationDate).max(OffsetDateTime::compareTo).orElse(null);

            data.setLastModificationDate(since);
            persist(data);
        } while (page < MAX_PAGE);
    }

    private Alert fetchAlert(String id, Map<String, String> translations) {
        try {
            RapexNotification rapexNotification = this.getClient.get()
                                                                .uri(b -> b.pathSegment(this.properties.getGetPath(), id).queryParam("language", "fr").build())
                                                                .retrieve()
                                                                .body(RapexNotification.class);

            return rapexNotification != null ? rapexNotification.toAlert(translations) : null;
        } catch (HttpClientErrorException.NotFound e) {
            this.logger.warn("Notification {} not found, skipping", id);
            return null;
        }
    }
}
