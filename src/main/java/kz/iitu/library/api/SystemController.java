package kz.iitu.library.api;

import kz.iitu.library.audit.AuditLog;
import kz.iitu.library.lifecycle.LifecycleDemo;
import kz.iitu.library.notify.NotificationService;
import kz.iitu.library.scope.TicketOffice;
import kz.iitu.library.service.BookService;
import kz.iitu.library.service.CitationCache;
import org.springframework.aop.support.AopUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Demonstration endpoints for the "inside of Spring": IoC (Lab 2) and AOP proxies (Lab 4). */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final NotificationService notifications;
    private final LifecycleDemo lifecycle;
    private final TicketOffice ticketOffice;
    private final BookService bookService;
    private final CitationCache citationCache;
    private final AuditLog auditLog;

    public SystemController(NotificationService notifications, LifecycleDemo lifecycle,
                            TicketOffice ticketOffice, BookService bookService,
                            CitationCache citationCache, AuditLog auditLog) {
        this.notifications = notifications;
        this.lifecycle = lifecycle;
        this.ticketOffice = ticketOffice;
        this.bookService = bookService;
        this.citationCache = citationCache;
        this.auditLog = auditLog;
    }

    // ---- Lab 2 ----
    @GetMapping("/notify")
    public Map<String, Object> notify(@RequestParam(defaultValue = "Hello") String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("primary", notifications.viaPrimary(text));
        result.put("console", notifications.viaConsole(text));
        result.put("all", notifications.viaAll(text));
        result.put("order", notifications.order());
        result.put("beanNames", notifications.names());
        return result;
    }

    @GetMapping("/notify/html")
    public String html(@RequestParam String text) {
        return notifications.viaHtml(text);
    }

    @GetMapping("/lifecycle")
    public List<String> lifecycle() {
        return lifecycle.events();
    }

    @GetMapping("/scopes")
    public Map<String, Object> scopes() {
        return ticketOffice.demo();
    }

    // ---- Lab 4 ----
    @GetMapping("/proxy")
    public Map<String, Object> proxy() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isAopProxy", AopUtils.isAopProxy(bookService));
        result.put("isCglib", AopUtils.isCglibProxy(bookService));
        result.put("className", bookService.getClass().getName());
        result.put("superClass", bookService.getClass().getSuperclass().getSimpleName());
        return result;
    }

    @GetMapping("/audit")
    public List<AuditLog.Entry> audit() {
        return auditLog.latest();
    }

    @DeleteMapping("/audit")
    public void clearAudit() {
        auditLog.clear();
    }

    // ---- Lab 3 (variant 7 cache) ----
    @GetMapping("/cache")
    public Map<String, Object> cache() {
        return citationCache.stats();
    }
}
