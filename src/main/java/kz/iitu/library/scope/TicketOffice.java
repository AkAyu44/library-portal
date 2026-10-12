package kz.iitu.library.scope;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Lab 2: a singleton holding a prototype directly vs. through ObjectProvider. */
@Component
public class TicketOffice {
    private final Ticket direct;
    private final ObjectProvider<Ticket> provider;

    public TicketOffice(Ticket direct, ObjectProvider<Ticket> provider) {
        this.direct = direct;
        this.provider = provider;
    }

    public Map<String, Object> demo() {
        return Map.of(
                "injectedDirectly", List.of(direct.id(), direct.id()),
                "viaProvider", List.of(provider.getObject().id(), provider.getObject().id()),
                "office", System.identityHashCode(this));
    }
}
