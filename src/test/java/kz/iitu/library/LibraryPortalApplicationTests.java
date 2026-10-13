package kz.iitu.library;

import kz.iitu.library.audit.AuditLog;
import kz.iitu.library.notify.NotificationService;
import kz.iitu.library.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** One integration test per lab: every experiment from the reports is reproduced automatically. */
@SpringBootTest
@AutoConfigureMockMvc
class LibraryPortalApplicationTests {

    private static final String NEW_BOOK = """
            {"title":"Pro Spring 6","author":"Iuliana Cosmina","year":2023,
             "genre":"PROGRAMMING","available":true,"publishedOn":"2023-03-01"}""";

    @Autowired MockMvc mvc;
    @Autowired BookService bookService;
    @Autowired NotificationService notifications;
    @Autowired AuditLog auditLog;

    // ---------------- Lab 1 ----------------
    @Test
    void lab1_helloAndWordCount() throws Exception {
        mvc.perform(get("/api/hello").param("name", "IITU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, IITU!"))
                .andExpect(jsonPath("$.owner").value("Bektursyn Kuat"));
        mvc.perform(get("/api/wordcount").param("text", "Spring Boot is very interesting"))
                .andExpect(jsonPath("$.wordCount").value(5))
                .andExpect(jsonPath("$.characterCount").value(31))
                .andExpect(jsonPath("$.longestWord").value("interesting"));
        mvc.perform(get("/api/wordcount"))
                .andExpect(jsonPath("$.wordCount").value(0));
    }

    // ---------------- Lab 2 ----------------
    @Test
    void lab2_primaryQualifierListAndMap() throws Exception {
        assertThat(notifications.viaPrimary("x")).startsWith("email");
        assertThat(notifications.order()).containsExactly("console", "email", "html", "noop");
        assertThat(notifications.names()).contains("console", "email", "html", "noop");
        mvc.perform(get("/api/system/notify/html").param("text", "Hello <world>"))
                .andExpect(content().string("<p>Hello &lt;world&gt;</p>"));
    }

    @Test
    void lab2_prototypeViaProviderDiffers() throws Exception {
        mvc.perform(get("/api/system/scopes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.injectedDirectly[0]").exists());
        mvc.perform(get("/api/system/lifecycle"))
                .andExpect(jsonPath("$[0]", containsString("Constructor")))
                .andExpect(jsonPath("$[1]", containsString("@PostConstruct")));
    }

    // ---------------- Lab 3 ----------------
    @Test
    void lab3_typedConfigurationWithoutProfile() throws Exception {
        mvc.perform(get("/api/config"))
                .andExpect(jsonPath("$.mailRetryCount").value(3))
                .andExpect(jsonPath("$.cacheTtl").value("PT1M"))
                .andExpect(jsonPath("$.cacheMaxEntries").value(100))
                .andExpect(jsonPath("$.banner").value("no profile is active"));
    }

    // ---------------- Lab 4 ----------------
    @Test
    void lab4_serviceIsCglibProxy() {
        assertThat(AopUtils.isAopProxy(bookService)).isTrue();
        assertThat(AopUtils.isCglibProxy(bookService)).isTrue();
        assertThat(bookService.getClass().getName()).contains("$$SpringCGLIB$$");
    }

    @Test
    void lab4_selfInvocationBypassesProxyUnlessSelfIsUsed() {
        long a = bookService.create(new kz.iitu.library.domain.Book(null, "A", "X", 2000, null, true, null)).id();
        long b = bookService.create(new kz.iitu.library.domain.Book(null, "B", "X", 2000, null, true, null)).id();
        long c = bookService.create(new kz.iitu.library.domain.Book(null, "C", "X", 2000, null, true, null)).id();
        long d = bookService.create(new kz.iitu.library.domain.Book(null, "D", "X", 2000, null, true, null)).id();

        auditLog.clear();
        bookService.deleteBulk(List.of(a, b), false);       // this.delete(...)
        assertThat(auditLog.count("BOOK_DELETE")).isZero();

        bookService.deleteBulk(List.of(c, d), true);        // self.delete(...)
        assertThat(auditLog.count("BOOK_DELETE")).isEqualTo(2);
    }

    @Test
    void lab4_auditRecordsFailureAndRethrows() throws Exception {
        auditLog.clear();
        mvc.perform(delete("/api/books/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid identifier: 0"));
        assertThat(auditLog.latest().getFirst().outcome()).isEqualTo("failure");
    }

