package kz.iitu.library.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.iitu.library.domain.Genre;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Lab 7: what the API promises to return. A separate type from BookRequest:
 * it carries the server-owned id and createdAt, and renaming a storage field
 * never leaks into the contract because the mapper is the only place that knows both.
 */
@Schema(description = "A book as returned by the API")
@Relation(collectionRelation = "books", itemRelation = "book")
public record BookResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Effective Java") String title,
        @Schema(example = "Joshua Bloch") String author,
        @Schema(example = "2018") int year,
        Genre genre,
        boolean available,
        @Schema(example = "2018-01-06") LocalDate publishedOn,
        @Schema(description = "Set by the server when the book was stored") Instant createdAt
) { }
