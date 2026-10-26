package kz.iitu.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Lab 8: the other end of book <-> tag. The association is unidirectional — Book.tags owns it
 * and declares book_tag — so a Tag knows nothing about its books and has no collection.
 */
@Entity
@Table(name = "tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    protected Tag() { }

    public Tag(String name) {
        this.name = name;
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    /** Tags live in a Set: equal by the natural key, which exists before the id is generated. */
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Tag other && Objects.equals(name, other.name));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "Tag{" + name + "}";
    }
}
