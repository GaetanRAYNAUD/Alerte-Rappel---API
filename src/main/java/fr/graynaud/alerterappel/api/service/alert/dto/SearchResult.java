package fr.graynaud.alerterappel.api.service.alert.dto;

import java.util.List;

public record SearchResult(List<String> alertNumbers, long totalHits) {}
