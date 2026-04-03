package fr.graynaud.alerterappel.api.controller.publics;

import fr.graynaud.alerterappel.api.controller.dto.PageResponse;
import fr.graynaud.alerterappel.api.service.alert.AlertService;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/search")
    public PageResponse<Alert> search(@RequestParam String q, @RequestParam(defaultValue = "0") int page) {
        return this.alertService.search(q, page, 15);
    }

    @GetMapping("/latest")
    public PageResponse<Alert> getLatest(@RequestParam(defaultValue = "0") int page) {
        return this.alertService.getLatest(page, 15);
    }

    @GetMapping("/{*alertNumber}")
    public ResponseEntity<Alert> getByAlertNumber(@PathVariable String alertNumber) {
        return ResponseEntity.of(this.alertService.findByAlertNumber(alertNumber.substring(1)));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<Alert> getByBarcode(@PathVariable String barcode) {
        return ResponseEntity.of(this.alertService.findByBarcode(barcode));
    }
}
