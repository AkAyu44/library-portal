package kz.iitu.library.persistence;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lab 8: hands Hibernate an instance of the statement inspector instead of a class name —
 * with a name, Hibernate loads the class itself and fails under the devtools restart class loader.
 */
@Configuration
public class SqlRecorderConfig {

    @Bean
    public HibernatePropertiesCustomizer sqlRecorder() {
        return properties -> properties.put(AvailableSettings.STATEMENT_INSPECTOR, new SqlRecorder());
    }
}
