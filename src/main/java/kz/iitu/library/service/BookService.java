package kz.iitu.library.service;

import kz.iitu.library.audit.Audited;
import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.notify.NotificationService;
import kz.iitu.library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The business layer that every lab meets:
 * Lab 2 — constructor injection of the repository, notifications and the cache;
 * Lab 4 — public methods are advised (log, time, trace), @Audited ones are also audited;
 * Lab 5 — CRUD and the citation; Lab 6 — BookNotFoundException for a missing id.
 */
@Service
public class BookService {

    private final BookRepository repository;
    private final NotificationService notifications;
    private final CitationCache citations;

    // Lab 4: a reference to our own proxy, so internal calls also pass through the aspects.
    // @Lazy injects a lazy-resolving proxy and avoids a circular-reference error at startup.
    @Autowired
    @Lazy
    private BookService self;

    public BookService(BookRepository repository, NotificationService notifications,
                       CitationCache citations) {
        this.repository = repository;
        this.notifications = notifications;
        this.citations = citations;
    }

    @Audited(action = "BOOK_LIST", logArguments = true)
    public List<Book> findAll(String author) {
        return repository.findAll().stream()
                .filter(b -> author == null || author.isBlank()
                        || b.author().toLowerCase().contains(author.toLowerCase()))
                .sorted(Comparator.comparing(Book::id))
                .toList();
    }

    public Book findById(long id) {
        return repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    @Audited(action = "BOOK_CREATE", logArguments = true)
    public Book create(Book book) {
        rejectDuplicate(book, null);    // Lab 7: 409
        Book saved = repository.save(book.withId(null).withCreatedAt(null));
        notifications.viaPrimary("New book in the catalogue: " + saved.title());
        return saved;
    }

    @Audited(action = "BOOK_UPDATE", logArguments = true)
    public Book update(long id, Book book) {
        Book existing = findById(id);   // throws BookNotFoundException
        rejectDuplicate(book, id);
        citations.evict(id);
        // PUT replaces the client's fields; the server-owned createdAt survives
        return repository.save(book.withId(id).withCreatedAt(existing.createdAt()));
    }

    /**
     * Lab 7: a second DELETE of the same id answers 404 — the state of the server does not
     * change (DELETE stays idempotent) and the client learns there was nothing to delete.
     */
    @Audited(action = "BOOK_DELETE", logArguments = true)
    public boolean delete(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid identifier: " + id);
        }
        citations.evict(id);
        return repository.deleteById(id);
    }

    /** Same title and same author (ignoring case) is the same book; ignoreId lets PUT keep its own. */
    private void rejectDuplicate(Book candidate, Long ignoreId) {
        boolean taken = repository.findAll().stream()
                .filter(b -> !b.id().equals(ignoreId))
                .anyMatch(b -> b.title().equalsIgnoreCase(candidate.title().trim())
                        && b.author().equalsIgnoreCase(candidate.author().trim()));
        if (taken) {
            throw new DuplicateTitleException(candidate.title(), candidate.author());
        }
    }

    /**
     * Lab 4, self-invocation experiment. viaProxy=false calls this.delete(...) — the call never
     * leaves the object, so no BOOK_DELETE audit records appear; viaProxy=true calls self.delete(...).
     */
    public Map<Long, Boolean> deleteBulk(List<Long> ids, boolean viaProxy) {
        Map<Long, Boolean> result = new LinkedHashMap<>();
        for (long id : ids) {
            result.put(id, viaProxy ? self.delete(id) : delete(id));
        }
        return result;
    }

    /** Lab 5, variant 6: "Author (year). Title." — cached according to Lab 3 app.cache. */
    public String citation(long id) {
        return citations.get(id, () -> {
            Book b = findById(id);
            return b.author() + " (" + b.year() + "). " + b.title() + ".";
        });
    }

    /** Deliberately slow (about 300 ms) so that TimingAspect prints a SLOW warning. */
    @Audited(action = "BOOK_STATS")
    public Map<Genre, Long> statistics() {
        sleep(300);
        Map<Genre, Long> byGenre = new EnumMap<>(Genre.class);
        for (Book b : repository.findAll()) {
            if (b.genre() != null) {
                byGenre.merge(b.genre(), 1L, Long::sum);
            }
        }
        return byGenre;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
