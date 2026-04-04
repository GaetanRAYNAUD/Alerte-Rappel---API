package fr.graynaud.alerterappel.api.controller.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    void fromWithTotalElements() {
        List<String> items = List.of("a", "b");
        PageResponse<String> result = PageResponse.from(items, 0, 10, 42);

        assertEquals(List.of("a", "b"), result.content());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(5, result.totalPages());
        assertEquals(42, result.totalElements());
    }

    @Test
    void fromWithTotalElementsRoundsUp() {
        PageResponse<String> result = PageResponse.from(List.of("a"), 0, 10, 11);
        assertEquals(2, result.totalPages());
    }

    @Test
    void fromWithTotalElementsExactMultiple() {
        PageResponse<String> result = PageResponse.from(List.of(), 1, 10, 20);
        assertEquals(2, result.totalPages());
    }

    @Test
    void fromSlicesFirstPage() {
        List<String> items = List.of("a", "b", "c", "d", "e");
        PageResponse<String> result = PageResponse.from(items, 0, 2);

        assertEquals(List.of("a", "b"), result.content());
        assertEquals(0, result.page());
        assertEquals(2, result.size());
        assertEquals(3, result.totalPages());
        assertEquals(5, result.totalElements());
    }

    @Test
    void fromSlicesMiddlePage() {
        List<String> items = List.of("a", "b", "c", "d", "e");
        PageResponse<String> result = PageResponse.from(items, 1, 2);

        assertEquals(List.of("c", "d"), result.content());
    }

    @Test
    void fromSlicesLastPagePartial() {
        List<String> items = List.of("a", "b", "c", "d", "e");
        PageResponse<String> result = PageResponse.from(items, 2, 2);

        assertEquals(List.of("e"), result.content());
    }

    @Test
    void fromPageBeyondTotal() {
        List<String> items = List.of("a", "b");
        PageResponse<String> result = PageResponse.from(items, 5, 2);

        assertTrue(result.content().isEmpty());
        assertEquals(5, result.page());
        assertEquals(1, result.totalPages());
        assertEquals(2, result.totalElements());
    }

    @Test
    void fromEmptyList() {
        PageResponse<String> result = PageResponse.from(List.of(), 0, 10);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalPages());
        assertEquals(0, result.totalElements());
    }
}
