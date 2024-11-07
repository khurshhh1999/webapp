package aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.timgroup.statsd.StatsDClient;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class LoggingMetricsAspect {
    private final Logger logger = LoggerFactory.getLogger(LoggingMetricsAspect.class);
    @PostConstruct
    public void init() {
        System.out.println("DEBUG: LoggingMetricsAspect initialized");
        logger.error("TEST LOG: LoggingMetricsAspect initialized");
    }
    @Autowired
    private StatsDClient statsDClient;

    @Around("within(controllers..*)")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // Add method parameter logging
        Object[] args = joinPoint.getArgs();
        logger.info("Controller method called with {} parameters", args.length);
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                // Safely log parameter types without exposing sensitive data
                logger.debug("Parameter {}: Type={}", i, args[i].getClass().getSimpleName());
            }
        }
        return logMethod(joinPoint, "controller");
    }

    @Around("within(services..*)")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // Add service-specific logging
        String methodName = joinPoint.getSignature().getName();
        if (methodName.contains("ProfilePicture")) {
            logger.info("Profile Picture Service operation: {}", methodName);
        }
        return logMethod(joinPoint, "service");
    }

    private Object logMethod(ProceedingJoinPoint joinPoint, String type) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String metricPrefix = "csye6225." + type + "." + className.toLowerCase() + "." + methodName;

        // Enhanced HTTP request logging
        String requestDetails = "";
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();
            requestDetails = String.format(" [%s %s] [ContentType: %s] [ContentLength: %s]", 
                request.getMethod(), 
                request.getRequestURI(),
                request.getContentType(),
                request.getContentLength());
            logger.info("Request Headers:");
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                logger.debug("Header {}: {}", headerName, request.getHeader(headerName));
            }
        } catch (Exception e) {
            logger.warn("Could not get request details: {}", e.getMessage());
        }

        logger.info("START: {}.{}{}", className, methodName, requestDetails);
        long startTime = System.currentTimeMillis();

        try {
            statsDClient.incrementCounter(metricPrefix + ".count");
            
            // Log memory usage before method execution
            Runtime runtime = Runtime.getRuntime();
            long usedMemoryBefore = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            logger.debug("Memory usage before {}.{}: {}MB", className, methodName, usedMemoryBefore);
            
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            statsDClient.recordExecutionTime(metricPrefix + ".time", duration);
            
            // Log memory usage after method execution
            long usedMemoryAfter = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            logger.debug("Memory usage after {}.{}: {}MB", className, methodName, usedMemoryAfter);
            
            logger.info("SUCCESS: {}.{} - Duration: {}ms{}", 
                className, methodName, duration, requestDetails);
            
            // Log result type (but not content for security)
            if (result != null) {
                logger.debug("Method returned type: {}", result.getClass().getSimpleName());
            }
            
            return result;
        } catch (Exception e) {
            statsDClient.incrementCounter(metricPrefix + ".error");
            
            logger.error("ERROR: {}.{} - Error: {}{}", 
                className, methodName, e.getMessage(), requestDetails);
            logger.error("Stack trace: ", e);
            
            // Log additional error context
            logger.error("Error occurred in {}.{}", className, methodName);
            logger.error("Error type: {}", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            
            throw e;
        }
    }

    @Around("execution(* com.amazonaws.services.s3.AmazonS3.*(..))")
    public Object logS3Operation(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = joinPoint.getSignature().getName();
        String metricPrefix = "csye6225.s3." + operation.toLowerCase();
        
        // Log S3 operation details
        Object[] args = joinPoint.getArgs();
        if (args.length > 0) {
            logger.info("S3 Operation: {} with {} parameters", operation, args.length);
            if (args[0] != null) {
                logger.debug("Bucket name: {}", args[0]);
            }
            if (args.length > 1 && args[1] != null) {
                logger.debug("S3 key/prefix: {}", args[1]);
            }
        }
        
        logger.info("S3 Operation START: {}", operation);
        long startTime = System.currentTimeMillis();

        try {
            statsDClient.incrementCounter(metricPrefix + ".count");
            
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            statsDClient.recordExecutionTime(metricPrefix + ".time", duration);
            
            logger.info("S3 Operation SUCCESS: {} - Duration: {}ms", operation, duration);
            
            // Log S3 operation result type
            if (result != null) {
                logger.debug("S3 operation returned type: {}", result.getClass().getSimpleName());
            }
            
            return result;
        } catch (Exception e) {
            statsDClient.incrementCounter(metricPrefix + ".error");
            
            logger.error("S3 Operation ERROR: {} - Error: {}", operation, e.getMessage());
            logger.error("S3 Error Details:", e);
            if (e.getCause() != null) {
                logger.error("S3 Error Cause: {}", e.getCause().getMessage());
            }
            
            throw e;
        }
    }

    @Around("within(data..*)")
    public Object logDatabaseOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = joinPoint.getSignature().getName();
        String metricPrefix = "csye6225.db." + operation.toLowerCase();
        
        // Log DB operation details
        Object[] args = joinPoint.getArgs();
        logger.info("DB Operation: {} with {} parameters", operation, args.length);
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                logger.debug("Parameter {} type: {}", i, args[i].getClass().getSimpleName());
            }
        }
        
        logger.info("DB Operation START: {}", operation);
        long startTime = System.currentTimeMillis();

        try {
            statsDClient.incrementCounter(metricPrefix + ".count");
            
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            statsDClient.recordExecutionTime(metricPrefix + ".time", duration);
            
            logger.info("DB Operation SUCCESS: {} - Duration: {}ms", operation, duration);
            
            // Log DB operation result
            if (result != null) {
                logger.debug("DB operation returned type: {}", result.getClass().getSimpleName());
            }
            
            return result;
        } catch (Exception e) {
            statsDClient.incrementCounter(metricPrefix + ".error");
            
            logger.error("DB Operation ERROR: {} - Error: {}", operation, e.getMessage());
            logger.error("DB Error Details:", e);
            logger.error("DB Operation: {}", operation);
            if (e.getCause() != null) {
                logger.error("DB Error Cause: {}", e.getCause().getMessage());
            }
            
            throw e;
        }
    }
}