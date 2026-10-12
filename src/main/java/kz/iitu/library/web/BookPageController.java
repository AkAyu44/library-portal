package kz.iitu.library.web;

import jakarta.validation.Valid;
import kz.iitu.library.domain.Book;
import kz.iitu.library.domain.Genre;
import kz.iitu.library.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Lab 5: @Controller returns a view name; the model carries the data to Thymeleaf.
 * Lab 6: form binding + @Valid + BindingResult, Post/Redirect/Get with a flash attribute.
 */
@Controller
@RequestMapping("/books")
public class BookPageController {

    private final BookService service;

    public BookPageController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String author, Model model) {
        model.addAttribute("books", service.findAll(author));
        model.addAttribute("author", author);
        return "books/list";                    // -> templates/books/list.html
    }

    @GetMapping("/{id}")
    public String details(@PathVariable long id, Model model) {
        model.addAttribute("book", service.findById(id));   // may throw BookNotFoundException
        model.addAttribute("citation", service.citation(id));
        return "books/details";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new BookForm());
        model.addAttribute("genres", Genre.values());
        return "books/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BookForm form,
                         BindingResult binding,          // immediately after the validated object!
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("genres", Genre.values());   // the POST model is new
            return "books/form";
        }
        Book saved = service.create(form.toBook());
        redirect.addFlashAttribute("addedTitle", saved.title());
        return "redirect:/books";                // 302 → the browser performs GET /books
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirect) {
        Book book = service.findById(id);
        service.delete(id);
        redirect.addFlashAttribute("deletedTitle", book.title());
        return "redirect:/books";
    }
}
