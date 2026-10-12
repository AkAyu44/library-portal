package kz.iitu.library.web;

import kz.iitu.library.service.BookNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lab 6: an HTML error page for the @Controller classes.
 * Ordered after ApiExceptionHandler, so it only ever sees exceptions of the page controllers.
 */
@ControllerAdvice(annotations = Controller.class)
@Order(Ordered.LOWEST_PRECEDENCE)
public class PageExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(BookNotFoundException ex, Model model) {
        model.addAttribute("bookId", ex.getId());
        model.addAttribute("message", ex.getMessage());
        return "error/not-found";
    }
}
