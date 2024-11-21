package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.timgroup.statsd.StatsDClient;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @Autowired
    private StatsDClient statsDClient;

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String path = getCurrentRequestPath();
        
        // Enhanced logging
        logger.error("ResponseStatusException occurred: Status={}, Path={}, Message={}, Reason={}", 
            status, path, ex.getMessage(), ex.getReason());
        
        // More specific metrics
        statsDClient.incrementCounter("api.error." + status.value());
        statsDClient.incrementCounter("api.error.response_status");
        
        // Log stack trace for 5xx errors
        if (status.is5xxServerError()) {
            logger.error("Server error details:", ex);
        }

        // Additional debug information for specific error types
        if (status == HttpStatus.FORBIDDEN) {
            logger.warn("Access denied to resource: {}", path);
        } else if (status == HttpStatus.NOT_FOUND) {
            logger.warn("Resource not found: {}", path);
        }

        return new ResponseEntity<>(
            buildErrorResponse(ex.getReason(), status.value(), path),
            ex.getStatusCode()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        String path = getCurrentRequestPath();
        
        // Enhanced error logging
        logger.error("Unhandled exception occurred: Type={}, Path={}", 
            ex.getClass().getSimpleName(), path);
        logger.error("Exception details:", ex);

        // Log cause chain
        Throwable cause = ex.getCause();
        if (cause != null) {
            logger.error("Exception cause chain:");
            while (cause != null) {
                logger.error("Caused by: {} - {}", 
                    cause.getClass().getSimpleName(), 
                    cause.getMessage());
                cause = cause.getCause();
            }
        }

        // Specific metrics for different types of errors
        statsDClient.incrementCounter("api.error.500");
        statsDClient.incrementCounter("api.error.unhandled");
        if (ex instanceof IllegalArgumentException) {
            statsDClient.incrementCounter("api.error.validation");
        } else if (ex instanceof IllegalStateException) {
            statsDClient.incrementCounter("api.error.state");
        }

        return new ResponseEntity<>(
            buildErrorResponse("Internal Server Error", 500, path),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    // Add handler for S3 specific exceptions
    @ExceptionHandler(AmazonS3Exception.class)
    public ResponseEntity<Object> handleAmazonS3Exception(AmazonS3Exception ex) {
        String path = getCurrentRequestPath();
        
        logger.error("S3 operation failed: ErrorCode={}, ErrorType={}, Path={}", 
            ex.getErrorCode(), ex.getErrorType(), path);
        
        statsDClient.incrementCounter("api.error.s3");
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex.getStatusCode() == 404) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex.getStatusCode() == 403) {
            status = HttpStatus.FORBIDDEN;
        }

        return new ResponseEntity<>(
            buildErrorResponse(ex.getMessage(), status.value(), path),
            status
        );
    }

    private Map<String, Object> buildErrorResponse(String message, int status, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", new Date());
        errorResponse.put("status", status);
        errorResponse.put("error", HttpStatus.valueOf(status).getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("path", path);
        return errorResponse;
    }

    private String getCurrentRequestPath() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getRequestURI();
        } catch (Exception e) {
            logger.warn("Could not get current request path", e);
            return "unknown";
        }
    }
}