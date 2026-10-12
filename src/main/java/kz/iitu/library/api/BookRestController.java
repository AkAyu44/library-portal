package kz.iitu.library.api;

import jakarta.validation.Valid;
import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.service.BookService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Lab 5: the REST side of the catalogue — every part of the request is bound to a parameter
 * (@RequestParam, @PathVariable, @RequestBody) and ResponseEntity sets meaningful status codes.
 * Lab 6: a missing book is a BookNotFoundException → RFC 9457 problem detail (ApiExceptionHandler).
 */
@RestController
@RequestMapping("/api/books")
public class BookRestController {

    private final BookService service;

    public BookRestController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public List<Book> list(@RequestParam(required = false) String author,
                           @RequestParam(defaultValue = "10") int limit) {
        return service.findAll(author).stream().limit(limit).toList();
    }

    @GetMapping("/{id}")
    public Book find(@PathVariable long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<Book> create(@Valid @RequestBody BookRequest request) {
        Book saved = service.create(request.toBook());
        return ResponseEntity.created(URI.create("/api/books/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    public Book replace(@PathVariable long id, @Valid @RequestBody BookRequest request) {
        return service.update(id, request.toBook());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /** Lab 4: self-invocation experiment, e.g. DELETE /api/books/bulk?ids=1,2&viaProxy=false */
    @DeleteMapping("/bulk")
    public Map<Long, Boolean> deleteBulk(@RequestParam List<Long> ids,
                                         @RequestParam(defaultValue = "true") boolean viaProxy) {
        return service.deleteBulk(ids, viaProxy);
    }

    /** Lab 5, variant 6: plain-text citation; Accept: application/json gives 406. */
    @GetMapping(value = "/{id}/citation", produces = MediaType.TEXT_PLAIN_VALUE)
    public String citation(@PathVariable long id) {
        return service.citation(id);
    }

    /** Lab 4: a deliberately slow, audited operation (SLOW warning of TimingAspect). */
    @GetMapping("/stats")
    public Map<Genre, Long> stats() {
        return service.statistics();
    }
}
