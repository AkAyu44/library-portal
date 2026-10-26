package kz.iitu.library.service;

import kz.iitu.library.domain.Genre;

import java.time.LocalDate;
import java.util.Set;

/**
 * Lab 8: what the API (BookRequest) and the HTML form (BookForm) hand to the service.
 * The entity cannot be built by them any more: the author is an entity looked up by name,
 * tags are entities too, and id, createdAt and version belong to the database.
 */
public record BookCommand(String title, String author, int year, Genre genre,
                          boolean available, LocalDate publishedOn, Set<String> tags) {

    public BookCommand {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}
