package fr.graynaud.alerterappel.api.service.source.rappelconso;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.config.properties.RappelConsoProperties;
import fr.graynaud.alerterappel.api.service.alert.AlertService;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.source.explore21.Explore21Service;
import fr.graynaud.alerterappel.api.service.source.rappelconso.dto.RappelConsoData;
import fr.graynaud.alerterappel.api.service.source.rappelconso.dto.RappelConsoRappel;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class RappelConsoService extends Explore21Service<RappelConsoData> {

    public static final String SOURCE_NAME = "RappelConso";

    private final AlertService alertService;

    public RappelConsoService(RestClient.Builder restClientBuilder, RappelConsoProperties properties, DataProperties dataProperties,
                              JsonMapper jsonMapper, TaskScheduler taskScheduler, AlertService alertService, Environment environment) throws IOException {
        super(restClientBuilder, properties, dataProperties, jsonMapper, taskScheduler, SOURCE_NAME, RappelConsoData.class, "date_publication", environment);
        this.alertService = alertService;
    }

    @EventListener
    public void checkNewData(ApplicationReadyEvent event) {
        super.checkNewData();
    }

    @Override
    protected void handleNewData(OffsetDateTime since, RappelConsoData data) {
        String where = since == null ? this.dateField + " is not null" : this.dateField + " > \"" + since + "\"";

        List<RappelConsoRappel> rappels = this.updateClient.get()
                                                           .uri(b -> b.path("/exports/json")
                                                                      .queryParam("where", where)
                                                                      .queryParam("limit", -1)
                                                                      .queryParam("order_by", this.dateField + " ASC")
                                                                      .queryParam("lang", "fr")
                                                                      .queryParam("timezone", "UTC")
                                                                      .queryParam("use_labels", true)
                                                                      .build())
                                                           .exchange((_, res) ->
                                                                             this.jsonMapper.readValue(res.getBody(),
                                                                                                       this.jsonMapper.getTypeFactory()
                                                                                                                      .constructCollectionType(List.class,
                                                                                                                                               RappelConsoRappel.class)));

        List<Alert> alerts = rappels.stream().map(RappelConsoRappel::toAlert).toList();

        this.logger.info("New data for {}: {} alert(s) since {}", this.sourceName, alerts.size(), since);
        this.alertService.addAlerts(alerts);

        data.setLastPublishData(rappels.stream().map(RappelConsoRappel::datePublication).max(OffsetDateTime::compareTo).orElse(data.getLastPublishData()));
        persist(data);
    }
}
