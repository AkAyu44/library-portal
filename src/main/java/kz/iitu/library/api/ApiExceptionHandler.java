package kz.iitu.library.api;

import kz.iitu.library.service.BookNotFoundException;
import kz.iitu.library.service.DuplicateTitleException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lab 6: RFC 9457 problem details for the @RestController classes.
 * Lab 7: the single place where API exceptions become responses (application/problem+json).
 * Extending ResponseEntityExceptionHandler gives problem details for the standard Spring MVC
 * failures (invalid body, unreadable JSON, type mismatch, ...) without writing them.
 * Scoped by annotation, so the HTML pages of lab 6 keep their own error page; because
 * @RestController is meta-annotated with @Controller, HIGHEST_PRECEDENCE makes this advice win.
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERRORS = "https://api.iitu.kz/errors/";

    @ExceptionHandler(BookNotFoundException.class)
    public ProblemDetail handleNotFound(BookNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(ERRORS + "book-not-found"));
        problem.setTitle("Book not found");
        problem.setProperty("bookId", ex.getId());
        return problem;
    }

    /** Lab 7: a business conflict — 409, not 400 (the request is well-formed) and not 500. */
    @ExceptionHandler(DuplicateTitleException.class)
    public ProblemDetail handleDuplicate(DuplicateTitleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create(ERRORS + "duplicate-title"));
        problem.setTitle("Duplicate title");
        // an extra field beyond the five standard ones; not called "title" — that name is
        // already the standard RFC 9457 field, and the JSON would carry the key twice
        problem.setProperty("bookTitle", ex.getTitle());
        return problem;
    }

    // Lab 4 returned 500 for remove(0); a client error deserves 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    /**
     * The 400 for an invalid body is produced by the base class; we only add which fields were
     * rejected and why, so a client can show the message next to the right input.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ResponseEntity<Object> response = super.handleMethodArgumentNotValid(ex, headers, status, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            Map<String, String> errors = new LinkedHashMap<>();
            for (FieldError error : ex.getBindingResult().getFieldErrors()) {
                errors.putIfAbsent(error.getField(), error.getDefaultMessage());
            }
            problem.setTitle("Validation failed");
            problem.setProperty("errors", errors);
        }
        return response;
    }
}
