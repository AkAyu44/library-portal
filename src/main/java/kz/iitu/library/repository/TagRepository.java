package kz.iitu.library.repository;

import kz.iitu.library.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Lab 8: tags are found or created by name. */
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNameIgnoreCase(String name);
}
