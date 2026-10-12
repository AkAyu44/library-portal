# Защита итогового проекта «Library Portal»

**Дисциплина:** RWPSF 3305 — Web Application Development with Spring Framework
**Студент:** Бектурсын Куат, группа IT1-2409IS
**Соавтор лабораторных 3–5:** Бекбаев Гибрат

## 1. Идея проекта

Шесть лабораторных были шестью отдельными шагами одного и того же приложения `spring-lab-01`.
В итоговом проекте они собраны в **одно** приложение — каталог библиотеки, — где каждая лабораторная
является слоем системы, а не отдельной демонстрацией:

```
Браузер / curl
   │
   ▼
DispatcherServlet ── LocaleChangeInterceptor (лаб. 6)
   │
   ├── @RestController  BookRestController, HelloController, ConfigController, SystemController (лаб. 1, 3, 5)
   │        └── ошибки → ApiExceptionHandler → ProblemDetail (лаб. 6)
   └── @Controller      BookPageController → Thymeleaf (лаб. 5, 6)
            └── ошибки → PageExceptionHandler → error/not-found.html (лаб. 6)
   │
   ▼
CGLIB-прокси BookService  ── AuditAspect(1) → LoggingAspect(2) → TimingAspect(3) → CallTracingAspect (лаб. 4)
   │
   ▼
BookService ──► BookRepository (in-memory, лаб. 5)
            ──► NotificationService ──► @Primary EmailNotifier (лаб. 2) ── app.mail.* (лаб. 3)
            ──► CitationCache ── app.cache.ttl / max-entries (лаб. 3, вариант 7)
```

## 2. Что сделано по каждой лабораторной

### Лаб. 1 — Spring Boot, первое REST-приложение (вариант 7)
- Проект на Spring Boot 4.1.1 / Java 25, `spring-boot-starter-parent`, стартер `webmvc`, DevTools, Maven Wrapper.
- `HelloController`: `@RestController`, `@RequestMapping("/api")`, `@GetMapping`, `@RequestParam(defaultValue=...)`,
  `@Value("${app.owner:unknown}")`.
- **Вариант 7:** `GET /api/wordcount?text=...` → количество слов, символов и самое длинное слово;
  без параметра возвращаются нули (не ошибка).
- Тест: `lab1_helloAndWordCount`.

### Лаб. 2 — IoC-контейнер (вариант 8)
- Интерфейс `Notifier` и 4 бина: `console` (`@Order(1)`), `email` (`@Primary`, `@Order(2)`),
  `html` (`@Order(3)`), `noop` (`@Fallback`, `@Order(99)`).
- `NotificationService` получает их всеми способами: `@Primary`, `@Qualifier`, `List<Notifier>`, `Map<String, Notifier>`.
- `@Bean`-метод `reportDateFormatter` для `DateTimeFormatter` (класс JDK нельзя пометить `@Component`).
- `LifecycleDemo`: конструктор → `@PostConstruct` → `@PreDestroy`.
- `Ticket` (`prototype`) внутри синглтона `TicketOffice`: прямое внедрение vs `ObjectProvider`.
- `ContainerReport` (`CommandLineRunner`) печатает число бинов и тип `bookService` (видно, что это CGLIB-прокси).
- **Вариант 8:** `HtmlNotifier` оборачивает текст в `<p>` и экранирует `<`/`>`.
- **Связь с проектом:** при добавлении книги `BookService` отправляет уведомление через `@Primary`-бин.
- Тесты: `lab2_primaryQualifierListAndMap`, `lab2_prototypeViaProviderDiffers`.

### Лаб. 3 — Конфигурация (вариант 7)
- `application.yml` + `application-dev.yml` (порт 8081, mail выключен, кэш 30 с / 50) + `application-prod.yml`.
- `AppProperties` — `record` с `@ConfigurationProperties(prefix="app")` и `@Validated`; вложенные `Mail` и `Cache`.
  Значение `app.mail.retry-count=99` останавливает запуск (нарушен `@Max(10)`).
- `EnvironmentBanner` с тремя реализациями `@Profile("dev")`, `@Profile("prod")`, `@Profile("!dev & !prod")`.
- `GET /api/config` показывает итоговую конфигурацию; Actuator: `/actuator/env/app.mail.retry-count` показывает
  значение **и источник** свойства, `--debug` — отчёт условий автоконфигурации.
