package fr.graynaud.alerterappel.api.service.source.rapex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RapexBatchNumber(@JsonProperty("batchNumber") String batchNumber) {}
