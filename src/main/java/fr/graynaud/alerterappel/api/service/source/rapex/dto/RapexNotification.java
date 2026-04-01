package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertCommercialization;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertCompanyRecall;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMeasureItem;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMeasures;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMedia;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadata;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadataSource;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertProduct;
import fr.graynaud.alerterappel.api.service.source.rapex.RapexService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record RapexNotification(@JsonProperty("id") Long id, @JsonProperty("notificationType") RapexNotificationType notificationType,
                                @JsonProperty("reference") String reference, @JsonProperty("corrigendum") String corrigendum,
                                @JsonProperty("country") RapexCountry country, @JsonProperty("creationDate") OffsetDateTime creationDate,
                                @JsonProperty("publicationDate") OffsetDateTime publicationDate,
                                @JsonProperty("modificationDate") OffsetDateTime modificationDate,
                                @JsonProperty("isImmediatePublication") Boolean isImmediatePublication,
                                @JsonProperty("singlePublication") Boolean singlePublication, @JsonProperty("forInfo") Boolean forInfo,
                                @JsonProperty("product") RapexProduct product, @JsonProperty("risk") RapexRisk risk,
                                @JsonProperty("measureTaken") RapexMeasureTaken measureTaken, @JsonProperty("traceability") RapexTraceability traceability,
                                @JsonProperty("reactingCountries") List<RapexReactingCountry> reactingCountries,
                                @JsonProperty("webReport") RapexWebReport webReport, @JsonProperty("versions") List<RapexNotificationVersion> versions) {

    private static final String PHOTO_URL_PREFIX = "https://ec.europa.eu/safety-gate-alerts/public/api/notification/image/";

    public Alert toAlert(Map<String, String> translations, String rapexUrl) {
        AlertMetadataSource source = new AlertMetadataSource(RapexService.SOURCE_NAME, this.id, rapexUrl, OffsetDateTime.now(), null);
        AlertMetadata metadata = new AlertMetadata(List.of(source), null);

        List<String> risks = this.risk != null && this.risk.riskType() != null ? this.risk.riskType()
                                                                                          .stream()
                                                                                          .map(r -> translate(r.key(), translations))
                                                                                          .toList() : null;

        String riskDescription = firstRiskVersion() != null ? firstRiskVersion().riskDescription() : null;

        AlertProduct alertProduct = mapProduct(translations);
        AlertCommercialization commercialization = mapCommercialization(translations);
        AlertMeasures measures = mapMeasures(translations);
        AlertMedia media = mapMedia(rapexUrl);

        return new Alert(metadata, this.reference != null ? this.reference.toUpperCase() : null, this.publicationDate, risks, riskDescription, null,
                         alertProduct, commercialization, measures, media, null);
    }

    private AlertProduct mapProduct(Map<String, String> translations) {
        if (this.product == null) {
            return null;
        }

        RapexProductVersion version = firstProductVersion();

        String brand = this.product.brands() != null && !this.product.brands().isEmpty() ? this.product.brands().getFirst().brand() : null;

        List<String> barcodes = this.product.barcodes() != null ? this.product.barcodes().stream().map(RapexBarcode::barcode).toList() : null;

        List<String> batchNumbers = this.product.batchNumbers() != null ? this.product.batchNumbers().stream().map(RapexBatchNumber::batchNumber).toList()
                                                                        : null;

        List<String> modelRefs = this.product.modelTypes() != null ? this.product.modelTypes().stream().map(RapexModelType::modelType).toList() : null;

        return new AlertProduct(this.product.nameSpecific(), version != null ? version.name() : null, version != null ? version.description() : null, brand,
                                null, this.product.productCategory() != null ? translate(this.product.productCategory().key(), translations) : null,
                                optionToBoolean(this.product.isCounterfeit()), barcodes != null && !barcodes.isEmpty() ? barcodes : null,
                                batchNumbers != null && !batchNumbers.isEmpty() ? batchNumbers : null,
                                modelRefs != null && !modelRefs.isEmpty() ? modelRefs : null, version != null ? version.packageDescription() : null, null);
    }

    private AlertCommercialization mapCommercialization(Map<String, String> translations) {
        String originCountry = this.traceability != null && this.traceability.countryOrigin() != null ? this.traceability.countryOrigin().name() : null;

        String alertCountry = this.country != null ? this.country.name() : null;

        List<String> reacting = this.reactingCountries != null ? this.reactingCountries.stream()
                                                                                       .filter(rc -> rc.country() != null)
                                                                                       .map(rc -> translate(rc.country().key(), translations))
                                                                                       .toList() : null;

        Boolean soldOnline = this.traceability != null ? optionToBoolean(this.traceability.isSoldOnline()) : null;

        return new AlertCommercialization(originCountry, alertCountry, reacting != null && !reacting.isEmpty() ? reacting : null, soldOnline, null, null, null);
    }

    private AlertMeasures mapMeasures(Map<String, String> translations) {
        if (this.measureTaken == null) {
            return null;
        }

        Boolean recallOnline = optionToBoolean(this.measureTaken.hasPublishedRecallOnline());

        List<AlertMeasureItem> measureItems = this.measureTaken.measures() != null ?
                                              this.measureTaken.measures().stream().map(m -> mapMeasureItem(m, translations)).toList() : null;

        List<AlertCompanyRecall> companyRecalls = this.measureTaken.companyRecalls() != null ?
                                                  this.measureTaken.companyRecalls().stream().map(r -> mapCompanyRecall(r, translations)).toList() : null;

        return new AlertMeasures(recallOnline, measureItems != null && !measureItems.isEmpty() ? measureItems : null,
                                 companyRecalls != null && !companyRecalls.isEmpty() ? companyRecalls : null, null, null, null);
    }

    private AlertMeasureItem mapMeasureItem(RapexMeasure measure, Map<String, String> translations) {
        RapexMeasureVersion version = measure.versions() != null && !measure.versions().isEmpty() ? measure.versions().getFirst() : null;

        return new AlertMeasureItem(measure.measureCategory() != null ? translate(measure.measureCategory().key(), translations) : null,
                                    version != null ? version.measureCategoryOther() : null,
                                    measure.measureType() != null ? translate(measure.measureType().key(), translations) : null,
                                    measure.entryIntoForceDate() != null ? measure.entryIntoForceDate().toLocalDate() : null);
    }

    private AlertCompanyRecall mapCompanyRecall(RapexCompanyRecall recall, Map<String, String> translations) {
        return new AlertCompanyRecall(recall.link(), recall.language() != null ? translate(recall.language().key(), translations) : null);
    }

    private AlertMedia mapMedia(String recallSheetUrl) {
        List<String> photos = this.product != null && this.product.photos() != null ?
                              this.product.photos().stream().map(p -> PHOTO_URL_PREFIX + p.id()).toList() : null;

        return new AlertMedia(photos != null && !photos.isEmpty() ? photos : null, recallSheetUrl);
    }

    private RapexProductVersion firstProductVersion() {
        return this.product != null && this.product.versions() != null && !this.product.versions().isEmpty() ? this.product.versions().getFirst() : null;
    }

    private RapexRiskVersion firstRiskVersion() {
        return this.risk != null && this.risk.versions() != null && !this.risk.versions().isEmpty() ? this.risk.versions().getFirst() : null;
    }

    private static String translate(String key, Map<String, String> translations) {
        return translations.getOrDefault(key, key);
    }

    private static Boolean optionToBoolean(RapexKeyName option) {
        if (option == null || option.key() == null) {
            return null;
        }

        return switch (option.key()) {
            case "option.yes" -> Boolean.TRUE;
            case "option.no" -> Boolean.FALSE;
            default -> null;
        };
    }
}
