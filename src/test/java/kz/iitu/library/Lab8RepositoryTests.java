package kz.iitu.library;

import jakarta.persistence.EntityManager;
import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.persistence.SqlRecorder;
import kz.iitu.library.repository.BookRepository;
import kz.iitu.library.service.BookCommand;
import kz.iitu.library.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lab 8: the data access layer against PostgreSQL (database springlab_test, see config/application.yml).
 * Every test runs in a transaction that is rolled back, so the seed data stays as it was.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Lab8RepositoryTests {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("year"), Sort.Order.desc("id"));

    @Autowired BookRepository books;
    @Autowired BookService service;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    /** Each measurement starts with an empty persistence context, as in a new request. */
    @BeforeEach
    void emptyPersistenceContext() {
        entityManager.clear();
    }

    @Test
    @DisplayName("task 3.1: the five derived queries return rows from the seed data")
    void derivedQueries() {
        assertThat(books.findByGenre(Genre.PROGRAMMING)).hasSizeGreaterThanOrEqualTo(5);
        assertThat(books.findByTitleContainingIgnoreCase("JAVA")).extracting(Book::getTitle)
                .contains("Effective Java", "Java Puzzlers", "Java Concurrency in Practice");
        assertThat(books.findByYearBetweenOrderByYearDesc(2005, 2018)).extracting(Book::getYear)
                .isSortedAccordingTo((a, b) -> b - a);
        // other test classes add SCIENCE books to the shared database, so only the shape is checked
        List<Book> top3 = books.findTop3ByGenreOrderByYearDesc(Genre.SCIENCE);
        assertThat(top3).hasSize(3).allMatch(b -> b.getGenre() == Genre.SCIENCE);
        assertThat(top3).extracting(Book::getYear).isSortedAccordingTo((a, b) -> b - a);
        assertThat(books.countByAuthorName("Joshua Bloch")).isEqualTo(3);
    }

    @Test
    @DisplayName("question 13: a % in the argument is escaped and matches nothing")
    void percentIsEscaped() {
        assertThat(books.findByTitleContainingIgnoreCase("%")).isEmpty();
    }

    @Test
    @DisplayName("task 3.2: the JPQL query with named parameters and the bulk update")
    void jpqlAndBulkUpdate() {
        assertThat(books.search(Genre.PROGRAMMING, 2010)).extracting(Book::getTitle)
                .containsSubsequence("Effective Java", "Spring Boot in Action", "Spring in Action");

        Book old = books.findByTitleContainingIgnoreCase("Brief History").getFirst();
        assertThat(old.isArchived()).isFalse();
        assertThat(service.archiveOlderThan(2000)).isEqualTo(1);
        // clearAutomatically = true: the stale managed copy is gone, the next read sees the UPDATE
        assertThat(books.findById(old.getId()).orElseThrow().isArchived()).isTrue();
    }

    @Test
    @DisplayName("task 2: addBook sets the owning side, so author_id is written; book_tag gets a row per tag")
    void owningSideWritesTheForeignKey() {
        Book saved = service.create(new BookCommand("Effective Java 4th", "Joshua Bloch", 2026,
                Genre.PROGRAMMING, true, null, Set.of("java", "new")));
        entityManager.flush();

        Long authorId = jdbc.queryForObject("select author_id from book where id = ?", Long.class, saved.getId());
        Long blochId = jdbc.queryForObject("select id from author where name = ?", Long.class, "Joshua Bloch");
        assertThat(authorId).isEqualTo(blochId);
        assertThat(jdbc.queryForObject("select count(*) from book_tag where book_id = ?", Integer.class,
                saved.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("task 3.4: N+1 is 1 + one select per distinct author; the fetch join makes it 1")
    void nPlusOne() {
        long authors = jdbc.queryForObject("select count(distinct author_id) from book", Long.class);

        List<String> before = recordSql(() -> books.findAll().stream().map(b -> b.getAuthor().getName()).toList());
        entityManager.clear();
        List<String> after = recordSql(() -> books.findAllWithAuthor().stream().map(b -> b.getAuthor().getName()).toList());

        assertThat(before).hasSize((int) (1 + authors));
        assertThat(after).hasSize(1);
        assertThat(after.getFirst()).contains("join author");
    }

    @Test
    @DisplayName("task 4.3: a Page costs a select and a count, a Slice only the select")
    void pageAndSlice() {
        Pageable second = PageRequest.of(1, 3, Sort.by("title"));
        assertThat(recordSql(() -> books.findAll(second))).hasSize(2)
                .anySatisfy(sql -> assertThat(sql).startsWith("select count"));
        entityManager.clear();
        assertThat(recordSql(() -> books.findSliceBy(second))).hasSize(1);
    }

    @Test
    @DisplayName("variant 7: keyset windows walk the whole catalogue without repeats, no OFFSET in the SQL")
    void keysetScrolling() {
        List<Long> seen = new ArrayList<>();
        List<String> sql = new ArrayList<>();
        ScrollPosition position = BookService.FIRST_WINDOW;
        boolean more = true;
        while (more) {
            SqlRecorder.start();
            Window<Book> window = books.findBy(position, NEWEST_FIRST, Limit.of(3));
            sql.addAll(SqlRecorder.stop());
            window.forEach(b -> seen.add(b.getId()));
            more = window.hasNext();
            if (more) {
                position = window.positionAt(window.size() - 1);
            }
        }

        assertThat(seen).doesNotHaveDuplicates().hasSize((int) books.count());
        assertThat(sql).allSatisfy(s -> assertThat(s).contains("year<").doesNotContain("offset"));
    }

    @Test
    @DisplayName("task 4.2: GET /api/books?page=1&size=3&sort=year,desc is a sorted page with the total")
    void pagedEndpoint() throws Exception {
        mvc.perform(get("/api/books").param("page", "1").param("size", "3").param("sort", "year,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(3))
                .andExpect(jsonPath("$.page.totalElements").value((int) books.count()));
    }

    @Test
    @DisplayName("task 4.2: sort=nosuchfield is a 400 problem detail, not a 500")
    void unknownSortPropertyIs400() throws Exception {
        mvc.perform(get("/api/books").param("sort", "nosuchfield"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(endsWith("/unknown-sort-property")))
                .andExpect(jsonPath("$.property").value("nosuchfield"));
    }

    private static List<String> recordSql(Supplier<?> action) {
        SqlRecorder.start();
        List<String> sql;
        try {
            action.get();
        } finally {
            sql = SqlRecorder.stop();
        }
        return sql;
    }
}
