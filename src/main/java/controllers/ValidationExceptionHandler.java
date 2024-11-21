package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.timgroup.statsd.StatsDClient;


@ControllerAdvice
public class ValidationExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ValidationExceptionHandler.class);

    @Autowired
    private StatsDClient statsDClient;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String path = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest().getRequestURI();
            
        logger.warn("Validation failed for request to {}", path);
        statsDClient.incrementCounter("api.error.validation");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                String fieldName = fieldError.getField();
                String errorMessage = error.getDefaultMessage();
                
                logger.debug("Validation error - Field: {}, Message: {}, Value: '{}'", 
                    fieldName, 
                    errorMessage, 
                    fieldError.getRejectedValue());
                
                errors.put(fieldName, errorMessage);
                statsDClient.incrementCounter("api.error.validation." + fieldName);
            }
        });

        response.put("timestamp", new Date());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("errors", errors);
        response.put("path", path);
        
        logger.warn("Validation failed with {} errors", errors.size());
        return ResponseEntity.badRequest().body(response);
    }
}