package fr.graynaud.alerterappel.api.service.alert;

import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.alert.dto.SearchSuggestion;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SuggestionIndex {

    private static final int MAX_SUGGESTIONS = 8;
    private static final int MIN_NGRAM = 1;
    private static final int MAX_NGRAM = 5;
    private static final int MIN_WORD_LENGTH = 2;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHA = Pattern.compile("[^\\p{L}\\p{N}]+");

    private volatile List<SuggestionEntry> entries = List.of();

    public synchronized void rebuild(Map<String, Alert> alerts) {
        Map<String, MutableEntry> entryMap = new HashMap<>();

        for (Alert alert : alerts.values()) {
            if (alert.product() == null) {
                continue;
            }

            extractNgrams(entryMap, alert.product().specificName());
            extractNgrams(entryMap, alert.product().brand());
            extractNgrams(entryMap, alert.product().category());
            extractNgrams(entryMap, alert.product().family());
        }

        List<SuggestionEntry> sorted = new ArrayList<>(entryMap.size());
        for (MutableEntry entry : entryMap.values()) {
            sorted.add(new SuggestionEntry(entry.normalized, entry.count));
        }
        sorted.sort(Comparator.comparingInt((SuggestionEntry e) -> e.count).reversed());
        this.entries = List.copyOf(sorted);
    }

    public List<SearchSuggestion> suggest(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String normalizedQuery = normalize(query.trim());

        List<SuggestionEntry> current = this.entries;
        List<SearchSuggestion> results = new ArrayList<>();

        for (SuggestionEntry entry : current) {
            if (startsWith(entry.normalized, normalizedQuery)) {
                results.add(new SearchSuggestion(entry.normalized, entry.count));
                if (results.size() >= MAX_SUGGESTIONS) {
                    break;
                }
            }
        }

        return results;
    }

    private static void extractNgrams(Map<String, MutableEntry> entryMap, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        String[] words = tokenize(text);
        if (words.length == 0) {
            return;
        }

        for (int n = MIN_NGRAM; n <= Math.min(MAX_NGRAM, words.length); n++) {
            for (int i = 0; i <= words.length - n; i++) {
                String ngram = joinWords(words, i, i + n);
                String normalized = normalize(ngram);

                entryMap.compute(normalized, (k, existing) -> {
                    if (existing == null) {
                        return new MutableEntry(normalized, 1);
                    }
                    existing.count++;
                    return existing;
                });
            }
        }
    }

    private static String[] tokenize(String text) {
        String[] raw = NON_ALPHA.split(text.trim());
        List<String> words = new ArrayList<>();
        for (String word : raw) {
            if (word.length() >= MIN_WORD_LENGTH) {
                words.add(word);
            }
        }
        return words.toArray(String[]::new);
    }

    private static String joinWords(String[] words, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(words[i]);
        }
        return sb.toString();
    }

    private static boolean startsWith(String text, String prefix) {
        return text.startsWith(prefix);
    }

    private static String normalize(String input) {
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("").toLowerCase();
    }

    private record SuggestionEntry(String normalized, int count) {}

    private static class MutableEntry {
        final String normalized;
        int count;

        MutableEntry(String normalized, int count) {
            this.normalized = normalized;
            this.count = count;
        }
    }
}
