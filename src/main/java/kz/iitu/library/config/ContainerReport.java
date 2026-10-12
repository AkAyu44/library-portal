package kz.iitu.library.config;

import kz.iitu.library.notify.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** Lab 2: programmatic inspection of the ApplicationContext at startup. */
@Component
public class ContainerReport implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ContainerReport.class);

    private final ApplicationContext context;

    public ContainerReport(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(String... args) {
        log.info("Bean definitions in total: {}", context.getBeanDefinitionCount());
        log.info("Notifier implementations: {}",
                Arrays.toString(context.getBeanNamesForType(Notifier.class)));
        log.info("Type of the bookService bean: {}",
                context.getBean("bookService").getClass().getName());
    }
}
