package kz.iitu.library.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Lab 4, variant 8: call tracing — indentation grows with the nesting depth of calls. */
@Aspect
@Component
public class CallTracingAspect {
    private static final Logger log = LoggerFactory.getLogger(CallTracingAspect.class);

    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    @Before("kz.iitu.library.aspect.Pointcuts.serviceOperation()")
    public void before(JoinPoint jp) {
        int currentDepth = depth.get();
        log.info("[TRACE] {}-> {}", "  ".repeat(currentDepth), jp.getSignature().toShortString());
        depth.set(currentDepth + 1);
    }

    @After("kz.iitu.library.aspect.Pointcuts.serviceOperation()")
    public void after(JoinPoint jp) {
        int currentDepth = Math.max(0, depth.get() - 1);
        depth.set(currentDepth);
        log.info("[TRACE] {}<- {}", "  ".repeat(currentDepth), jp.getSignature().toShortString());
    }
}
