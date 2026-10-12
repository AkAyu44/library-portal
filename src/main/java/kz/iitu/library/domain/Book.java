package kz.iitu.library.domain;

import java.time.LocalDate;

/**
 * Lab 5: immutable domain record (id, title, author, year).
 * Lab 6: extended with genre, availability and the publication date (variant 7).
 */
public record Book(Long id, String title, String author, int year,
                   Genre genre, boolean available, LocalDate publishedOn) {

    public Book withId(Long newId) {
        return new Book(newId, title, author, year, genre, available, publishedOn);
    }
}
