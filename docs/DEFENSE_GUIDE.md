# Гайд защиты: что открыть, что показать, что сказать

Порядок: **подготовка → презентация → лаба 7 по чек-листу → обзор лаб 1–6 → вопросы**.
Лаба 7 идёт первой, пока в каталоге «чистые» данные (id новых книг совпадают с файлом запросов).
Время: презентация 2–3 мин, лаба 7 ~6 мин, лабы 1–6 ~6–8 мин.

Обозначения: 📂 — открыть в IntelliJ, 🌐 — открыть в браузере, ▶ — запустить, 💬 — что говорить.

---

## Этап 0. Подготовка (до того, как позвать учителя)

1. В IntelliJ открыт проект `C:\spring_labs\library-portal` (в заголовке окна — `library-portal`).
2. Остановить все старые запуски (■), запустить `LibraryPortalApplication` (▶).
   В консоли должно быть `Started LibraryPortalApplication`.
   **Важно:** перезапустить прямо перед защитой — так каталог чистый (4 книги, id 1–4).
3. Открыть вкладки IntelliJ заранее:
   - `src/test/http/books.http`
   - `src/main/java/kz/iitu/library/api/dto/BookRequest.java`, `BookResponse.java`, `BookMapper.java`
   - `src/main/java/kz/iitu/library/domain/Book.java`
   - `src/main/java/kz/iitu/library/api/BookRestController.java`
   - `src/main/java/kz/iitu/library/api/ApiExceptionHandler.java`
4. Браузер, вкладки: `http://localhost:8080/`, `http://localhost:8080/swagger-ui.html`, `http://localhost:8080/v3/api-docs`.
5. Открыть `docs/Defense_Presentation.pptx` в PowerPoint.
6. В IntelliJ внизу открыть вкладку **Terminal** (понадобится для curl).

---

## Этап 1. Презентация (слайды 1–3)

🖥 **Слайд 1 — титульный.**
💬 «Здравствуйте. Я представляю итоговый проект по курсу Web Application Development with Spring.
Это одно приложение — каталог библиотеки, — в котором собраны темы и индивидуальные варианты всех семи лабораторных работ».

🖥 **Слайд 2 — идея.**
💬 «Лабораторные делались отдельно, а здесь каждая стала слоем одной системы: REST и MVC — вход,
IoC собирает приложение, конфигурация задаёт параметры, AOP добавляет сквозную логику, формы — интерфейс,
а лаба 7 превращает API в документированный контракт. Всё работает на одном сервисе книг,
и каждый эксперимент из отчётов проверяется автотестом — их 32».

🖥 **Слайд 3 — архитектура.**
💬 «Запрос приходит в DispatcherServlet. REST-контроллеры отдают JSON через DTO, страничный контроллер —
шаблон Thymeleaf. Ошибки обрабатывают два ControllerAdvice. Сервис книг — это CGLIB-прокси, через него
проходят четыре аспекта. Внутри — репозиторий, уведомления и кэш цитат».

Дальше: «Сейчас покажу лабу 7 вживую», — и переключиться в IntelliJ.

---

## ЛАБА 7 — по чек-листу методички (раздел 7)

### Этап 2. Три типа: Request, Response, Book (пункт 1 чек-листа)
📂 Открыть рядом `BookRequest.java`, `BookResponse.java`, `Book.java`.
Показать: в `BookRequest` нет `id` и `createdAt`; в `BookResponse` они есть; `Book` — доменный record.

💬 «Здесь три разных типа, и у каждого своя задача.
Book — доменная модель, она живёт внутри сервера.
BookRequest — то, что клиенту разрешено прислать: в нём нет id и createdAt, их назначает сервер,
поэтому клиент не может подменить идентификатор или дату.
BookResponse — то, что API обещает вернуть.
Если бы контроллер принимал Book напрямую, клиент мог бы сам выставить id — это mass assignment.
А если бы я переименовал поле в хранилище, сломались бы все клиенты. DTO отделяет контракт от хранилища.
Валидация из лабы 6 — @NotBlank, @Size(max = 120), @NoDigits — осталась на BookRequest».

