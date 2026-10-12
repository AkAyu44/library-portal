package kz.iitu.library.repository;

import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Lab 5: in-memory storage — thread-safe map + id sequence (a singleton is shared by all requests). */
@Repository
public class BookRepository {

    private final Map<Long, Book> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public BookRepository() {
        save(new Book(null, "Effective Java", "Joshua Bloch", 2018,
                Genre.PROGRAMMING, true, LocalDate.of(2018, 1, 6)));
        save(new Book(null, "Clean Code", "Robert Martin", 2008,
                Genre.PROGRAMMING, true, LocalDate.of(2008, 8, 1)));
        save(new Book(null, "Spring in Action", "Craig Walls", 2022,
                Genre.PROGRAMMING, false, LocalDate.of(2022, 2, 1)));
        save(new Book(null, "A Brief History of Time", "Stephen Hawking", 1988,
                Genre.SCIENCE, true, LocalDate.of(1988, 4, 1)));
    }

    public List<Book> findAll() {
        return List.copyOf(storage.values());
    }

    public Optional<Book> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Book save(Book book) {
        long id = book.id() != null ? book.id() : sequence.incrementAndGet();
        Book stored = book.withId(id);
        storage.put(id, stored);
        return stored;
    }

    public boolean deleteById(long id) {
        return storage.remove(id) != null;
    }
}
