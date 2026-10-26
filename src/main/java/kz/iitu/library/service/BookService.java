package kz.iitu.library.service;

import kz.iitu.library.audit.Audited;
import kz.iitu.library.domain.Author;
import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.domain.Tag;
import kz.iitu.library.notify.NotificationService;
import kz.iitu.library.repository.AuthorRepository;
import kz.iitu.library.repository.BookRepository;
import kz.iitu.library.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The business layer that every lab meets:
 * Lab 2 — constructor injection of the repositories, notifications and the cache;
 * Lab 4 — public methods are advised (log, time, trace), @Audited ones are also audited;
 * Lab 5 — CRUD and the citation; Lab 6 — BookNotFoundException for a missing id;
 * Lab 8 — the transaction boundary: every public method runs in a transaction
 * (read-only unless it writes), so loaded entities are managed and their changes are flushed at commit.
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("year"), Sort.Order.desc("id"));

    /**
     * Lab 8, variant 7: the first window starts "after" a book newer than any real one.
     * Every call then has the same query shape (with the keyset predicate) — see BookRepository.findBy.
     */
    public static final ScrollPosition FIRST_WINDOW =
            ScrollPosition.forward(Map.of("year", Integer.MAX_VALUE, "id", Long.MAX_VALUE));

    private final BookRepository repository;
    private final AuthorRepository authors;
    private final TagRepository tags;
    private final NotificationService notifications;
    private final CitationCache citations;

    // Lab 4: a reference to our own proxy, so internal calls also pass through the aspects.
    // @Lazy injects a lazy-resolving proxy and avoids a circular-reference error at startup.
    @Autowired
    @Lazy
    private BookService self;

    public BookService(BookRepository repository, AuthorRepository authors, TagRepository tags,
                       NotificationService notifications, CitationCache citations) {
        this.repository = repository;
        this.authors = authors;
        this.tags = tags;
        this.notifications = notifications;
        this.citations = citations;
    }

    /**
     * Lab 8, task 4: one page of the catalogue, filtered by genre and/or part of the author's name.
     * The author comes with the page (entity graph); the tags of the whole page are loaded here,
     * inside the transaction, by one batched select — open-in-view is off, so later would be too late.
     */
    @Audited(action = "BOOK_LIST", logArguments = true)
    public Page<Book> find(Genre genre, String author, Pageable pageable) {
        boolean byAuthor = author != null && !author.isBlank();
        Page<Book> page;
        if (genre != null && byAuthor) {
            page = repository.findByGenreAndAuthorNameContainingIgnoreCase(genre, author.trim(), pageable);
        } else if (genre != null) {
            page = repository.findByGenre(genre, pageable);
        } else if (byAuthor) {
            page = repository.findByAuthorNameContainingIgnoreCase(author.trim(), pageable);
        } else {
            page = repository.findAll(pageable);
        }
        page.forEach(book -> book.getTags().size());     // initialises the lazy collections (@BatchSize)
        return page;
    }

    /**
     * Lab 8, variant 7: one window of keyset scrolling, newest first. The id is the tie-breaker
     * that makes the sort keys unique — without it two books of the same year could be skipped.
     */
    public Window<Book> scroll(ScrollPosition position, int size) {
        if (position.isInitial()) {
            position = FIRST_WINDOW;
        }
        Window<Book> window = repository.findBy(position, NEWEST_FIRST, Limit.of(size));
        window.forEach(book -> book.getTags().size());
        return window;
    }

    public Book findById(long id) {
        return repository.findWithDetailsById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    @Audited(action = "BOOK_CREATE", logArguments = true)
    public Book create(BookCommand command) {
        rejectDuplicate(command, null);    // Lab 7: 409
        Book book = new Book(command.title().trim(), command.genre(), command.year());
        book.setAvailable(command.available());
        book.setPublishedOn(command.publishedOn());
        command.tags().forEach(name -> book.addTag(tag(name)));
        author(command.author()).addBook(book);          // both sides of the link, see Author.addBook
        Book saved = repository.save(book);              // IDENTITY: the INSERT runs now to obtain the id
        notifications.viaPrimary("New book in the catalogue: " + saved.getTitle());
        return saved;
    }

    /**
     * Lab 8: no save() here. The book is managed by the persistence context of this transaction;
     * Hibernate compares it with the loaded snapshot at commit and issues the UPDATE itself
     * (with "and version = ?" because of @Version). createdAt is updatable = false and survives.
     */
    @Transactional
    @Audited(action = "BOOK_UPDATE", logArguments = true)
    public Book update(long id, BookCommand command) {
        Book book = findById(id);           // throws BookNotFoundException
        rejectDuplicate(command, id);
        citations.evict(id);
        book.setTitle(command.title().trim());
        book.setGenre(command.genre());
        book.setYear(command.year());
        book.setAvailable(command.available());
        book.setPublishedOn(command.publishedOn());
        if (book.getAuthor() == null || !book.getAuthor().getName().equalsIgnoreCase(command.author().trim())) {
            author(command.author()).addBook(book);
        }
        book.clearTags();
        command.tags().forEach(name -> book.addTag(tag(name)));
        return book;
    }

    /**
     * Lab 7: a second DELETE of the same id answers 404 — the state of the server does not
     * change (DELETE stays idempotent) and the client learns there was nothing to delete.
     * Lab 8: the book is taken out of its author's list; orphanRemoval turns that into the DELETE.
     */
    @Transactional
    @Audited(action = "BOOK_DELETE", logArguments = true)
    public boolean delete(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid identifier: " + id);
        }
        citations.evict(id);
        Optional<Book> found = repository.findById(id);
        if (found.isEmpty()) {
            return false;
        }
        Book book = found.get();
        if (book.getAuthor() != null) {
            book.getAuthor().removeBook(book);
        } else {
            repository.delete(book);
        }
        return true;
    }

    /** Lab 8, task 3.2: the bulk update needs a read-write transaction around it. */
    @Transactional
    public int archiveOlderThan(int year) {
        return repository.archiveOlderThan(year);
    }

    /** Same title and same author (ignoring case) is the same book; ignoreId lets PUT keep its own. */
    private void rejectDuplicate(BookCommand candidate, Long ignoreId) {
        String title = candidate.title().trim();
        String author = candidate.author().trim();
        boolean taken = ignoreId == null
                ? repository.existsByTitleIgnoreCaseAndAuthorNameIgnoreCase(title, author)
                : repository.existsByTitleIgnoreCaseAndAuthorNameIgnoreCaseAndIdNot(title, author, ignoreId);
        if (taken) {
            throw new DuplicateTitleException(candidate.title(), candidate.author());
        }
    }

    private Author author(String name) {
        String trimmed = name.trim();
        return authors.findByNameIgnoreCase(trimmed).orElseGet(() -> authors.save(new Author(trimmed, null)));
    }

    private Tag tag(String name) {
        String trimmed = name.trim().toLowerCase();
        return tags.findByNameIgnoreCase(trimmed).orElseGet(() -> tags.save(new Tag(trimmed)));
    }

    /**
     * Lab 4, self-invocation experiment. viaProxy=false calls this.delete(...) — the call never
     * leaves the object, so no BOOK_DELETE audit records appear; viaProxy=true calls self.delete(...).
     * Lab 8: read-write, otherwise the inner deletes would join a read-only transaction and never flush.
     */
    @Transactional
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
            return b.getAuthor().getName() + " (" + b.getYear() + "). " + b.getTitle() + ".";
        });
    }

    /** Deliberately slow (about 300 ms) so that TimingAspect prints a SLOW warning. */
    @Audited(action = "BOOK_STATS")
    public Map<Genre, Long> statistics() {
        sleep(300);
        Map<Genre, Long> byGenre = new EnumMap<>(Genre.class);
        for (Object[] row : repository.countPerGenre()) {
            byGenre.put((Genre) row[0], (Long) row[1]);
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
