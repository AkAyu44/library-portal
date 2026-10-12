package kz.iitu.library.notify;

/** Lab 2: one interface, several beans — the source of the ambiguity experiment. */
public interface Notifier {
    String send(String message);

    String channel();
}
