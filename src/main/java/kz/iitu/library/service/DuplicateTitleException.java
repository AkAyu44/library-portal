package kz.iitu.library.service;

/**
 * Lab 7: the catalogue already has a book with this title by this author.
 * A business conflict, not a malformed request — the API answers 409, not 400.
 */
public class DuplicateTitleException extends RuntimeException {

    private final String title;

    public DuplicateTitleException(String title, String author) {
        super("A book «" + title + "» by " + author + " is already in the catalogue");
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
