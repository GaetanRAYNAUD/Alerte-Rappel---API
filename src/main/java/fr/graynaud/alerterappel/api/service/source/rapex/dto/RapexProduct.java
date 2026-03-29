package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RapexProduct(
        @JsonProperty("id") Long id,
        @JsonProperty("productCategory") RapexKeyName productCategory,
        @JsonProperty("professionalProduct") Boolean professionalProduct,
        @JsonProperty("isCounterfeit") RapexKeyName isCounterfeit,
        @JsonProperty("nameSpecific") String nameSpecific,
        @JsonProperty("nameSpecificKnown") Boolean nameSpecificKnown,
        @JsonProperty("brandKnown") Boolean brandKnown,
        @JsonProperty("brands") List<RapexBrand> brands,
        @JsonProperty("name") String name,
        @JsonProperty("typeModelKnown") Boolean typeModelKnown,
        @JsonProperty("batchNumberKnown") Boolean batchNumberKnown,
        @JsonProperty("barcodeKnown") Boolean barcodeKnown,
        @JsonProperty("packageDescriptionKnown") Boolean packageDescriptionKnown,
        @JsonProperty("versions") List<RapexProductVersion> versions,
        @JsonProperty("barcodes") List<RapexBarcode> barcodes,
        @JsonProperty("batchNumbers") List<RapexBatchNumber> batchNumbers,
        @JsonProperty("modelTypes") List<RapexModelType> modelTypes,
        @JsonProperty("description") String description,
        @JsonProperty("packageDescription") String packageDescription,
        @JsonProperty("productCategoryOther") String productCategoryOther,
        @JsonProperty("photos") List<RapexPhoto> photos
) {}
