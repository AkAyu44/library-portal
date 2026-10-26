package kz.iitu.library.repository;

import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Lab 5 kept the catalogue in a ConcurrentHashMap; Lab 8 replaces it with an interface that
 * nobody implements by hand. At startup Spring Data creates a proxy (SimpleJpaRepository for the
 * CRUD methods of JpaRepository) and parses every other method name into JPQL.
 */
public interface BookRepository extends JpaRepository<Book, Long> {

    // ---------------- Task 3.1: five derived queries ----------------

    /** Also the declarative cure for N+1: the author comes in the same select (left join). */
    @EntityGraph(attributePaths = "author")
    List<Book> findByGenre(Genre genre);

    /** where upper(b.title) like upper(?) escape '\' — the argument is wrapped in % ... %. */
    List<Book> findByTitleContainingIgnoreCase(String fragment);

    List<Book> findByYearBetweenOrderByYearDesc(int from, int to);

    /** Subject "findTop3", predicate "ByGenre", order "OrderByYearDesc" → fetch first 3 rows only. */
    List<Book> findTop3ByGenreOrderByYearDesc(Genre genre);

    /** A nested property: b.author.name → an inner join to "author". */
    long countByAuthorName(String name);

    // used by BookService for the Lab 7 duplicate rule (409) — no more scanning a list in Java
    boolean existsByTitleIgnoreCaseAndAuthorNameIgnoreCase(String title, String author);

    boolean existsByTitleIgnoreCaseAndAuthorNameIgnoreCaseAndIdNot(String title, String author, Long id);

    // ---------------- Task 3.2: JPQL and a bulk update ----------------

    @Query("""
           select b from Book b
           where b.genre = :genre and b.year >= :year
           order by b.title""")
    List<Book> search(@Param("genre") Genre genre, @Param("year") int year);

    /** One UPDATE statement, the persistence context is bypassed; @Transactional is on the caller. */
    @Modifying(clearAutomatically = true)
    @Query("update Book b set b.archived = true where b.year < :year")
    int archiveOlderThan(@Param("year") int year);

    /** Lab 4 statistics, now counted by the database: one row per genre. */
    @Query("select b.genre, count(b) from Book b group by b.genre")
    List<Object[]> countPerGenre();

    // ---------------- Task 3.4: the cure for N+1 ----------------

    @Query("select b from Book b join fetch b.author")
    List<Book> findAllWithAuthor();

    /** One book with everything the details page and GET /api/books/{id} print. */
    @Query("select b from Book b left join fetch b.author left join fetch b.tags where b.id = :id")
    Optional<Book> findWithDetailsById(@Param("id") Long id);

    // ---------------- Task 4: pagination and sorting ----------------

    /** JpaRepository.findAll(Pageable) re-declared only to add the entity graph. */
    @Override
    @EntityGraph(attributePaths = "author")
    Page<Book> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Book> findByGenre(Genre genre, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Book> findByAuthorNameContainingIgnoreCase(String author, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Book> findByGenreAndAuthorNameContainingIgnoreCase(Genre genre, String author, Pageable pageable);

    /** Task 4.3: the same page as a Slice — no count query, one extra row tells whether there is more. */
    @EntityGraph(attributePaths = "author")
    Slice<Book> findSliceBy(Pageable pageable);

    // ---------------- Individual variant 7: Window and keyset scrolling ----------------

    /**
     * Keyset (seek) scrolling: the next window starts after the sort keys of the last row —
     * where (year < ? or year = ? and id < ?) — instead of skipping OFFSET rows.
     * BookService never passes ScrollPosition.keyset() (the empty initial position): Spring Data
     * caches the query rendered for the first call, and a query rendered without the keyset
     * predicate would then be reused for every later window (found in this lab, see the report).
     */
    @EntityGraph(attributePaths = "author")
    Window<Book> findBy(ScrollPosition position, Sort sort, Limit limit);
}
