package kz.iitu.library.persistence;

import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.repository.BookRepository;
import kz.iitu.library.service.BookService;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Lab 8, tasks 3.3, 3.4, 4.2, 4.3 and variant 7: calls every repository method once and reports
 * what it returned, how many SQL statements it caused and the statements themselves.
 * Each measurement runs in its own transaction, so it starts with an empty persistence context
 * and nothing is served from the first-level cache of a previous call.
 */
@Component
public class QueryLab {

    /** One row of the tables in the report. */
    public record Measurement(String method, int rows, Object result, int statements, List<String> sql) { }

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("year"), Sort.Order.desc("id"));

    private final BookRepository books;
    private final TransactionTemplate readOnly;
    private final TransactionTemplate readWrite;

    public QueryLab(BookRepository books, PlatformTransactionManager transactionManager) {
        this.books = books;
        this.readWrite = new TransactionTemplate(transactionManager);
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    /** Task 3.3: the five derived queries, the JPQL query and the bulk update. */
    public List<Measurement> queries() {
        return List.of(
                measure("findByGenre(PROGRAMMING)", readOnly,
                        () -> titles(books.findByGenre(Genre.PROGRAMMING))),
                measure("findByTitleContainingIgnoreCase(\"java\")", readOnly,
                        () -> titles(books.findByTitleContainingIgnoreCase("java"))),
                measure("findByYearBetweenOrderByYearDesc(2005, 2018)", readOnly,
                        () -> titles(books.findByYearBetweenOrderByYearDesc(2005, 2018))),
                measure("findTop3ByGenreOrderByYearDesc(PROGRAMMING)", readOnly,
                        () -> titles(books.findTop3ByGenreOrderByYearDesc(Genre.PROGRAMMING))),
                measure("countByAuthorName(\"Joshua Bloch\")", readOnly,
                        () -> books.countByAuthorName("Joshua Bloch")),
                measure("search(PROGRAMMING, 2010) [JPQL]", readOnly,
                        () -> titles(books.search(Genre.PROGRAMMING, 2010))),
                measure("archiveOlderThan(2000) [bulk update]", readWrite,
                        () -> books.archiveOlderThan(2000)),
                // question 13: a % in the argument is escaped, it is not a wildcard
                measure("findByTitleContainingIgnoreCase(\"%\")", readOnly,
                        () -> titles(books.findByTitleContainingIgnoreCase("%"))));
    }

    /** Task 3.4: N+1 before the fix, and the two cures. */
    public List<Measurement> nPlusOne() {
        return List.of(
                measure("findAll() + book.getAuthor().getName()", readOnly,
                        () -> withAuthors(books.findAll())),
                measure("findAllWithAuthor() [join fetch]", readOnly,
                        () -> withAuthors(books.findAllWithAuthor())),
                measure("findByGenre(PROGRAMMING) [@EntityGraph]", readOnly,
                        () -> withAuthors(books.findByGenre(Genre.PROGRAMMING))));
    }

    /** Tasks 4.2 and 4.3: the same request as a Page and as a Slice. */
    public List<Measurement> paging(Pageable pageable) {
        return List.of(
                measure("Page<Book> findAll(" + pageable + ")", readOnly, () -> {
                    Page<Book> page = books.findAll(pageable);
                    return pageInfo(page.getContent(), Map.of(
                            "totalElements", page.getTotalElements(), "totalPages", page.getTotalPages()));
                }),
                measure("Slice<Book> findSliceBy(" + pageable + ")", readOnly, () -> {
                    Slice<Book> slice = books.findSliceBy(pageable);
                    return pageInfo(slice.getContent(), Map.of("hasNext", slice.hasNext()));
                }));
    }

    /**
     * Variant 7: two windows of keyset scrolling, then the same second "page" fetched by offset.
     * The keyset query seeks straight to the cursor; the offset query reads and throws away
     * the first rows, and the Page version also needs a count query.
     */
    public List<Measurement> scrolling() {
        Window<Book> first = readOnly.execute(s -> books.findBy(BookService.FIRST_WINDOW, NEWEST_FIRST, Limit.of(3)));
        ScrollPosition afterFirst = first.positionAt(first.size() - 1);
        return List.of(
                measure("Window findBy(first window, year desc, id desc, Limit 3)", readOnly,
                        () -> windowInfo(books.findBy(BookService.FIRST_WINDOW, NEWEST_FIRST, Limit.of(3)))),
                measure("Window findBy(" + afterFirst + ", Limit 3)", readOnly,
                        () -> windowInfo(books.findBy(afterFirst, NEWEST_FIRST, Limit.of(3)))),
                measure("Page findAll(page 1, size 3, year desc, id desc) [offset paging]", readOnly, () -> {
                    Page<Book> page = books.findAll(PageRequest.of(1, 3, NEWEST_FIRST));
                    return pageInfo(page.getContent(), Map.of("totalElements", page.getTotalElements()));
                }));
    }

    private Measurement measure(String method, TransactionTemplate tx, Supplier<Object> action) {
        SqlRecorder.start();
        Object result;
        List<String> sql;
        try {
            result = tx.execute(status -> action.get());
        } finally {
            sql = SqlRecorder.stop();
        }
        return new Measurement(method, rows(result), result, sql.size(), sql);
    }

    private static int rows(Object result) {
        if (result instanceof List<?> list) {
            return list.size();
        }
        if (result instanceof Map<?, ?> map && map.get("content") instanceof List<?> content) {
            return content.size();
        }
        return result instanceof Number number ? number.intValue() : 1;
    }

    private static List<String> titles(List<Book> list) {
        return list.stream().map(b -> b.getTitle() + " (" + b.getYear() + ")").toList();
    }

    /** Touches book.getAuthor().getName() — the line that triggers N+1 when the author is lazy. */
    private static List<String> withAuthors(List<Book> list) {
        return list.stream().map(b -> b.getTitle() + " — " + b.getAuthor().getName()).toList();
    }

    private static Map<String, Object> pageInfo(List<Book> content, Map<String, Object> extra) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("content", titles(content));
        info.putAll(extra);
        return info;
    }

    private static Map<String, Object> windowInfo(Window<Book> window) {
        return pageInfo(window.getContent(), Map.of("hasNext", window.hasNext()));
    }
}
