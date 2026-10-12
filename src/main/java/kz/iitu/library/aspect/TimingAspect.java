package kz.iitu.library.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Lab 4: @Around measures execution time; without pjp.proceed() the target would never run. */
@Aspect
@Component
@Order(3)
public class TimingAspect {
    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);
    private static final long SLOW_THRESHOLD_MS = 200;

    @Around("kz.iitu.library.aspect.Pointcuts.serviceOperation()")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        long started = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long ms = (System.nanoTime() - started) / 1_000_000;
            String name = pjp.getSignature().toShortString();
            if (ms > SLOW_THRESHOLD_MS) {
                log.warn("[TIME] SLOW: {} - {} ms", name, ms);
            } else {
                log.info("[TIME] {} - {} ms", name, ms);
            }
        }
    }
}