- Приоритет источников: командная строка > системное свойство JVM > переменная окружения > профильный yml > application.yml.
- **Вариант 7:** `app.cache.ttl` (`Duration`) и `app.cache.max-entries` (`@Min(10) @Max(10000)`).
  В проекте это уже не просто поле в JSON: `CitationCache` реально кэширует цитаты по этим параметрам
  (`GET /api/system/cache` показывает hits/misses).
- Тесты: `lab3_typedConfigurationWithoutProfile`, `DevProfileTests`.

### Лаб. 4 — Spring AOP (вариант 8)
- Библиотека именованных pointcut-ов `Pointcuts`: `within(kz.iitu.library.service..*) && execution(public * *(..))`.
- `AuditAspect` (`@Order(1)`, `@Around("@annotation(audited)")`) + своя аннотация `@Audited(action, logArguments)`
  с `RetentionPolicy.RUNTIME`; записи сохраняются в `AuditLog` (`GET /api/system/audit`).
- `LoggingAspect` (`@Order(2)`): `@Before`, `@AfterReturning(returning="result")`, `@AfterThrowing(throwing="ex")`.
- `TimingAspect` (`@Order(3)`): `@Around`, `pjp.proceed()`, предупреждение `SLOW` > 200 мс (`GET /api/books/stats`, ~300 мс).
- Прокси: `GET /api/system/proxy` → `BookService$$SpringCGLIB$$…`, `isCglib=true`.
- Самовызов: `DELETE /api/books/bulk?ids=..&viaProxy=false` вызывает `this.delete()` → 0 записей аудита;
  `viaProxy=true` вызывает `self.delete()` через прокси → по записи на каждый id.
  `self` внедрён с `@Lazy`, чтобы не получить ошибку циклической ссылки.
- **Вариант 8:** `CallTracingAspect` (`@Before`/`@After`, `ThreadLocal` глубины): вызов
  `BookService.citation → CitationCache.get` даёт реальный вложенный трейс с отступами.
- Тесты: `lab4_serviceIsCglibProxy`, `lab4_selfInvocationBypassesProxyUnlessSelfIsUsed`, `lab4_auditRecordsFailureAndRethrows`.

### Лаб. 5 — Spring MVC (вариант 6, выполнял Бекбаев Гибрат)
- Доменный `record Book`, `BookRepository` (`ConcurrentHashMap` + `AtomicLong`), `BookService`.
- `BookRestController` (`/api/books`): `@RequestParam` (author, limit), `@PathVariable`, `@RequestBody`;
  `ResponseEntity`: 201 + `Location`, 204, 404.
- `BookPageController` (`/books`) — `@Controller`, `Model`, Thymeleaf `books/list.html`, фильтр по автору.
- Статические ресурсы: `static/index.html` (стартовая страница), `static/css/app.css` — без контроллера.
- Post/Redirect/Get + flash-атрибут: после F5 нет дубля и сообщение исчезает.
- Коды ошибок сопоставления: 400 (`/api/books/abc`), 404 (`/api/nothing`), 405 (`DELETE /api/books`),
  415 (тело `text/plain`), 406 (`Accept: application/xml`).
- **Вариант 6:** `GET /api/books/{id}/citation`, `produces="text/plain"` → «Joshua Bloch (2018). Effective Java.»
- Тесты: `lab5_crudStatusCodes`, `lab5_mappingFailures`, `lab5_citationIsPlainText`, `lab5_pageAndStaticResources`.

### Лаб. 6 — Формы и валидация (вариант 7)
- `BookForm` — отдельный изменяемый класс (не сущность): нет поля `id` → защита от mass assignment.
- `th:object`, `th:field`, `th:errors`, `th:errorclass`, `#fields.allErrors()`; `@Valid` + `BindingResult` сразу после формы.
- Два языка: `messages.properties` (обязателен) и `messages_ru.properties`; `?lang=ru` → `LocaleChangeInterceptor` +
  `SessionLocaleResolver`; `spring.messages.fallback-to-system-locale=false`.
- Своё ограничение `@NoDigits` + `NoDigitsValidator` (`null` считается валидным).
- `BookNotFoundException` → `PageExceptionHandler` (HTML-страница 404) и `ApiExceptionHandler`
  (`ProblemDetail`, `application/problem+json`, `@Order(HIGHEST_PRECEDENCE)`).
- **Вариант 7:** `LocalDate publishedOn` с `@PastOrPresent` и `@DateTimeFormat(style="S-", fallbackPatterns="yyyy-MM-dd")`:
  en — `10/12/26`, ru — `12.10.2026`.
