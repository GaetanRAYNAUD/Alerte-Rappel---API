package fr.graynaud.alerterappel.api.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestConfig.class);

    private static final String HEADER_RATELIMIT_LIMIT = "x-ratelimit-limit";
    private static final String HEADER_RATELIMIT_REMAINING = "x-ratelimit-remaining";
    private static final String HEADER_RATELIMIT_RESET = "x-ratelimit-reset";

    private static final DateTimeFormatter RESET_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxxx");
    private static final DateTimeFormatter RESET_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    @Bean
    public RestClient.Builder restClientBuilder() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
        );
        factory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                         .requestFactory(factory)
                         .requestInterceptor((request, body, execution) -> {
                             ClientHttpResponse response = execution.execute(request, body);
                             logRateLimitHeaders(request.getURI().getHost(), response.getHeaders());
                             return response;
                         });
    }

    private void logRateLimitHeaders(String host, HttpHeaders headers) {
        String limitStr = headers.getFirst(HEADER_RATELIMIT_LIMIT);
        String remainingStr = headers.getFirst(HEADER_RATELIMIT_REMAINING);
        String resetStr = headers.getFirst(HEADER_RATELIMIT_RESET);

        if (limitStr == null && remainingStr == null && resetStr == null) {
            return;
        }

        LOGGER.info("[{}] Rate limit: {} / {} remaining — resets {}",
                    host,
                    formatNumber(remainingStr),
                    formatNumber(limitStr),
                    formatReset(resetStr));
    }

    private String formatNumber(String value) {
        if (value == null) {
            return "?";
        }
        try {
            return String.format(Locale.ENGLISH, "%,d", Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private String formatReset(String value) {
        if (value == null) {
            return "?";
        }
        try {
            OffsetDateTime reset = OffsetDateTime.parse(value.trim(), RESET_PARSER);
            Duration until = Duration.between(OffsetDateTime.now(ZoneOffset.UTC), reset);
            return "in %dh %02dm (%s)".formatted(until.toHours(), until.toMinutesPart(),
                                                 reset.atZoneSameInstant(ZoneId.of("UTC")).format(RESET_DISPLAY));
        } catch (Exception e) {
            return value;
        }
    }
}
