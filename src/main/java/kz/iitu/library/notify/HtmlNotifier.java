package kz.iitu.library.notify;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** Lab 2, variant 8: wraps the message in <p> and escapes angle brackets. */
@Component("html")
@Order(3)
public class HtmlNotifier implements Notifier {
    private static final Logger log = LoggerFactory.getLogger(HtmlNotifier.class);

    @PostConstruct
    void init() { log.info("HTML notifier initialized"); }

    @Override
    public String send(String message) { return "<p>" + HtmlUtils.htmlEscape(message) + "</p>"; }

    @Override
    public String channel() { return "html"; }
}
