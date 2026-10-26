package kz.iitu.library.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.iitu.library.api.dto.BookMapper;
import kz.iitu.library.api.dto.BookRequest;
import kz.iitu.library.api.dto.BookResponse;
import kz.iitu.library.api.dto.BookWindowResponse;
import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.service.BookNotFoundException;
import kz.iitu.library.service.BookService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Lab 5: the REST side of the catalogue — @RequestParam, @PathVariable, @RequestBody, ResponseEntity.
 * Lab 7: the controller speaks DTOs only (BookRequest in, BookResponse out, BookMapper between),
 * every outcome has a deliberate status code, errors are ProblemDetail from ApiExceptionHandler,
 * the contract is documented with OpenAPI; variant 7 — GET /{id} is a HATEOAS EntityModel.
 * Lab 8: the collection is paged and sorted; variant 7 — keyset scrolling with Window.
 */
@Tag(name = "Books", description = "The catalogue of books")
@RestController
@RequestMapping("/api/books")
public class BookRestController {

    private final BookService service;
    private final BookMapper mapper;

    public BookRestController(BookService service, BookMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /**
     * Lab 8, task 4: a page instead of the whole table. Spring MVC builds the Pageable from
     * ?page=1&size=3&sort=year,desc; without parameters it is page 0, five books, sorted by title.
     */
    @Operation(summary = "One page of books, optionally filtered by genre and/or author")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The page: content plus size, number, totalElements, totalPages"),
            @ApiResponse(responseCode = "400", description = "Sorting by a property the entity does not have",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public Page<BookResponse> list(
            @Parameter(description = "Only books of this genre", example = "PROGRAMMING")
            @RequestParam(required = false) Genre genre,
            @Parameter(description = "Part of the author's name, case-insensitive", example = "Bloch")
            @RequestParam(required = false) String author,
            @ParameterObject @PageableDefault(size = 5, sort = "title") Pageable pageable) {
        return service.find(genre, author, pageable).map(mapper::toResponse);
    }

    /**
     * Lab 8, individual variant 7: keyset scrolling, newest first. The first call has no cursor;
     * every next call passes the year and id of the last book it received (the "next" object).
     */
    @Operation(summary = "Variant 7: scroll the catalogue by keyset (year desc, id desc)")
    @GetMapping("/scroll")
    public BookWindowResponse scroll(
            @Parameter(description = "Books per window", example = "3")
            @RequestParam(defaultValue = "3") int size,
            @Parameter(description = "Year of the last book of the previous window", example = "2018")
            @RequestParam(required = false) Integer afterYear,
            @Parameter(description = "Id of the last book of the previous window", example = "8")
            @RequestParam(required = false) Long afterId) {
        ScrollPosition position = afterYear == null || afterId == null
                ? ScrollPosition.keyset()
                : ScrollPosition.forward(Map.of("year", afterYear, "id", afterId));
        Window<Book> window = service.scroll(position, Math.clamp(size, 1, 50));
        List<BookResponse> content = mapper.toResponseList(window.getContent());
        BookWindowResponse.Cursor next = null;
        if (window.hasNext() && !content.isEmpty()) {
            BookResponse last = content.getLast();
            next = new BookWindowResponse.Cursor(last.year(), last.id());
        }
        return new BookWindowResponse(content, window.hasNext(), next);
    }

    /** Lab 7, variant 7 (HATEOAS): the book plus links to itself, the collection and its citation. */
    @Operation(summary = "Find a book by its identifier",
            description = "Returns HAL: the book's fields and _links (self, books, citation)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The book was found"),
            @ApiResponse(responseCode = "404", description = "No book with this id",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public EntityModel<BookResponse> find(
            @Parameter(description = "Identifier of the book", example = "1") @PathVariable long id) {
        BookResponse book = mapper.toResponse(service.findById(id));
        return EntityModel.of(book,
                linkTo(methodOn(BookRestController.class).find(id)).withSelfRel(),
                linkTo(BookRestController.class).withRel("books"),
                linkTo(BookRestController.class).slash(id).slash("citation").withRel("citation"));
    }

    @Operation(summary = "Add a book to the catalogue")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created; Location holds the URI of the new book"),
            @ApiResponse(responseCode = "400", description = "The body breaks a validation rule",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "A book with this title and author already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        Book saved = service.create(mapper.toCommand(request));
        return ResponseEntity
                .created(URI.create("/api/books/" + saved.getId()))
                .body(mapper.toResponse(saved));
    }

    @Operation(summary = "Replace a book (every field of the body is written)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The book was replaced"),
            @ApiResponse(responseCode = "400", description = "The body breaks a validation rule",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No book with this id",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Another book already has this title and author",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public BookResponse replace(@Parameter(description = "Identifier of the book", example = "1")
                                @PathVariable long id,
                                @Valid @RequestBody BookRequest request) {
        return mapper.toResponse(service.update(id, mapper.toCommand(request)));
    }

    /** A second DELETE of the same id answers 404 — see BookService.delete for why. */
    @Operation(summary = "Delete a book")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted, no body"),
            @ApiResponse(responseCode = "404", description = "No book with this id (also on a repeated DELETE)",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Identifier of the book", example = "1")
                                       @PathVariable long id) {
        if (!service.delete(id)) {
            throw new BookNotFoundException(id);
        }
        return ResponseEntity.noContent().build();
    }

    /** Lab 4: self-invocation experiment, e.g. DELETE /api/books/bulk?ids=1,2&viaProxy=false */
    @Operation(summary = "Lab 4: delete several books (self-invocation experiment)")
    @DeleteMapping("/bulk")
    public Map<Long, Boolean> deleteBulk(@RequestParam List<Long> ids,
                                         @RequestParam(defaultValue = "true") boolean viaProxy) {
        return service.deleteBulk(ids, viaProxy);
    }

    /** Lab 5, variant 6: plain-text citation; Accept: application/json gives 406. */
    @Operation(summary = "Lab 5: the citation of a book as plain text")
    @GetMapping(value = "/{id}/citation", produces = MediaType.TEXT_PLAIN_VALUE)
    public String citation(@PathVariable long id) {
        return service.citation(id);
    }

    /** Lab 4: a deliberately slow, audited operation (SLOW warning of TimingAspect). */
    @Operation(summary = "Lab 4: number of books per genre (deliberately slow)")
    @GetMapping("/stats")
    public Map<Genre, Long> stats() {
        return service.statistics();
    }
}
