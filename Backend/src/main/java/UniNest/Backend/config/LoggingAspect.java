package UniNest.Backend.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

//@Aspect
//@Component
@Slf4j
public class LoggingAspect {

    // Target all methods in your controller package
    @Pointcut("within(UniNest.Backend.controller..*)")
    public void controllerMethods() {}

    @Before("controllerMethods()")
    public void logRequest(JoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            // ADD THIS CHECK: Ensure attributes and request are not null
            if (attributes != null && attributes.getRequest() != null) {
                HttpServletRequest request = attributes.getRequest();
                log.info(">>> API REQ: {} {} | Method: {}.{}",
                        request.getMethod(),
                        request.getRequestURI(),
                        joinPoint.getSignature().getDeclaringTypeName(),
                        joinPoint.getSignature().getName());
            }
        } catch (Exception e) {
            // Safe guard: Never allow the logger to crash the actual API call
            System.err.println("LoggingAspect Error: " + e.getMessage());
        }
    }
    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void logResponse(JoinPoint joinPoint, Object result) {
        log.info("<<< API RES: Method {}.{} completed successfully",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
    }

    @AfterThrowing(pointcut = "controllerMethods()", throwing = "ex")
    public void logError(JoinPoint joinPoint, Exception ex) {
        log.error("!!! API ERR: Method {}.{} failed with message: {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                ex.getMessage());
    }
}