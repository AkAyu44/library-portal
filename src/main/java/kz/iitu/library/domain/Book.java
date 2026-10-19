package kz.iitu.library.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Lab 5: immutable domain record (id, title, author, year).
 * Lab 6: extended with genre, availability and the publication date (variant 7).
 * Lab 7: createdAt — set by the server, never sent by a client.
 */
public record Book(Long id, String title, String author, int year,
                   Genre genre, boolean available, LocalDate publishedOn, Instant createdAt) {

    /** A book that has not been stored yet: the repository assigns id and createdAt. */
    public Book(Long id, String title, String author, int year,
                Genre genre, boolean available, LocalDate publishedOn) {
        this(id, title, author, year, genre, available, publishedOn, null);
    }

    public Book withId(Long newId) {
        return new Book(newId, title, author, year, genre, available, publishedOn, createdAt);
    }

    public Book withCreatedAt(Instant instant) {
        return new Book(id, title, author, year, genre, available, publishedOn, instant);
    }
}
