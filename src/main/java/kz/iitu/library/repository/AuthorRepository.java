package kz.iitu.library.repository;

import kz.iitu.library.domain.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Lab 8: authors are looked up by name — the API and the form still send the author as text. */
public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByNameIgnoreCase(String name);
}
