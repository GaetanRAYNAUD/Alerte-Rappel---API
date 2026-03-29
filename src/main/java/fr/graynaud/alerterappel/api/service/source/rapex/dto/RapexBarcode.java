package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexBarcode(@JsonProperty("barcode") String barcode) {}
