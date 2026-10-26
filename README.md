# Library Portal — final educational project

**Course:** RWPSF 3305 — Web Application Development with Spring Framework (IITU)
**Student:** Bektursyn Kuat, group IT1-2409IS
**Co-authors of individual labs:** Bekbaev Gibrat (labs 3, 4, 5)

One Spring Boot application — a library catalogue — that combines the topics and
individual variants of laboratory works 1–8. Every lab is not a separate demo, but a layer of
the same system: the REST API from labs 1, 5 and 7, the IoC beans from lab 2, the configuration
from lab 3, the aspects from lab 4 and the forms from lab 6 all work on the same `BookService`,
which since lab 8 stores the catalogue in PostgreSQL through Spring Data JPA.

Stack: **Java 25, Spring Boot 4.1.1** (Spring Framework 7), Spring MVC, Thymeleaf,
Hibernate Validator, Spring AOP (AspectJ annotations), Actuator, Spring HATEOAS,
springdoc-openapi 3 (Swagger UI), Spring Data JPA + Hibernate 7, PostgreSQL 16, Maven 3.9.

## Run

Since lab 8 the application needs PostgreSQL 16+ with a database and a user `springlab / springlab`
(tests use a second database, `springlab_test`, owned by the same user):

```bash
psql -U postgres -c "create user springlab with password 'springlab';"
psql -U postgres -c "create database springlab owner springlab;"
psql -U postgres -c "create database springlab_test owner springlab;"
# or: docker run --name pg-springlab -e POSTGRES_DB=springlab -e POSTGRES_USER=springlab -e POSTGRES_PASSWORD=springlab -p 5432:5432 -d postgres:16
```

```bash
./mvnw spring-boot:run                                   # http://localhost:8080
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev    # http://localhost:8081
./mvnw test                                              # 41 integration tests against PostgreSQL
```

The start page `http://localhost:8080/` lists links to every demonstration endpoint;
Swagger UI is at `http://localhost:8080/swagger-ui.html`, and `src/test/http/books.http`
holds the Lab 7 demonstration requests for the IntelliJ HTTP client.

## What each lab contributes

| Lab | Topic | Where in the project | Individual variant |
|-----|-------|----------------------|--------------------|
| 1 | Spring Boot, Initializr, first REST controller | `pom.xml`, `web/HelloController` → `/api/hello`, `/api/info` | **V7** `/api/wordcount` — words, characters, longest word |
| 2 | IoC container, DI, ambiguity, scopes, lifecycle | `notify/*`, `config/FormatConfig`, `lifecycle/LifecycleDemo`, `scope/*`, `config/ContainerReport` → `/api/system/notify`, `/lifecycle`, `/scopes` | **V8** `HtmlNotifier` — `<p>` + HTML escaping, `/api/system/notify/html` |
| 3 | YAML, profiles, `@ConfigurationProperties`, precedence, Actuator | `application*.yml`, `config/AppProperties`, `config/EnvironmentBanner`, `web/ConfigController` → `/api/config`, `/actuator/env` | **V7** `app.cache` (ttl, max-entries) — **really used** by `service/CitationCache`, `/api/system/cache` |
| 4 | Spring AOP | `aspect/*`, `audit/Audited`, `audit/AuditLog` — advise every public method of `kz.iitu.library.service..*` | **V8** `CallTracingAspect` — indentation by nesting depth |
| 5 | Spring MVC: REST + MVC controllers, status codes, static resources | `domain/Book`, `repository/BookRepository`, `service/BookService`, `api/BookRestController`, `web/BookPageController`, `templates/books/list.html`, `static/` | **V6** `GET /api/books/{id}/citation`, `text/plain` |
| 6 | Forms, Bean Validation, i18n, `@ControllerAdvice` | `web/BookForm`, `validation/NoDigits`, `config/LocaleConfiguration`, `messages*.properties`, `web/PageExceptionHandler`, `api/ApiExceptionHandler` | **V7** `publishedOn` — `LocalDate`, `@PastOrPresent`, locale-dependent `@DateTimeFormat` |
| 7 | REST API: DTOs and a mapper, status codes, ProblemDetail, OpenAPI | `api/dto/{BookRequest,BookResponse,BookMapper}`, `api/BookRestController`, `api/ApiExceptionHandler` (extends `ResponseEntityExceptionHandler`), `service/DuplicateTitleException`, `config/OpenApiConfig` → `/v3/api-docs`, `/swagger-ui.html` | **V7** HATEOAS — `GET /api/books/{id}` is an `EntityModel` with `self`, `books` and `citation` links |
| 8 | Data access layer: PostgreSQL, entities, `@ManyToOne`/`@OneToMany`, `@ManyToMany`, derived and JPQL queries, bulk update, N+1, paging | `domain/{Book,Author,Tag}`, `repository/*Repository`, `persistence/{DataSeeder,QueryLab,SqlRecorder}`, `api/Lab8Controller` → `/api/lab8/*`, paged `GET /api/books`; report: `docs/lab08/` | **V7** Window + keyset scrolling — `GET /api/books/scroll`, compared with offset paging in `/api/lab8/scroll` |

