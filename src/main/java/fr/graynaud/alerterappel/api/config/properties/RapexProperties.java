package fr.graynaud.alerterappel.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "source.rapex")
public class RapexProperties extends Explore21Properties {

    private String getBaseUrl;

    private String getPath;

    private String translationPath;

    public String getGetBaseUrl() {
        return getBaseUrl;
    }

    public void setGetBaseUrl(String getBaseUrl) {
        this.getBaseUrl = getBaseUrl;
    }

    public String getGetPath() {
        return getPath;
    }

    public void setGetPath(String getPath) {
        this.getPath = getPath;
    }

    public String getTranslationPath() {
        return translationPath;
    }

    public void setTranslationPath(String translationPath) {
        this.translationPath = translationPath;
    }
}
