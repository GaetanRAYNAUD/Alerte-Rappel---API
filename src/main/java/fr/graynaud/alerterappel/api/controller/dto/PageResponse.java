package fr.graynaud.alerterappel.api.controller.dto;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, int totalPages, long totalElements) {

    public static <T> PageResponse<T> from(List<T> items, int page, int size, long totalElements) {
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(items, page, size, totalPages, totalElements);
    }

    public static <T> PageResponse<T> from(List<T> items, int page, int size) {
        int totalElements = items.size();
        int totalPages = (totalElements + size - 1) / size;
        int from = page * size;
        
        if (from >= totalElements) {
            return new PageResponse<>(List.of(), page, size, totalPages, totalElements);
        }
        
        List<T> content = items.subList(from, Math.min(from + size, totalElements));
        return new PageResponse<>(content, page, size, totalPages, totalElements);
    }
}
