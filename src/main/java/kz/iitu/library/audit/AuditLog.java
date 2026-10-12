package kz.iitu.library.audit;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Keeps the last audit records in memory so they can be shown at /api/system/audit. */
@Component
public class AuditLog {
    private static final int CAPACITY = 50;

    private final Deque<Entry> entries = new ConcurrentLinkedDeque<>();

    public void add(String action, String outcome, String details) {
        entries.addFirst(new Entry(LocalDateTime.now(), action, outcome, details));
        while (entries.size() > CAPACITY) {
            entries.pollLast();
        }
    }

    public List<Entry> latest() {
        return new ArrayList<>(entries);
    }

    public long count(String action) {
        return entries.stream().filter(e -> e.action().equals(action)).count();
    }

    public void clear() {
        entries.clear();
    }

    public record Entry(LocalDateTime at, String action, String outcome, String details) {}
}
