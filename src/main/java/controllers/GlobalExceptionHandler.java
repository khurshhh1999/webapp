package controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import com.timgroup.statsd.StatsDClient;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private StatsDClient statsDClient;

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
        // Increment error counter
        System.out.println("DEBUG: Handling ResponseStatusException: " + ex.getMessage());
        System.out.println("DEBUG: Status code: " + ex.getStatusCode());
        System.out.println("DEBUG: Reason: " + ex.getReason());
        statsDClient.incrementCounter("api.error");
        logger.error("Error occurred: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ex.getReason(), ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        System.out.println("DEBUG: Handling unexpected Exception: " + ex.getMessage());
        System.out.println("DEBUG: Exception type: " + ex.getClass().getName());
        if (ex.getCause() != null) {
        System.out.println("DEBUG: Cause: " + ex.getCause().getMessage());
    }
        statsDClient.incrementCounter("api.error");
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