### Lab 7: the contract of `/api/books`

| Method and URI | Request body | Success | Errors (all `application/problem+json`) |
|----------------|--------------|---------|------------------------------------------|
| `GET /api/books?genre=&author=&page=&size=&sort=` (lab 8) | — | 200, page: `content` + `page{size,number,totalElements,totalPages}` | 400 for an unknown sort property |
| `GET /api/books/{id}` | — | 200, HAL `BookResponse` + `_links` | 404 |
| `POST /api/books` | `BookRequest` | 201 + `Location`, `BookResponse` | 400 (invalid / unparsable), 409 (same title and author) |
| `PUT /api/books/{id}` | `BookRequest` | 200, `BookResponse` (`createdAt` kept) | 400, 404, 409 |
| `DELETE /api/books/{id}` | — | 204 | 404 — also on a repeated DELETE |

`BookRequest` has no `id` and no `createdAt`: those belong to the server. A repeated DELETE answers
404 on purpose: the state of the server does not change (DELETE stays idempotent), and the client
learns there was nothing to delete. Under the `prod` profile `/v3/api-docs` and Swagger UI are switched off.

### How the labs are connected (not just placed side by side)

- **Lab 2 → Lab 5.** `BookService.create` announces every new book through `NotificationService`
  (the `@Primary` `EmailNotifier`).
- **Lab 3 → Lab 2.** `EmailNotifier` reads `app.mail.*`: under `dev` mail is disabled, under `prod` retries = 5.
- **Lab 3 → Lab 5.** Citations (lab 5, variant 6) are cached by `CitationCache`, sized by `app.cache` (lab 3, variant 7):
  1 minute / 100 entries by default, 30 s / 50 entries under `dev`.
- **Lab 4 → everything.** Every request to the catalogue passes through Audit → Logging → Timing → Tracing aspects.
  `BookService → CitationCache` calls show a real nested trace (variant 8).
- **Lab 6 → Lab 7.** The REST API validates its JSON body (`BookRequest`) with the same constraints and the same
  bundles as the HTML form.
- **Lab 7 → Lab 6.** The 409 duplicate rule lives in the service, so the HTML form shows it as a field error.
- **Lab 3 → Lab 7.** The `prod` profile closes the OpenAPI document and Swagger UI.
- **Lab 4 + Lab 6.** `delete(0)` throws `IllegalArgumentException`: `@AfterThrowing` logs it, `AuditAspect` records the
  failure and rethrows, `ApiExceptionHandler` turns it into **400** (lab 4 returned 500).

## Endpoint map

