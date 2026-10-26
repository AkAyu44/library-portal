package kz.iitu.library.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lab 8: the "one" side of book -> author. The inverse side: mappedBy = "author" says
 * "the column is described by Book.author", so the table "author" has no books column.
 */
@Entity
@Table(name = "author")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 60)
    private String country;

    // ALL: saving or deleting an author saves or deletes its books;
    // orphanRemoval: a book taken out of this list is deleted, not left with author_id = null
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    protected Author() { }

    public Author(String name, String country) {
        this.name = name;
        this.country = country;
    }

    /** Keeps both sides in step: JPA reads only the owning side (Book.author) when it writes author_id. */
    public void addBook(Book book) {
        books.add(book);
        book.setAuthor(this);
    }

    public void removeBook(Book book) {
        books.remove(book);
        book.setAuthor(null);
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    public String getCountry() { return country; }

    public List<Book> getBooks() { return Collections.unmodifiableList(books); }

    @Override
    public String toString() {
        return "Author{id=" + id + ", name='" + name + "'}";
    }
}
