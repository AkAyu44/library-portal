package kz.iitu.library.notify;

import kz.iitu.library.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Lab 2: @Primary — chosen for a single-valued Notifier dependency.
 * Lab 3: reads app.mail.* (enabled / from / retry-count differ between dev and prod).
 */
@Component("email")
@Primary
@Order(2)
public class EmailNotifier implements Notifier {
    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    private final AppProperties.Mail mail;

    public EmailNotifier(AppProperties props) {
        this.mail = props.mail();
    }

    @Override
    public String send(String message) {
        if (!mail.enabled()) {
            log.info("EMAIL (disabled) >> {}", message);
            return "email disabled: " + message;
        }
        log.info("EMAIL from {} (retries={}) >> {}", mail.from(), mail.retryCount(), message);
        return "email: " + message;
    }

    @Override
    public String channel() { return "email"; }
}
