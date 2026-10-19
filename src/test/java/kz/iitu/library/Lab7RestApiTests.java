package kz.iitu.library;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Lab 7: the REST contract — DTOs, status codes, problem details, HATEOAS (variant 7), OpenAPI. */
@SpringBootTest
@AutoConfigureMockMvc
class Lab7RestApiTests {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired MockMvc mvc;

    /** Every test works on its own book, so the shared in-memory catalogue never causes a 409. */
    private static String body(String title) {
        return """
                {"title":"%s","author":"Lab Seven","year":2024,"genre":"SCIENCE",
                 "available":true,"publishedOn":"2024-05-01"}""".formatted(title);
    }

    private static String uniqueTitle() {
        return "Lab7 book " + SEQ.incrementAndGet();
    }

    private ResultActions postBook(String json) throws Exception {
        return mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String createBook(String title) throws Exception {
        return postBook(body(title)).andReturn().getResponse().getHeader("Location");
    }

    @Test
    @DisplayName("POST answers 201 with a Location header and a response DTO with server-owned fields")
    void createReturns201LocationAndResponseDto() throws Exception {
        String title = uniqueTitle();
        String location = postBook(body(title))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/books/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getHeader("Location");

        // the Location really names the new resource
        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title));
    }

    @Test
    @DisplayName("a client cannot choose the id or createdAt: they are not part of BookRequest")
    void clientCannotSetServerOwnedFields() throws Exception {
        String json = """
                {"id":777,"createdAt":"2000-01-01T00:00:00Z","title":"%s","author":"Lab Seven",
                 "year":2024,"genre":"SCIENCE"}""".formatted(uniqueTitle());
        postBook(json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(777)))
                .andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.not("2000-01-01T00:00:00Z")));
    }

    @Test
    @DisplayName("a duplicate title by the same author answers 409 with a problem detail")
    void duplicateReturns409ProblemDetail() throws Exception {
        String title = uniqueTitle();
        postBook(body(title)).andExpect(status().isCreated());

        postBook(body(title.toUpperCase()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Duplicate title"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.bookTitle").value(title.toUpperCase()))
                .andExpect(jsonPath("$.type").value(endsWith("/duplicate-title")));
    }

    @Test
    @DisplayName("a missing book is 404 with all five RFC 9457 fields and no stack trace")
    void missingBookIs404WithFiveFields() throws Exception {
        mvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(endsWith("/book-not-found")))
                .andExpect(jsonPath("$.title").value("Book not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Book with id 999 does not exist"))
                .andExpect(jsonPath("$.instance").value("/api/books/999"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("DELETE answers 204, the same DELETE again answers 404 with a problem detail")
    void secondDeleteIs404() throws Exception {
        String location = createBook(uniqueTitle());
        mvc.perform(delete(location)).andExpect(status().isNoContent());
        mvc.perform(delete(location))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("an invalid body is 400 from ResponseEntityExceptionHandler, with the rejected fields")
    void invalidBodyIs400FromBaseClass() throws Exception {
        postBook("{\"title\":\"\",\"author\":\"A1\",\"year\":1200}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid request content."))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.author").exists())
                .andExpect(jsonPath("$.errors.genre").exists());
    }

    @Test
    @DisplayName("an unparsable body is 400 too, but produced for a different reason")
    void unparsableBodyIs400() throws Exception {
        postBook("{not json")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("PUT replaces the client fields, keeps createdAt, and also refuses a duplicate")
    void putKeepsCreatedAtAndRejectsDuplicate() throws Exception {
        String first = uniqueTitle();
        String location = createBook(first);
        String createdAt = com.jayway.jsonpath.JsonPath.read(
                mvc.perform(get(location)).andReturn().getResponse().getContentAsString(), "$.createdAt");

        String renamed = uniqueTitle();
        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content(body(renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(renamed))
                .andExpect(jsonPath("$.createdAt").value(createdAt));

        String other = uniqueTitle();
        createBook(other);
        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content(body(other)))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/books/999").contentType(MediaType.APPLICATION_JSON).content(body(uniqueTitle())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("variant 7: GET of one book is a HAL EntityModel with self, books and citation links")
    void getOneIsHateoasEntityModel() throws Exception {
        mvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.title").value("Effective Java"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/api/books/1")))
                .andExpect(jsonPath("$._links.books.href").value(endsWith("/api/books")))
                .andExpect(jsonPath("$._links.citation.href").value(endsWith("/api/books/1/citation")));
    }

    @Test
    @DisplayName("the collection is a JSON array of response DTOs")
    void collectionIsArrayOfDtos() throws Exception {
        mvc.perform(get("/api/books").param("author", "Bloch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Effective Java"))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("the OpenAPI document describes the paths and takes maxLength from @Size")
    void openApiDocument() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Library Portal API"))
                .andExpect(jsonPath("$.paths['/api/books'].post").exists())
                .andExpect(jsonPath("$.paths['/api/books/{id}'].get.summary").value("Find a book by its identifier"))
                .andExpect(jsonPath("$.paths['/api/books/{id}'].delete").exists())
                .andExpect(jsonPath("$.components.schemas.BookRequest.properties.title.maxLength").value(120))
                .andExpect(jsonPath("$.tags[0].name").value("Books"))
                // the HTML pages are not part of the API description
                .andExpect(jsonPath("$.paths['/books']").doesNotExist());
    }

    @Test
    @DisplayName("Swagger UI is served at /swagger-ui.html")
    void swaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("the HTML form shows the same duplicate rule as a field error, not a 500")
    void formShowsDuplicateAsFieldError() throws Exception {
        mvc.perform(post("/books")
                        .param("title", "Effective Java").param("author", "Joshua Bloch")
                        .param("year", "2018").param("genre", "PROGRAMMING")
                        .param("publishedOn", "2018-01-06"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrorCode("form", "title", "book.duplicate"));
    }
}
