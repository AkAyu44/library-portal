package kz.iitu.library.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.validation.NoDigits;

import java.time.LocalDate;
import java.util.List;

/**
 * Lab 7: what a client may send to POST/PUT /api/books.
 * No id and no createdAt — those belong to the server. The Lab 6 constraints and bundles
 * are kept, so the API and the HTML form validate the same way.
 */
@Schema(description = "A book as sent by the client")
public record BookRequest(
        @Schema(example = "Pro Spring 6")
        @NotBlank(message = "{book.title.required}") @Size(max = 120, message = "{book.title.size}") String title,

        @Schema(example = "Iuliana Cosmina")
        @NotBlank(message = "{book.author.required}") @NoDigits String author,

        // wrapper type: Jackson 3 rejects a missing primitive before validation could report it
        @Schema(example = "2023")
        @NotNull(message = "{book.year.range}")
        @Min(value = 1450, message = "{book.year.range}") @Max(value = 2100, message = "{book.year.range}") Integer year,

        @NotNull(message = "{book.genre.required}") Genre genre,

        @Schema(example = "true")
        Boolean available,

        @Schema(example = "2023-03-01")
        @PastOrPresent(message = "{book.published.past}") LocalDate publishedOn,

        // Lab 8: tag names; unknown ones are created (book_tag gets one row per tag)
        @Schema(example = "[\"java\", \"spring\"]")
        List<@NotBlank String> tags
) { }
