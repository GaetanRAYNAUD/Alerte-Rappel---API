package fr.graynaud.alerterappel.api.controller.dto;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, int totalPages, long totalElements) {}
