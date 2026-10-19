package kz.iitu.library.api.dto;

import kz.iitu.library.domain.Book;
import org.springframework.stereotype.Component;

import java.util.List;

/** Lab 7: the single place that knows both the DTO shapes and the domain record. */
@Component
public class BookMapper {

    /** id and createdAt stay null: the repository assigns them, a client never can. */
    public Book toDomain(BookRequest request) {
        return new Book(null, request.title(), request.author(), request.year(), request.genre(),
                Boolean.TRUE.equals(request.available()), request.publishedOn());
    }

    public BookResponse toResponse(Book book) {
        return new BookResponse(book.id(), book.title(), book.author(), book.year(), book.genre(),
                book.available(), book.publishedOn(), book.createdAt());
    }

    public List<BookResponse> toResponseList(List<Book> books) {
        return books.stream().map(this::toResponse).toList();
    }
}
