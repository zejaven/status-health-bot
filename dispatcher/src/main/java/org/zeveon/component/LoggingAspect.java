package org.zeveon.component;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* org.zeveon.service.impl.ChatSettingsServiceImpl.getAllChatSettings(..))")
    public void excludedMethod() {}

    @Before("!excludedMethod() && execution(* org.zeveon.service.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        log.debug("Entering method {} with arguments {}", joinPoint.getSignature(), joinPoint.getArgs());
    }
}