### Этап 3. Маппер (пункт 2)
📂 `BookMapper.java`, затем `BookRestController.java` — показать вызовы `mapper.toDomain(...)` и `mapper.toResponse(...)`.

💬 «BookMapper — это @Component и единственное место, которое знает обе формы и доменный объект.
toDomain превращает запрос в Book, toResponse — Book в ответ. Контроллер вызывает маппер,
а своего кода конверсии в нём нет — поэтому контроллер тонкий, а правило "что видит клиент" в одном месте».

### Этап 4. Список и одна книга (пункт 3)
📂 `books.http` → ▶ первый запрос `GET /api/books`, потом ▶ второй `GET /api/books/1`.

💬 «Список — 200 и JSON-массив. Поля — ровно поля BookResponse: id, title, author, year, genre,
available, publishedOn и createdAt. Одна книга — тоже 200, но это мой вариант — об этом в конце».

### Этап 5. Создание: 201 + Location (пункт 4)
▶ запрос `POST /api/books` (Pro Spring 6). Показать в ответе: `HTTP/1.1 201`, заголовок `Location: /api/books/5`, тело.

💬 «Создание возвращает 201 Created, а не 200 — так клиент отличает создание от обновления.
Заголовок Location содержит URI новой книги, а в теле — созданный ресурс с id и createdAt, которые назначил сервер».

### Этап 6. Переход по Location (пункт 5)
▶ `GET /api/books/5` (если в Location другой номер — подставить его).

💬 «Книга действительно есть по этому адресу. Location избавляет клиента от необходимости угадывать URI —
сервер сам сообщает, где новый ресурс».

### Этап 7. Конфликт 409 (пункт 9)
▶ второй `POST` той же книги. Показать: `409`, `Content-Type: application/problem+json`, тело.

💬 «Повторное создание той же книги — 409 Conflict. Не 400, потому что сам запрос корректный —
он конфликтует с текущим состоянием каталога: книга с таким названием и автором уже есть.
И не 500 — это бизнес-ситуация, а не сбой сервера. Сервис бросает DuplicateTitleException,
обработчик превращает её в 409».
Если спросят про `bookTitle`:
💬 «В методичке дополнительное поле называлось title, но title — уже стандартное поле ProblemDetail,
и в JSON было бы два одинаковых ключа. Поэтому я назвал его bookTitle».

### Этап 8. Невалидное тело — 400 от базового класса (пункт 8)
▶ `POST` с пустым `title`. Показать: `400`, `application/problem+json`, `"detail": "Invalid request content."`, `errors.title`.

💬 «Пустое название — 400. Этот ответ построил не я, а базовый класс ResponseEntityExceptionHandler —
видно по detail "Invalid request content.". Я только добавил поле errors: какое поле отклонено и почему.
Битый JSON тоже даёт 400, но по другой причине — тело просто нельзя прочитать».

### Этап 9. PUT (дополнительно)
▶ `PUT /api/books/5`. Показать `200`, новое название, `createdAt` не изменился.

💬 «PUT заменяет все поля, которые задаёт клиент. createdAt принадлежит серверу, поэтому сохраняется.
PUT идемпотентен: повторный такой же запрос даст тот же результат. В отличие от PATCH,
поле, которого нет в теле, при PUT не сохранится — PUT заменяет ресурс целиком».

### Этап 10. Удаление дважды (пункт 6)
▶ `DELETE /api/books/5` → `204`. ▶ тот же `DELETE` ещё раз → `404`.

💬 «Первый раз — 204 No Content: удалено, возвращать нечего.
Второй раз я осознанно отвечаю 404. Состояние сервера при повторе не меняется — DELETE остаётся идемпотентным,
а клиент получает полезную информацию: удалять было нечего. Главное — это не 500».

### Этап 11. Несуществующая книга — пять полей (пункт 7)
▶ `GET /api/books/999`. Прочитать вслух поля.

