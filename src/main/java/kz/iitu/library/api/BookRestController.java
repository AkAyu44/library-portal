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
import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.service.BookNotFoundException;
import kz.iitu.library.service.BookService;
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

    @Operation(summary = "List books, optionally filtered by author")
    @ApiResponse(responseCode = "200", description = "The (possibly empty) list of books")
    @GetMapping
    public List<BookResponse> list(
            @Parameter(description = "Part of the author's name, case-insensitive", example = "Bloch")
            @RequestParam(required = false) String author,
            @Parameter(description = "Maximum number of books", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        return mapper.toResponseList(service.findAll(author).stream().limit(limit).toList());
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
        Book saved = service.create(mapper.toDomain(request));
        return ResponseEntity
                .created(URI.create("/api/books/" + saved.id()))
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
        return mapper.toResponse(service.update(id, mapper.toDomain(request)));
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
