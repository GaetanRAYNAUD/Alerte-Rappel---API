package fr.graynaud.alerterappel.api.config.properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.RestClient;

public abstract class Explore21Properties extends SourceProperties {

    private String baseUrl;

    private String dataset;

    public RestClient.Builder restClientBuilder(RestClient.Builder builder) {
        return builder.clone().baseUrl(StringUtils.stripEnd(this.baseUrl, "/") + "/api/explore/v2.1/catalog/datasets/" + this.dataset);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }
}
