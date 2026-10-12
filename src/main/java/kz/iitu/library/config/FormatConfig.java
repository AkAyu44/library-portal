package kz.iitu.library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Lab 2: a @Bean method for a JDK class we cannot annotate. Bean name = method name. */
@Configuration
public class FormatConfig {

    @Bean
    public DateTimeFormatter reportDateFormatter(
            @Value("${app.date-pattern:dd.MM.yyyy HH:mm:ss}") String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
    }
}
