package kz.iitu.library.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.iitu.library.persistence.QueryLab;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lab 8: the "temporary endpoint" of task 3.3 — every repository method is called once and the
 * SQL Hibernate sent is returned next to the result, so the tables of the report are measured.
 */
@Tag(name = "Lab 8", description = "Repository experiments: queries, N+1, Page vs Slice, scrolling")
@RestController
@RequestMapping("/api/lab8")
public class Lab8Controller {

    private final QueryLab lab;

    public Lab8Controller(QueryLab lab) {
        this.lab = lab;
    }

    /** POST, not GET: the bulk update among these queries changes data (archives books before 2000). */
    @Operation(summary = "Task 3.3: derived queries, the JPQL query and the bulk update")
    @PostMapping("/queries")
    public List<QueryLab.Measurement> queries() {
        return lab.queries();
    }

    @Operation(summary = "Task 3.4: statements for N+1, then for the fetch join and the entity graph")
    @GetMapping("/n-plus-one")
    public List<QueryLab.Measurement> nPlusOne() {
        return lab.nPlusOne();
    }

    @Operation(summary = "Tasks 4.2–4.3: one request as a Page (select + count) and as a Slice (select only)")
    @GetMapping("/paging")
    public List<QueryLab.Measurement> paging(
            @ParameterObject @PageableDefault(size = 3, sort = "title") Pageable pageable) {
        return lab.paging(pageable);
    }

    @Operation(summary = "Variant 7: keyset scrolling (Window + ScrollPosition) compared with offset scrolling")
    @GetMapping("/scroll")
    public List<QueryLab.Measurement> scroll() {
        return lab.scrolling();
    }
}
