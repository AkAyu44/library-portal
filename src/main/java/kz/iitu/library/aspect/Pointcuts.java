package kz.iitu.library.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/** Lab 4: a library of named pointcuts (WHERE), reused by every aspect. */
@Aspect
@Component
public class Pointcuts {

    @Pointcut("within(kz.iitu.library.service..*)")
    public void serviceLayer() {}

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {}

    @Pointcut("serviceLayer() && publicMethod()")
    public void serviceOperation() {}
}