- **Развитие в проекте:** те же ограничения и сообщения применяются к JSON API (`BookRequest`) → 400 с картой ошибок.
- Тесты: `lab6_invalidFormIsRedisplayedWithErrors`, `lab6_validFormRedirectsWithFlash`,
  `lab6_russianMessagesAndLocaleDates`, `lab6_notFoundIsHtmlForPagesAndProblemDetailForApi`, `lab6_apiValidationReturnsProblemDetail`.

## 3. Сценарий демонстрации (≈5 минут)

1. `./mvnw test` — 17 тестов зелёные (каждый тест = эксперимент из отчёта).
2. `./mvnw spring-boot:run`, открыть `http://localhost:8080/` — карточки по лабораторным.
3. `/books` → «Новая книга» → отправить пустую форму на `?lang=ru` → все ошибки на русском.
4. Заполнить форму корректно → redirect + flash → F5 (дубля нет). В консоли видно `[AUDIT] start BOOK_CREATE` и
   `EMAIL ... New book in the catalogue`.
5. Открыть карточку книги → в консоли вложенный `[TRACE]` `citation → CitationCache.get`.
6. `curl -X DELETE "localhost:8080/api/books/bulk?ids=1,2&viaProxy=false"` и `viaProxy=true` → `/api/system/audit`.
7. `curl -i localhost:8080/api/books/999` (ProblemDetail) и `/books/999` в браузере (HTML).
8. Перезапустить с `-Dspring-boot.run.profiles=dev` → `/api/config` (порт 8081, ttl PT30S, banner DEVELOPMENT).

## 4. Вопросы на защите — краткие ответы

| Вопрос | Ответ |
|--------|-------|
| Что такое бин и как его объявить? | Объект, которым управляет контейнер. `@Component`/`@Service`/... или `@Bean`-метод в `@Configuration`. |
| Почему внедрение через конструктор? | `final`-поля, объект всегда полностью собран, легко тестировать через `new`. |
| Как решить `NoUniqueBeanDefinitionException`? | `@Primary`, `@Qualifier`, или принять все через `List`/`Map`. |
| Singleton vs prototype? Как получить новый prototype в синглтоне? | Один экземпляр на контекст vs новый на каждый запрос. Через `ObjectProvider.getObject()`. |
| Почему `@PreDestroy` не вызывается при kill? | Он выполняется только при корректном закрытии контекста. |
| Зачем `@ConfigurationProperties` вместо `@Value`? | Типы, группировка, валидация при старте, автодополнение. |
| Что приоритетнее: профиль или аргумент командной строки? | Аргумент командной строки — профильный файл тоже лишь `PropertySource`. |
| Pointcut vs advice? | Pointcut — ГДЕ, advice — ЧТО ДЕЛАТЬ. |
| Порядок аспектов? | Меньший `@Order` — внешний: вход Audit → Log → Time, выход в обратном порядке. |
| Что будет без `proceed()`? | Целевой метод не выполнится. |
| Почему `@AfterReturning` не сработал при `delete(0)`? | Метод бросил исключение → сработал `@AfterThrowing`. |
| Почему самовызов не перехватывается? | `this.delete()` не проходит через прокси. Исправление — `self`-ссылка на прокси. |
| Почему CGLIB? | `BookService` не реализует интерфейс → прокси-подкласс. Private/static методы не перехватываются. |
| `@RestController` vs `@Controller`? | Первый пишет результат в тело ответа (JSON), второй возвращает имя представления. |
| Что означают 400/404/405/415/406? | Неверное преобразование аргумента / нет пути / нет метода / тип тела / тип ответа. |
| Зачем PRG? | После F5 повторяется только GET — нет повторной отправки формы. |
| Почему форма — отдельный класс? | Защита от mass assignment, частичное заполнение, сохранение ввода. |
| Что если `BindingResult` не сразу после формы? | Spring бросит `MethodArgumentNotValidException` → 400 вместо формы с ошибками. |
| `@NotNull` vs `@NotEmpty` vs `@NotBlank`? | не null / не null и длина > 0 / хотя бы один непробельный символ. |
| Как скоупятся два advice? | По аннотации `@Controller`/`@RestController`; так как `@RestController` содержит `@Controller`, API-advice получает `HIGHEST_PRECEDENCE`. |
| Почему `th:utext` опасен? | Не экранирует HTML → XSS. |