💬 «404 в формате RFC 9457, тип application/problem+json. Пять стандартных полей:
type — ссылка на тип ошибки, title — "Book not found", status — 404,
detail — "Book with id 999 does not exist", instance — "/api/books/999".
Плюс моё поле bookId. Стектрейса в ответе нет — он остаётся в логе сервера,
а клиенту показывать внутренности приложения нельзя».

### Этап 12. Один обработчик и его scope (пункт 10)
📂 `ApiExceptionHandler.java`: показать `@RestControllerAdvice(annotations = RestController.class)`,
`extends ResponseEntityExceptionHandler`, методы для 404 и 409.
🌐 затем открыть `http://localhost:8080/books/999`.

💬 «Все ошибки API превращаются в ответы в одном классе — в контроллерах нет try/catch.
Наследование от ResponseEntityExceptionHandler даёт ProblemDetail для стандартных ошибок Spring MVC бесплатно.
Advice ограничен аннотацией RestController, поэтому HTML-страницы лабы 6 не затронуты —
для /books/999 показывается обычная страница 404, а не JSON. Без этого ограничения страницы начали бы отдавать JSON».

### Этап 13. OpenAPI и Swagger UI (пункт 11)
🌐 `http://localhost:8080/v3/api-docs` → **Ctrl+F** → `maxLength` → показать `120`.

💬 «Документ OpenAPI генерирует springdoc 3.1.1 — это ветка для Spring Boot 4.
Без единой аннотации он уже знает пути, методы, параметры и схемы DTO.
maxLength 120 у названия взялся из @Size(max = 120) в BookRequest».

🌐 вкладка `swagger-ui.html` → раскрыть `GET /api/books/{id}` → **Try it out** → ввести `999` → **Execute**.
Показать 404 и ProblemDetail внутри Swagger.

💬 «Swagger UI отображает этот документ. Аннотации @Tag, @Operation, @ApiResponses и @Parameter добавляют то,
чего код сказать не может: описания операций, смысл каждого кода ответа, примеры».

📂 `src/main/resources/application-prod.yml` — показать `springdoc.api-docs.enabled: false`.
💬 «На публичном сервере Swagger оставлять нельзя: он раскрывает всё API и даёт консоль для запросов.
В профиле prod я его выключил — это проверяется тестом ProdProfileTests».

### Этап 14. Индивидуальный вариант 7 — HATEOAS (пункт 12)
▶ ещё раз `GET /api/books/1` в `books.http`. Показать `Content-Type: application/hal+json` и блок `_links`.

💬 «Мой вариант 7 — HATEOAS. GET одной книги возвращает EntityModel в формате HAL.
Кроме полей книги есть ссылки: self — на саму книгу, books — на весь каталог, citation — на её цитату из лабы 5.
Клиенту не нужно знать структуру URI заранее — ответ сам говорит, куда можно перейти дальше».
(Можно кликнуть `citation` — откроется текст «Joshua Bloch (2018). Effective Java.»)

### Этап 15. Тесты лабы 7
📂 `src/test/java/kz/iitu/library/Lab7RestApiTests.java` — пролистать названия тестов.
Если есть время: правой кнопкой по папке `src/test/java` → **Run 'All Tests'**.

💬 «Все эти сценарии покрыты автотестами: 13 тестов на лабу 7 — 201 с Location, 409, пять полей 404,
повторный DELETE, 400 от базового класса, HATEOAS-ссылки, maxLength в OpenAPI. Всего в проекте 32 теста, все зелёные».

---

## ЛАБЫ 1–6 — обзор итогового проекта

🌐 Открыть `http://localhost:8080/` — главная с карточками по каждой лабе.
💬 «Главная страница — статический ресурс из лабы 5, она отдаётся без контроллера. На ней ссылки на все лабы».

### Этап 16. Лаба 1 — Spring Boot и REST (вариант 7)
🌐 клики по карточке Lab 1: `/api/hello?name=IITU`, `/api/info`, `/api/wordcount`.
📂 `pom.xml` (parent 4.1.1, Java 25) и `web/HelloController.java`.

