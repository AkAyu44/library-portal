package kz.iitu.library.service;

/** Lab 6: thrown by the service, turned into a response by the @ControllerAdvice classes. */
public class BookNotFoundException extends RuntimeException {

    private final long id;

    public BookNotFoundException(long id) {
        super("Book with id " + id + " does not exist");
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
