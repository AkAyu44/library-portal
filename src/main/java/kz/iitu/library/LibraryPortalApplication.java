package kz.iitu.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point. Component scanning starts from this package (Lab 1),
 * {@code @ConfigurationPropertiesScan} registers the typed {@code AppProperties} record (Lab 3).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LibraryPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryPortalApplication.class, args);
    }
}
