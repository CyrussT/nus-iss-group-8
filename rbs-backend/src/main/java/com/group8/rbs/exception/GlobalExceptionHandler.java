package com.group8.rbs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (e.g., @Min, @NotBlank, etc.)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        
        String message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        
        errorResponse.put("message", message);
        errorResponse.put("errorCode", "BAD_REQUEST");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
