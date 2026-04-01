package fr.graynaud.alerterappel.api.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of();

    private List<String> allowedOriginPatterns = List.of();

    private List<String> allowedMethods = List.of("GET", "HEAD", "POST");

    private List<String> allowedHeaders = List.of();

    private List<String> exposedHeaders = List.of();

    private Boolean allowCredentials;

    private Long maxAge;

    public List<String> getAllowedOrigins() {
        return this.allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedOriginPatterns() {
        return this.allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    public List<String> getAllowedMethods() {
        return this.allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return this.allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public List<String> getExposedHeaders() {
        return this.exposedHeaders;
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public Boolean getAllowCredentials() {
        return this.allowCredentials;
    }

    public void setAllowCredentials(Boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public Long getMaxAge() {
        return this.maxAge;
    }

    public void setMaxAge(Long maxAge) {
        this.maxAge = maxAge;
    }
}
