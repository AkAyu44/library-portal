package kz.iitu.library.persistence;

import kz.iitu.library.domain.Author;
import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.domain.Tag;
import kz.iitu.library.repository.AuthorRepository;
import kz.iitu.library.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * Lab 8, task 2.2: seed data for the development profiles only ("default" is Spring's name for
 * a run without an active profile, "dev" is ours from lab 3; prod never gets test data).
 * Three authors, eight books, four tags; Joshua Bloch and Stephen Hawking own three books each,
 * several books carry two tags. Books are saved through the author: cascade = ALL persists them.
 */
@Component
@Profile({"default", "dev"})
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AuthorRepository authors;
    private final TagRepository tags;

    public DataSeeder(AuthorRepository authors, TagRepository tags) {
        this.authors = authors;
        this.tags = tags;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (authors.count() > 0) {
            return;
        }
        Map<String, Tag> tag = Map.of(
                "java", tags.save(new Tag("java")),
                "spring", tags.save(new Tag("spring")),
                "classic", tags.save(new Tag("classic")),
                "physics", tags.save(new Tag("physics")));

        Author bloch = new Author("Joshua Bloch", "USA");
        bloch.addBook(book("Effective Java", Genre.PROGRAMMING, 2018, LocalDate.of(2018, 1, 6), true,
                tag.get("java"), tag.get("classic")));
        bloch.addBook(book("Java Puzzlers", Genre.PROGRAMMING, 2005, LocalDate.of(2005, 7, 1), true,
                tag.get("java")));
        bloch.addBook(book("Java Concurrency in Practice", Genre.PROGRAMMING, 2006, LocalDate.of(2006, 5, 9), false,
                tag.get("java"), tag.get("classic")));

        Author walls = new Author("Craig Walls", "USA");
        walls.addBook(book("Spring in Action", Genre.PROGRAMMING, 2022, LocalDate.of(2022, 2, 1), false,
                tag.get("java"), tag.get("spring")));
        walls.addBook(book("Spring Boot in Action", Genre.PROGRAMMING, 2015, LocalDate.of(2015, 12, 1), true,
                tag.get("spring")));

        Author hawking = new Author("Stephen Hawking", "United Kingdom");
        hawking.addBook(book("A Brief History of Time", Genre.SCIENCE, 1988, LocalDate.of(1988, 4, 1), true,
                tag.get("physics"), tag.get("classic")));
        hawking.addBook(book("The Universe in a Nutshell", Genre.SCIENCE, 2001, LocalDate.of(2001, 11, 6), true,
                tag.get("physics")));
        hawking.addBook(book("Brief Answers to the Big Questions", Genre.SCIENCE, 2018, LocalDate.of(2018, 10, 16), true,
                tag.get("physics")));

        authors.save(bloch);      // cascade = ALL: the INSERTs of the three books follow the author's
        authors.save(walls);
        authors.save(hawking);
        log.info("Lab 8 seed: {} authors, 8 books, {} tags", authors.count(), tags.count());
    }

    private static Book book(String title, Genre genre, int year, LocalDate publishedOn, boolean available,
                             Tag... bookTags) {
        Book book = new Book(title, genre, year);
        book.setPublishedOn(publishedOn);
        book.setAvailable(available);
        for (Tag t : bookTags) {
            book.addTag(t);
        }
        return book;
    }
}
