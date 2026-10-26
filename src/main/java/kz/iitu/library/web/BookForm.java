package kz.iitu.library.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.service.BookCommand;
import kz.iitu.library.validation.NoDigits;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;

/**
 * Lab 6: a mutable form object, separate from the Book entity —
 * the binder can fill it partially, the page is redisplayed with the input intact,
 * and there is no id field to mass-assign.
 */
public class BookForm {

    @NotBlank(message = "{book.title.required}")
    @Size(max = 120, message = "{book.title.size}")
    private String title;

    @NotBlank(message = "{book.author.required}")
    @NoDigits
    private String author;

    @Min(value = 1450, message = "{book.year.range}")
    @Max(value = 2100, message = "{book.year.range}")
    private int year;

    @NotNull(message = "{book.genre.required}")
    private Genre genre;

    private boolean available = true;

    // Lab 6, variant 7: "S-" = short date, no time → en: 10/12/26, ru: 12.10.2026; ISO accepted everywhere
    @NotNull(message = "{book.published.required}")
    @PastOrPresent(message = "{book.published.past}")
    @DateTimeFormat(style = "S-", fallbackPatterns = "yyyy-MM-dd")
    private LocalDate publishedOn;

    /** Lab 8: the form no longer builds the entity — the service resolves the author by name. */
    public BookCommand toCommand() {
        return new BookCommand(title, author, year, genre, available, publishedOn, Set.of());
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public LocalDate getPublishedOn() { return publishedOn; }
    public void setPublishedOn(LocalDate publishedOn) { this.publishedOn = publishedOn; }
}
