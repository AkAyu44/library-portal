package kz.iitu.library.persistence;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;

/**
 * Lab 8: counts the SQL statements Hibernate sends, so the N+1 and Page/Slice numbers of the report
 * are measured, not guessed. Hibernate passes every statement it prepares through inspect();
 * the recorder keeps them only for the current thread and only between start() and stop().
 * Registered by SqlRecorderConfig.
 */
public class SqlRecorder implements StatementInspector {

    private static final ThreadLocal<List<String>> CURRENT = new ThreadLocal<>();

    public static void start() {
        CURRENT.set(new ArrayList<>());
    }

    public static List<String> stop() {
        List<String> recorded = CURRENT.get();
        CURRENT.remove();
        return recorded == null ? List.of() : List.copyOf(recorded);
    }

    @Override
    public String inspect(String sql) {
        List<String> recorded = CURRENT.get();
        if (recorded != null) {
            recorded.add(sql);
        }
        return sql;
    }
}
