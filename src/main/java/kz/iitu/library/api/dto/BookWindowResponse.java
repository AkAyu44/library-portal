package kz.iitu.library.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Lab 8, variant 7: one window of keyset scrolling. There is no total and no page number —
 * only the books and the cursor (sort keys of the last book) to ask for the next window with.
 */
@Schema(description = "A window of books and the cursor of the next one")
public record BookWindowResponse(List<BookResponse> content, boolean hasNext, Cursor next) {

    @Schema(description = "Pass as afterYear and afterId to get the next window")
    public record Cursor(int year, long id) { }
}
