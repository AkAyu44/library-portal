# Library Portal — final educational project

**Course:** RWPSF 3305 — Web Application Development with Spring Framework (IITU)
**Student:** Bektursyn Kuat, group IT1-2409IS
**Co-authors of individual labs:** Bekbaev Gibrat (labs 3, 4, 5)

One Spring Boot application — a library catalogue — that combines the topics and
individual variants of laboratory works 1–6. Every lab is not a separate demo, but a layer of
the same system: the REST API from lab 1 and 5, the IoC beans from lab 2, the configuration
from lab 3, the aspects from lab 4 and the forms from lab 6 all work on the same `BookService`.

Stack: **Java 25, Spring Boot 4.1.1** (Spring Framework 7), Spring MVC, Thymeleaf,
Hibernate Validator, Spring AOP (AspectJ annotations), Actuator, Maven 3.9.

## Run

```bash
./mvnw spring-boot:run                                   # http://localhost:8080
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev    # http://localhost:8081
./mvnw test                                              # 17 integration tests, one per lab experiment
```

The start page `http://localhost:8080/` lists links to every demonstration endpoint.

## What each lab contributes

| Lab | Topic | Where in the project | Individual variant |
|-----|-------|----------------------|--------------------|
| 1 | Spring Boot, Initializr, first REST controller | `pom.xml`, `web/HelloController` → `/api/hello`, `/api/info` | **V7** `/api/wordcount` — words, characters, longest word |
| 2 | IoC container, DI, ambiguity, scopes, lifecycle | `notify/*`, `config/FormatConfig`, `lifecycle/LifecycleDemo`, `scope/*`, `config/ContainerReport` → `/api/system/notify`, `/lifecycle`, `/scopes` | **V8** `HtmlNotifier` — `<p>` + HTML escaping, `/api/system/notify/html` |
| 3 | YAML, profiles, `@ConfigurationProperties`, precedence, Actuator | `application*.yml`, `config/AppProperties`, `config/EnvironmentBanner`, `web/ConfigController` → `/api/config`, `/actuator/env` | **V7** `app.cache` (ttl, max-entries) — **really used** by `service/CitationCache`, `/api/system/cache` |
| 4 | Spring AOP | `aspect/*`, `audit/Audited`, `audit/AuditLog` — advise every public method of `kz.iitu.library.service..*` | **V8** `CallTracingAspect` — indentation by nesting depth |
| 5 | Spring MVC: REST + MVC controllers, status codes, static resources | `domain/Book`, `repository/BookRepository`, `service/BookService`, `api/BookRestController`, `web/BookPageController`, `templates/books/list.html`, `static/` | **V6** `GET /api/books/{id}/citation`, `text/plain` |
| 6 | Forms, Bean Validation, i18n, `@ControllerAdvice` | `web/BookForm`, `validation/NoDigits`, `config/LocaleConfiguration`, `messages*.properties`, `web/PageExceptionHandler`, `api/ApiExceptionHandler` | **V7** `publishedOn` — `LocalDate`, `@PastOrPresent`, locale-dependent `@DateTimeFormat` |

### How the labs are connected (not just placed side by side)

- **Lab 2 → Lab 5.** `BookService.create` announces every new book through `NotificationService`
  (the `@Primary` `EmailNotifier`).
- **Lab 3 → Lab 2.** `EmailNotifier` reads `app.mail.*`: under `dev` mail is disabled, under `prod` retries = 5.
- **Lab 3 → Lab 5.** Citations (lab 5, variant 6) are cached by `CitationCache`, sized by `app.cache` (lab 3, variant 7):
  1 minute / 100 entries by default, 30 s / 50 entries under `dev`.
- **Lab 4 → everything.** Every request to the catalogue passes through Audit → Logging → Timing → Tracing aspects.
  `BookService → CitationCache` calls show a real nested trace (variant 8).
- **Lab 6 → Lab 5.** The REST API validates its JSON body (`BookRequest`) with the same constraints and the same
  bundles as the HTML form; errors come back as RFC 9457 `ProblemDetail`.
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
| `GET/POST /api/books`, `GET/PUT/DELETE /api/books/{id}` | 5 | 200, 201 + Location, 204, 404 |
| `GET /api/books/{id}/citation` | 5 (V6) | `text/plain`; `Accept: application/json` → 406 |
| `GET /api/books/abc` · `/api/nothing` · `DELETE /api/books` · `text/plain` body · `Accept: xml` | 5 | 400 · 404 · 405 · 415 · 406 |
| `GET /books`, `/books/{id}`, `/books/new` (`?lang=ru`) | 5, 6 | Thymeleaf pages, form, PRG + flash |
| `GET /books/999` vs `GET /api/books/999` | 6 | HTML 404 page vs `application/problem+json` |

## Project structure

```
src/main/java/kz/iitu/library/
├── LibraryPortalApplication.java   @SpringBootApplication, @ConfigurationPropertiesScan
├── api/          BookRestController, BookRequest, SystemController, ApiExceptionHandler
├── aspect/       Pointcuts, AuditAspect(1), LoggingAspect(2), TimingAspect(3), CallTracingAspect
├── audit/        @Audited, AuditLog
├── config/       AppProperties, EnvironmentBanner(+dev/prod/default), FormatConfig,
│                 LocaleConfiguration, ContainerReport
├── domain/       Book (record), Genre (enum)
├── lifecycle/    LifecycleDemo
├── notify/       Notifier, Console/Email/Noop/Html notifiers, NotificationService
├── repository/   BookRepository (in-memory)
├── scope/        Ticket (prototype), TicketOffice
├── service/      BookService, CitationCache, BookNotFoundException
├── validation/   @NoDigits, NoDigitsValidator
└── web/          HelloController, ConfigController, BookPageController, BookForm, PageExceptionHandler
src/main/resources/
├── application.yml, application-dev.yml, application-prod.yml
├── messages.properties, messages_ru.properties
├── static/       index.html, css/app.css
└── templates/    books/{list,form,details}.html, error/not-found.html, fragments/layout.html
```

## Defence materials

- [`docs/Defense_Presentation.pptx`](docs/Defense_Presentation.pptx) — the presentation.
- [`docs/DEFENSE.md`](docs/DEFENSE.md) — what was done in each lab, the demo script and answers to the defence questions.
