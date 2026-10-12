package kz.iitu.library.aspect;

import kz.iitu.library.audit.AuditLog;
import kz.iitu.library.audit.Audited;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** Lab 4: @Around + annotation binding; @Order(1) = outermost wrapper. */
@Aspect
@Component
@Order(1)
public class AuditAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLog auditLog;

    public AuditAspect(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        String action = audited.action();
        String args = audited.logArguments() ? Arrays.toString(pjp.getArgs()) : "";
        log.info("[AUDIT] start {} {}", action, args);
        try {
            Object result = pjp.proceed();
            log.info("[AUDIT] {} success", action);
            auditLog.add(action, "success", args);
            return result;
        } catch (Throwable ex) {
            log.warn("[AUDIT] {} failure: {}", action, ex.getMessage());
            auditLog.add(action, "failure", ex.getMessage());
            throw ex;   // never hide the original error
        }
    }
}