    // ---------------- Lab 5 ----------------
    @Test
    void lab5_crudStatusCodes() throws Exception {
        mvc.perform(get("/api/books").param("author", "Bloch").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Effective Java"));

        String location = mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(NEW_BOOK))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(delete(location)).andExpect(status().isNoContent());
        mvc.perform(delete(location)).andExpect(status().isNotFound());
    }

    @Test
    void lab5_mappingFailures() throws Exception {
        mvc.perform(get("/api/books/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/nothing")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/books")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/books").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/books").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void lab5_citationIsPlainText() throws Exception {
        mvc.perform(get("/api/books/1/citation"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Joshua Bloch (2018). Effective Java."));
        mvc.perform(get("/api/books/1/citation").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void lab5_pageAndStaticResources() throws Exception {
        mvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                .andExpect(model().attributeExists("books"));
        mvc.perform(get("/css/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("../img/bookshelf.svg")));
        mvc.perform(get("/img/bookshelf.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"));
    }

    @Test
    void lab5_startPageKeepsEveryDemoLink() throws Exception {
        String html = mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // relative links: no hardcoded host, port or context path
        assertThat(html).contains(
                "href=\"css/app.css\"", "href=\"books\"", "href=\"books/new\"",
                "href=\"api/hello?name=IITU\"", "href=\"api/info\"",
                "href=\"api/wordcount?text=Spring%20Boot%20is%20very%20interesting\"",
                "href=\"api/system/notify?text=IITU\"",
                "href=\"api/system/notify/html?text=Hello%20%3Cworld%3E\"",
                "href=\"api/system/lifecycle\"", "href=\"api/system/scopes\"",
                "href=\"api/config\"", "href=\"api/system/cache\"",
                "href=\"actuator/env/app.mail.retry-count\"", "href=\"actuator/health\"",
                "href=\"api/system/proxy\"", "href=\"api/books/stats\"", "href=\"api/system/audit\"",
                "/api/books/bulk?ids=1,2&amp;viaProxy=false|true",
                "href=\"api/books\"", "href=\"api/books?author=Bloch&amp;limit=1\"",
                "href=\"api/books/1/citation\"",
                "href=\"books/new?lang=ru\"", "href=\"books/999\"", "href=\"api/books/999\"",
                "SLOW warning", "variant 6", "variant 7", "variant 8");
        assertThat(html).doesNotContain("localhost");
    }

    // ---------------- Lab 6 ----------------
    @Test
    void lab6_invalidFormIsRedisplayedWithErrors() throws Exception {
        mvc.perform(post("/books")
                        .param("title", "")
                        .param("author", "Bloch 2018")
                        .param("year", "1200")
                        .param("publishedOn", "2999-01-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "title", "author", "year", "genre", "publishedOn"))
                .andExpect(content().string(containsString("The author name must not contain digits")));
    }

    @Test
    void lab6_validFormRedirectsWithFlash() throws Exception {
        mvc.perform(post("/books")
                        .param("title", "Abai")
                        .param("author", "Mukhtar Auezov")
                        .param("year", "1942")
                        .param("genre", "NOVEL")
                        .param("publishedOn", "1942-01-01"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attribute("addedTitle", "Abai"));
    }

    @Test
    void lab6_russianMessagesAndLocaleDates() throws Exception {
        mvc.perform(post("/books").param("lang", "ru").param("title", "").param("year", "2000"))
                .andExpect(content().string(containsString("Название не может быть пустым")));
        mvc.perform(post("/books").param("lang", "ru")
                        .param("title", "T").param("author", "A").param("year", "2000")
                        .param("genre", "NOVEL").param("publishedOn", "09.05.2006"))
                .andExpect(status().isFound());
    }

    @Test
    void lab6_notFoundIsHtmlForPagesAndProblemDetailForApi() throws Exception {
        mvc.perform(get("/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"));
        mvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Book not found"))
                .andExpect(jsonPath("$.bookId").value(999));
    }

    @Test
    void lab6_apiValidationReturnsProblemDetail() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"author\":\"R2D2\",\"year\":1200}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("The title must not be empty"))
                .andExpect(jsonPath("$.errors.author").value("The author name must not contain digits"))
                .andExpect(jsonPath("$.errors.genre").exists());
    }
}