💬 «Лаба 1 — первый REST-контроллер: @RestController, @GetMapping, @RequestParam, @Value.
Spring Boot сам превращает объект в JSON через Jackson. Мой вариант 7 — wordcount:
количество слов, символов и самое длинное слово. Без параметра возвращаются нули, а не ошибка — благодаря defaultValue».

### Этап 17. Лаба 2 — IoC-контейнер (вариант 8)
🌐 `/api/system/notify?text=IITU`, `/api/system/notify/html?text=Hello <world>`,
`/api/system/scopes` (обновить F5 два раза), `/api/system/lifecycle`.
📂 `notify/EmailNotifier.java` (@Primary), `notify/NotificationService.java`.

💬 «Есть интерфейс Notifier и четыре бина. email помечен @Primary — он выбирается, когда нужен один бин;
noop — @Fallback; порядок в списке задаёт @Order. NotificationService получает их всеми способами:
@Primary, @Qualifier, List и Map. Внедрение через конструктор — поля final, объект всегда полностью собран.
Мой вариант 8 — HtmlNotifier: оборачивает текст в <p> и экранирует угловые скобки.
В scopes видно: prototype-бин, внедрённый в синглтон, один и тот же, а через ObjectProvider каждый раз новый.
lifecycle показывает порядок: конструктор, потом @PostConstruct.
И это не просто демо: когда добавляется книга, сервис отправляет уведомление через @Primary-бин».

### Этап 18. Лаба 3 — конфигурация (вариант 7)
🌐 `/api/config`, `/actuator/env/app.mail.retry-count`, `/api/system/cache`.
📂 `application.yml`, `application-dev.yml`, `config/AppProperties.java`.

💬 «Настройки вынесены в YAML, для dev и prod есть свои файлы. AppProperties — record с @ConfigurationProperties
и @Validated: если поставить retry-count 99, приложение просто не запустится. Actuator показывает не только значение,
но и откуда оно взялось. Приоритет: аргумент командной строки сильнее системного свойства, переменной окружения и файлов.
Мой вариант 7 — app.cache: время жизни и размер кэша. В проекте он реально используется — им настроен кэш цитат,
в /api/system/cache видно попадания и промахи».
(Профиль dev показывать необязательно: перезапуск с `-Dspring-boot.run.profiles=dev` даёт порт 8081 и banner DEVELOPMENT.)

### Этап 19. Лаба 5 — Spring MVC (вариант 6)
🌐 `/api/books?author=Bloch&limit=1`, `/api/books/1/citation`, `/books`.
В Terminal IntelliJ показать коды ошибок сопоставления:
```
curl.exe -i http://localhost:8080/api/books/abc
curl.exe -i -X DELETE http://localhost:8080/api/books
curl.exe -i -X POST http://localhost:8080/api/books -H "Content-Type: text/plain" -d "x"
curl.exe -i http://localhost:8080/api/books -H "Accept: application/xml"
```
💬 «Лаба 5 — маршрутизация. Параметры берутся из разных частей запроса: author и limit из query, id из пути,
книга из тела. Каждый код ошибки называет часть сопоставления, которая не совпала:
abc нельзя превратить в число — 400; путь есть, но метод не тот — 405; тело не JSON — 415;
клиент просит XML, а мы его не отдаём — 406. Вариант 6 — цитата в text/plain, при Accept JSON будет 406.
Страница /books — @Controller: он возвращает не данные, а имя шаблона Thymeleaf».

### Этап 20. Лаба 6 — формы и валидация (вариант 7)
🌐 `http://localhost:8080/books/new?lang=ru` → нажать «Сохранить» с пустой формой.
Затем: автор `Bloch 2018`, год `1200`, дата `2030-01-01` → «Сохранить».
Затем корректно: «Абай жолы», «Мухтар Ауэзов», 1942, Роман, `01.01.1942` → «Сохранить» → нажать **F5**.
Потом попробовать добавить `Effective Java` / `Joshua Bloch` → ошибка дубликата.
📂 `web/BookForm.java`, `validation/NoDigits.java`, `messages_ru.properties`.

