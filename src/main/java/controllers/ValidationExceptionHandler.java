package controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        System.out.println("Validation Error. Request body: " + ex.getBindingResult().getTarget());
        
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                System.out.println("Field: " + fieldName);
                System.out.println("Error: " + errorMessage);
                System.out.println("Rejected Value: " + ((FieldError) error).getRejectedValue());
                errors.put(fieldName, errorMessage);
            }
        });
        
        return ResponseEntity.badRequest().body(errors);
    }
}