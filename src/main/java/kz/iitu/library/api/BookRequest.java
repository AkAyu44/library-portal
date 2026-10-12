package kz.iitu.library.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.validation.NoDigits;

import java.time.LocalDate;

/**
 * JSON body of POST/PUT /api/books. The Lab 6 constraints now protect the REST API too;
 * like BookForm it has no id, so a client cannot choose one (no mass assignment).
 */
public record BookRequest(
        @NotBlank(message = "{book.title.required}") @Size(max = 120, message = "{book.title.size}") String title,
        @NotBlank(message = "{book.author.required}") @NoDigits String author,
        // wrapper types: Jackson 3 rejects a missing primitive before validation could report it
        @NotNull(message = "{book.year.range}")
        @Min(value = 1450, message = "{book.year.range}") @Max(value = 2100, message = "{book.year.range}") Integer year,
        @NotNull(message = "{book.genre.required}") Genre genre,
        Boolean available,
        @PastOrPresent(message = "{book.published.past}") LocalDate publishedOn
) {
    public Book toBook() {
        return new Book(null, title, author, year, genre, Boolean.TRUE.equals(available), publishedOn);
    }
}
