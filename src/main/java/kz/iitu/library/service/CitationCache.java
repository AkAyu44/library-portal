package kz.iitu.library.service;

import kz.iitu.library.config.AppProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Lab 3, variant 7 in real use: the cache of Lab 5 citations is sized and expired by app.cache.ttl
 * and app.cache.max-entries (30s / 50 under the dev profile, 1m / 100 otherwise).
 * Lives in the service package, so it is also traced by the Lab 4 aspects (nested call depth).
 */
@Component
public class CitationCache {

    private final Duration ttl;
    private final int maxEntries;
    private final Map<Long, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public CitationCache(AppProperties props) {
        this.ttl = props.cache().ttl();
        this.maxEntries = props.cache().maxEntries();
    }

    public String get(long id, Supplier<String> loader) {
        Entry entry = entries.get(id);
        if (entry != null && entry.createdAt().plus(ttl).isAfter(Instant.now())) {
            hits.incrementAndGet();
            return entry.text();
        }
        misses.incrementAndGet();
        if (entries.size() >= maxEntries) {
            entries.clear();   // the simplest eviction policy is enough for a demo
        }
        String text = loader.get();
        entries.put(id, new Entry(text, Instant.now()));
        return text;
    }

    public void evict(long id) {
        entries.remove(id);
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("ttl", ttl.toString());
        stats.put("maxEntries", maxEntries);
        stats.put("size", entries.size());
        stats.put("hits", hits.get());
        stats.put("misses", misses.get());
        return stats;
    }

    private record Entry(String text, Instant createdAt) {}
}
