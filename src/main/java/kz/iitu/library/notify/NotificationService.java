package kz.iitu.library.notify;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lab 2: every way to resolve several candidates of one type —
 * @Primary (primary), @Qualifier (console, html), List (ordered by @Order), Map (keyed by bean name).
 * Used by BookService to announce new books.
 */
@Service
public class NotificationService {
    private final Notifier primary;
    private final Notifier console;
    private final Notifier html;
    private final List<Notifier> all;
    private final Map<String, Notifier> byName;

    public NotificationService(Notifier primary,
                               @Qualifier("console") Notifier console,
                               @Qualifier("html") Notifier html,
                               List<Notifier> all,
                               Map<String, Notifier> byName) {
        this.primary = primary;
        this.console = console;
        this.html = html;
        this.all = all;
        this.byName = byName;
    }

    public String viaPrimary(String m) { return primary.send(m); }
    public String viaConsole(String m) { return console.send(m); }
    public String viaHtml(String m) { return html.send(m); }
    public List<String> viaAll(String m) { return all.stream().map(n -> n.send(m)).toList(); }
    public List<String> order() { return all.stream().map(Notifier::channel).toList(); }
    public Set<String> names() { return byName.keySet(); }
}
