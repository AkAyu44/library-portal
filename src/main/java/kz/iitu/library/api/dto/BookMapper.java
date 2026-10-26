package kz.iitu.library.api.dto;

import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Tag;
import kz.iitu.library.service.BookCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Lab 7: the single place that knows both the DTO shapes and the domain model.
 * Lab 8: the entity never leaves through the controller — lazy associations would explode
 * during JSON serialisation (open-in-view is off) — so every response is built here.
 */
@Component
public class BookMapper {

    /** id, createdAt and version stay with the database: a client can never set them. */
    public BookCommand toCommand(BookRequest request) {
        return new BookCommand(request.title(), request.author(), request.year(), request.genre(),
                Boolean.TRUE.equals(request.available()), request.publishedOn(),
                request.tags() == null ? Set.of() : Set.copyOf(request.tags()));
    }

    public BookResponse toResponse(Book book) {
        List<String> tags = book.getTags().stream().map(Tag::getName).sorted().toList();
        return new BookResponse(book.getId(), book.getTitle(),
                book.getAuthor() == null ? null : book.getAuthor().getName(),
                book.getYear(), book.getGenre(), book.isAvailable(), book.isArchived(),
                book.getPublishedOn(), tags, book.getCreatedAt());
    }

    public List<BookResponse> toResponseList(List<Book> books) {
        return books.stream().map(this::toResponse).toList();
    }
}
