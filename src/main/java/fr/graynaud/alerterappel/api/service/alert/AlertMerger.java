package fr.graynaud.alerterappel.api.service.alert;

import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import fr.graynaud.alerterappel.api.service.alert.dto.AlertCommercialization;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMeasureItem;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMeasures;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMedia;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadata;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertMetadataSource;
import fr.graynaud.alerterappel.api.service.alert.dto.AlertProduct;
import fr.graynaud.alerterappel.api.service.source.rapex.RapexService;

@Component
public class AlertMerger {

    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}]");

    public Alert mergeAlerts(Alert existing, Alert incoming) {
        if (hasSingleSourceWithSameOrigin(existing, incoming)) {
            Integer existingVersion = existing.metadata().sources().getFirst().versionNumber();
            Integer incomingVersion = incoming.metadata().sources().getFirst().versionNumber();
            if (existingVersion != null && incomingVersion != null) {
                return incomingVersion >= existingVersion ? incoming : existing;
            }
            return incoming;
        }

        Alert rapex = isRapex(existing) ? existing : isRapex(incoming) ? incoming : null;
        Alert other = rapex == existing ? incoming : existing;

        if (rapex == null) {
            return incoming;
        }

        return new Alert(
                mergeMetadata(existing.metadata(), incoming.metadata()),
                rapex.alertNumber(),
                ObjectUtils.firstNonNull(rapex.publicationDate(), other.publicationDate()),
                mergeLists(rapex.risks(), other.risks()),
                ObjectUtils.firstNonNull(rapex.riskDescription(), other.riskDescription()),
                ObjectUtils.firstNonNull(rapex.supplementaryRiskDescription(), other.supplementaryRiskDescription()),
                mergeProduct(rapex.product(), other.product()),
                mergeCommercialization(rapex.commercialization(), other.commercialization()),
                mergeMeasures(rapex.measures(), other.measures()),
                mergeMedia(rapex.media(), other.media()),
                ObjectUtils.firstNonNull(rapex.additionalInformation(), other.additionalInformation())
        );
    }

    private boolean hasSingleSourceWithSameOrigin(Alert existing, Alert incoming) {
        if (existing.metadata() == null || existing.metadata().sources() == null || existing.metadata().sources().size() != 1) {
            return false;
        }
        if (incoming.metadata() == null || incoming.metadata().sources() == null || incoming.metadata().sources().isEmpty()) {
            return false;
        }
        return existing.metadata().sources().getFirst().origin().equals(incoming.metadata().sources().getFirst().origin());
    }

    private boolean isRapex(Alert alert) {
        return alert.metadata() != null
               && alert.metadata().sources() != null
               && alert.metadata().sources().stream().anyMatch(s -> RapexService.SOURCE_NAME.equals(s.origin()));
    }

    private AlertMetadata mergeMetadata(AlertMetadata a, AlertMetadata b) {
        List<AlertMetadataSource> sources = new ArrayList<>();
        if (a != null && a.sources() != null) sources.addAll(a.sources());
        if (b != null && b.sources() != null) {
            for (AlertMetadataSource newSource : b.sources()) {
                int existingIndex = -1;
                for (int i = 0; i < sources.size(); i++) {
                    if (sources.get(i).origin().equals(newSource.origin())) {
                        existingIndex = i;
                        break;
                    }
                }
                if (existingIndex >= 0) {
                    AlertMetadataSource existingSource = sources.get(existingIndex);
                    if (existingSource.versionNumber() != null && newSource.versionNumber() != null) {
                        if (newSource.versionNumber() >= existingSource.versionNumber()) sources.set(existingIndex, newSource);
                    } else {
                        sources.set(existingIndex, newSource);
                    }
                } else {
                    sources.add(newSource);
                }
            }
        }
        String guid = a != null ? a.rappelconsoGuid() : null;
        if (guid == null && b != null) guid = b.rappelconsoGuid();
        return new AlertMetadata(sources, guid);
    }

    private AlertProduct mergeProduct(AlertProduct rapex, AlertProduct other) {
        if (rapex == null) return other;
        if (other == null) return rapex;
        return new AlertProduct(
                ObjectUtils.firstNonNull(rapex.specificName(), other.specificName()),
                ObjectUtils.firstNonNull(rapex.type(), other.type()),
                ObjectUtils.firstNonNull(rapex.description(), other.description()),
                ObjectUtils.firstNonNull(rapex.brand(), other.brand()),
                ObjectUtils.firstNonNull(rapex.family(), other.family()),
                ObjectUtils.firstNonNull(rapex.category(), other.category()),
                ObjectUtils.firstNonNull(rapex.counterfeit(), other.counterfeit()),
                mergeLists(rapex.barcodes(), other.barcodes()),
                mergeLists(rapex.batchNumbers(), other.batchNumbers()),
                mergeLists(rapex.modelReferences(), other.modelReferences()),
                ObjectUtils.firstNonNull(rapex.packagingDescription(), other.packagingDescription()),
                ObjectUtils.firstNonNull(rapex.productionDates(), other.productionDates())
        );
    }

    private AlertCommercialization mergeCommercialization(AlertCommercialization rapex, AlertCommercialization other) {
        if (rapex == null) return other;
        if (other == null) return rapex;
        return new AlertCommercialization(
                ObjectUtils.firstNonNull(rapex.originCountryName(), other.originCountryName()),
                ObjectUtils.firstNonNull(rapex.alertCountryName(), other.alertCountryName()),
                mergeLists(rapex.reactingCountries(), other.reactingCountries()),
                ObjectUtils.firstNonNull(rapex.soldOnline(), other.soldOnline()),
                ObjectUtils.firstNonNull(rapex.marketingStartDate(), other.marketingStartDate()),
                ObjectUtils.firstNonNull(rapex.marketingEndDate(), other.marketingEndDate()),
                ObjectUtils.firstNonNull(rapex.distributors(), other.distributors())
        );
    }

    private AlertMeasures mergeMeasures(AlertMeasures rapex, AlertMeasures other) {
        if (rapex == null) return other;
        if (other == null) return rapex;
        return new AlertMeasures(
                ObjectUtils.firstNonNull(rapex.recallPublishedOnline(), other.recallPublishedOnline()),
                mergeMeasureItems(rapex.measuresList(), other.measuresList()),
                ObjectUtils.firstNonNull(rapex.companyRecalls(), other.companyRecalls()),
                ObjectUtils.firstNonNull(rapex.consumerActions(), other.consumerActions()),
                ObjectUtils.firstNonNull(rapex.compensationTerms(), other.compensationTerms()),
                ObjectUtils.firstNonNull(rapex.procedureEndDate(), other.procedureEndDate())
        );
    }

    private AlertMedia mergeMedia(AlertMedia rapex, AlertMedia other) {
        if (rapex == null) return other;
        if (other == null) return rapex;
        return new AlertMedia(mergeLists(rapex.photos(), other.photos()), ObjectUtils.firstNonNull(other.recallSheetUrl(), rapex.recallSheetUrl()));
    }

    private static List<AlertMeasureItem> mergeMeasureItems(List<AlertMeasureItem> primary, List<AlertMeasureItem> secondary) {
        LinkedHashSet<String> seenCategories = new LinkedHashSet<>();
        List<AlertMeasureItem> merged = new ArrayList<>();
        if (primary != null) {
            for (AlertMeasureItem item : primary) {
                if (item.category() == null || seenCategories.add(normalize(item.category()))) merged.add(item);
            }
        }
        if (secondary != null) {
            for (AlertMeasureItem item : secondary) {
                if (item.category() == null || seenCategories.add(normalize(item.category()))) merged.add(item);
            }
        }
        return merged.isEmpty() ? null : merged;
    }

    private static String normalize(String s) { return NON_ALNUM.matcher(s.toLowerCase()).replaceAll(""); }

    private static List<String> mergeLists(Collection<String> primary, Collection<String> secondary) {
        LinkedHashSet<String> normalizedKeys = new LinkedHashSet<>();
        List<String> merged = new ArrayList<>();
        if (primary != null) {
            for (String s : primary) { if (normalizedKeys.add(normalize(s))) merged.add(s); }
        }
        if (secondary != null) {
            for (String s : secondary) { if (normalizedKeys.add(normalize(s))) merged.add(s); }
        }
        return merged.isEmpty() ? null : merged;
    }
}