| Request | Lab | Result |
|---------|-----|--------|
| `GET /api/hello?name=IITU`, `GET /api/info` | 1 | JSON greeting / JVM info |
| `GET /api/wordcount?text=...` | 1 (V7) | `{"wordCount":5,"characterCount":31,"longestWord":"interesting"}` |
| `GET /api/system/notify?text=IITU` | 2 | primary / qualifier / list (by `@Order`) / map keys |
| `GET /api/system/notify/html?text=Hello <world>` | 2 (V8) | `<p>Hello &lt;world&gt;</p>` |
| `GET /api/system/lifecycle`, `GET /api/system/scopes` | 2 | lifecycle order, singleton vs prototype |
| `GET /api/config` | 3 | resolved `app.*`, port, profiles, banner |
| `GET /actuator/env/app.mail.retry-count` | 3 | value **and source** of a property |
| `GET /api/system/cache` | 3 (V7) | ttl, max entries, hits, misses |
| `GET /api/system/proxy` | 4 | `BookService$$SpringCGLIB$$…`, `isCglib=true` |
| `GET /api/books/stats` | 4 | ~300 ms → `[TIME] SLOW` warning |
| `DELETE /api/books/bulk?ids=1,2&viaProxy=false` | 4 | self-invocation: **0** BOOK_DELETE audit records |
| `DELETE /api/books/bulk?ids=1,2&viaProxy=true` | 4 | via `self` proxy: **2** audit records |
| `GET /api/system/audit` | 4 | last audit records |
| `GET/POST /api/books`, `GET/PUT/DELETE /api/books/{id}` | 5, 7 | 200, 201 + Location, 204, 404, 409 |
| `GET /api/books/{id}/citation` | 5 (V6) | `text/plain`; `Accept: application/json` → 406 |
| `GET /api/books/abc` · `/api/nothing` · `DELETE /api/books` · `text/plain` body · `Accept: xml` | 5 | 400 · 404 · 405 · 415 · 406 |
| `GET /books`, `/books/{id}`, `/books/new` (`?lang=ru`) | 5, 6 | Thymeleaf pages, form, PRG + flash |
| `GET /books/999` vs `GET /api/books/999` | 6, 7 | HTML 404 page vs `application/problem+json` |
| `GET /api/books/1` | 7 (V7) | HAL: fields + `_links.self`, `_links.books`, `_links.citation` |
| `GET /v3/api-docs`, `GET /swagger-ui.html` | 7 | OpenAPI document, Swagger UI |

## Project structure

```
src/main/java/kz/iitu/library/
├── LibraryPortalApplication.java   @SpringBootApplication, @ConfigurationPropertiesScan
├── api/          BookRestController, SystemController, ApiExceptionHandler
│   └── dto/      BookRequest, BookResponse, BookMapper            (lab 7)
├── aspect/       Pointcuts, AuditAspect(1), LoggingAspect(2), TimingAspect(3), CallTracingAspect
├── audit/        @Audited, AuditLog
├── config/       AppProperties, EnvironmentBanner(+dev/prod/default), FormatConfig,
│                 LocaleConfiguration, ContainerReport, OpenApiConfig
├── domain/       Book (record), Genre (enum)
├── lifecycle/    LifecycleDemo
├── notify/       Notifier, Console/Email/Noop/Html notifiers, NotificationService
├── repository/   BookRepository (in-memory)
├── scope/        Ticket (prototype), TicketOffice
├── service/      BookService, CitationCache, BookNotFoundException, DuplicateTitleException
├── validation/   @NoDigits, NoDigitsValidator
└── web/          HelloController, ConfigController, BookPageController, BookForm, PageExceptionHandler
src/main/resources/
├── application.yml, application-dev.yml, application-prod.yml
├── messages.properties, messages_ru.properties
├── static/       index.html, css/app.css, img/bookshelf.svg
└── templates/    books/{list,form,details}.html, error/not-found.html, fragments/layout.html
src/test/
├── java/kz/iitu/library/   LibraryPortalApplicationTests, Lab7RestApiTests, DevProfileTests, ProdProfileTests
└── http/books.http         Lab 7 demonstration requests
```

## Defence materials

- [`docs/Defense_Presentation.pptx`](docs/Defense_Presentation.pptx) — the presentation (English, labs 1–7, speaker notes included).
- [`docs/Defense_Presentation_Formal.pptx`](docs/Defense_Presentation_Formal.pptx) — the same content in a formal navy-and-gold style, with screenshots of the running application (18 slides).
- [`docs/Defense_Presentation_RU.pptx`](docs/Defense_Presentation_RU.pptx) — the earlier Russian version (labs 1–6).
- `docs/build_presentation.py` / `docs/build_presentation_ru.py` regenerate the decks (`pip install python-pptx`).
- [`docs/DEFENSE.md`](docs/DEFENSE.md) — what was done in each lab, the demo script and answers to the defence questions.
- [`docs/DEFENSE_GUIDE.md`](docs/DEFENSE_GUIDE.md) — step-by-step defence script (in Russian): what to open, what to show, what to say.
- [`docs/DEFENSE_SITE_TESTS.md`](docs/DEFENSE_SITE_TESTS.md) — the same without showing code: presentation text in Russian, tests, and the site/Swagger UI walk-through.
