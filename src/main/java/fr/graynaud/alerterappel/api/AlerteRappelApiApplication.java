package fr.graynaud.alerterappel.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("fr.graynaud.alerterappel.api.config.properties")
public class AlerteRappelApiApplication {

    static void main(String[] args) {
        SpringApplication.run(AlerteRappelApiApplication.class, args);
    }

}