💬 «Форма привязана к отдельному объекту BookForm — это защита от mass assignment, и введённые данные не теряются.
@Valid запускает валидацию, ошибки попадают в BindingResult, и форма показывается снова с сообщениями
на выбранном языке — тексты лежат в messages.properties и messages_ru.properties.
@NoDigits — моё собственное ограничение: имя автора без цифр.
Вариант 7 — дата публикации: @PastOrPresent, а формат зависит от языка — на русском 01.01.1942, на английском 1/1/42.
После сохранения — Post/Redirect/Get: F5 не создаёт дубль, а flash-сообщение исчезает.
А правило дубликата из лабы 7 работает и здесь — как ошибка поля».

### Этап 21. Лаба 4 — Spring AOP (вариант 8) — в конце, потому что удаляет книги
🌐 `/api/system/proxy` — показать `BookService$$SpringCGLIB$$...`.
🌐 `/api/books/stats` → 📂 переключиться на консоль IntelliJ: показать `[AUDIT]`, `[LOG]`, `[TRACE]`, `[TIME] SLOW`.
🌐 открыть карточку `/books/1` → в консоли вложенный `[TRACE]`: `citation` → `CitationCache.get` с отступом.
Самовызов, в Terminal:
```
curl.exe -X DELETE http://localhost:8080/api/system/audit
curl.exe -X DELETE "http://localhost:8080/api/books/bulk?ids=3&viaProxy=false"
curl.exe -X DELETE "http://localhost:8080/api/books/bulk?ids=4&viaProxy=true"
```
🌐 `/api/system/audit` — запись `BOOK_DELETE` только одна (для id 4).
📂 `aspect/Pointcuts.java`, `aspect/AuditAspect.java`, `service/BookService.java` (`deleteBulk`, поле `self`).

💬 «Логирование, замер времени, аудит и трассировка — сквозная функциональность, она вынесена в аспекты.
Pointcut говорит "где" — все публичные методы пакета service, advice говорит "что делать".
Порядок задаёт @Order: аудит снаружи, затем лог, затем время. Spring сделал CGLIB-прокси — подкласс BookService,
через который проходят все вызовы. statistics работает 300 мс, и TimingAspect пишет SLOW.
Эксперимент самовызова: при viaProxy=false метод вызывает this.delete — вызов не проходит через прокси,
аудита нет. При viaProxy=true вызов идёт через ссылку self на прокси — аудит есть.
Мой вариант 8 — трассировка: отступ показывает вложенность вызовов, как citation → CitationCache.get».

---

## Этап 22. Завершение (слайды 12–16)
🖥 Слайд «how labs connect» → «automated test» → «Summary».

💬 «Главное отличие проекта от набора лабораторных — связи между ними: конфигурация управляет уведомлениями
и кэшем, аспекты оборачивают все операции, валидация лабы 6 защищает и API лабы 7, а правило 409 работает и в форме.
Итог: одно приложение вместо семи отдельных демо, семь индивидуальных вариантов, 32 автотеста.
Спасибо за внимание, готов ответить на вопросы».

---

## Если что-то пошло не так
| Ситуация | Что делать |
|----------|-----------|
| `GET /api/books/5` даёт 404 | Посмотреть номер в `Location` после POST и подставить его |
| POST сразу даёт 409 | Книга уже создавалась раньше — это нормально, объяснить 409; для 201 поменять title |
| Whitelabel Error на `/` | Запущена старая лаба — остановить ■, запустить `LibraryPortalApplication` |
| Старый дизайн/нет Swagger | Maven → ⟳ Reload, перезапуск, в браузере Ctrl+F5 |

Ответы на вопросы преподавателя по всем лабам — `docs/DEFENSE.md`, раздел 4.
