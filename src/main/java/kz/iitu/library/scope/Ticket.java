package kz.iitu.library.scope;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Lab 2: prototype scope — a new instance for every request to the container. */
@Component
@Scope("prototype")
public class Ticket {
    private final String id = UUID.randomUUID().toString().substring(0, 8);

    public String id() { return id; }
}
