package fr.graynaud.alerterappel.api.controller.publics;

import fr.graynaud.alerterappel.api.config.properties.Explore21Properties;
import fr.graynaud.alerterappel.api.controller.dto.PageResponse;
import fr.graynaud.alerterappel.api.service.alert.AlertService;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.alert.dto.SearchSuggestion;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/public/alerts")
public class AlertController {

    private static final ResponseEntity.BodyBuilder SUGGEST_WITH_CACHE = ResponseEntity.ok()
                                                                                       .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic());

    private static final ResponseEntity<Alert> NOT_FOUND = ResponseEntity.notFound().build();

    private static final ResponseEntity.BodyBuilder DETAILS_WITH_CACHE = ResponseEntity.ok()
                                                                                       .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic());

    private final AlertService alertService;

    private final List<CronExpression> sourceCrons;

    public AlertController(AlertService alertService, List<Explore21Properties> sourceProperties) {
        this.alertService = alertService;
        this.sourceCrons = sourceProperties.stream()
                                           .map(p -> CronExpression.parse(p.getCron()))
                                           .toList();
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<SearchSuggestion>> suggest(@RequestParam String q) {
        return SUGGEST_WITH_CACHE.body(this.alertService.suggest(q));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<Alert>> search(@RequestParam String q, @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(this.alertService.search(q, page, 15));
    }

    @GetMapping("/latest")
    public ResponseEntity<PageResponse<Alert>> getLatest(@RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok().cacheControl(cacheUntilNextCron()).body(this.alertService.getLatest(page, 15));
    }

    @GetMapping("/details/{*alertNumber}")
    public ResponseEntity<Alert> getByAlertNumber(@PathVariable String alertNumber) {
        return this.alertService.findByAlertNumber(alertNumber.substring(1)).map(DETAILS_WITH_CACHE::body).orElse(NOT_FOUND);
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<Alert> getByBarcode(@PathVariable String barcode) {
        return this.alertService.findByBarcode(barcode).map(DETAILS_WITH_CACHE::body).orElse(NOT_FOUND);
    }

    private CacheControl cacheUntilNextCron() {
        LocalDateTime now = LocalDateTime.now();
        Duration shortest = Duration.ofMinutes(1);

        for (CronExpression cron : this.sourceCrons) {
            LocalDateTime next = cron.next(now);
            if (next != null) {
                Duration until = Duration.between(now, next);
                if (until.compareTo(shortest) < 0) {
                    shortest = until;
                } else if (shortest.equals(Duration.ofMinutes(1))) {
                    shortest = until;
                }
            }
        }

        return CacheControl.maxAge(shortest).cachePublic();
    }
}
