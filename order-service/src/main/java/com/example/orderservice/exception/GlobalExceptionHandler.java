package com.example.orderservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * מחלקה לטיפול גלובלי בשגיאות
 * מחזירה הודעות שגיאה מפורטות ומאורגנות ללקוח
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * טיפול בשגיאות validation של Spring
     * מחזיר הודעות שגיאה מפורטות עבור שדות לא תקינים
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error - {} field errors, {} global errors", 
                ex.getBindingResult().getFieldErrors().size(), 
                ex.getBindingResult().getGlobalErrors().size());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "validation-error");
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                        (existing, replacement) -> existing + "; " + replacement
                ));
        errorResponse.put("fieldErrors", fieldErrors);

        String generalMessage = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        errorResponse.put("message", generalMessage);

        log.error("Validation errors: {}", fieldErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * טיפול בשגיאות HttpMessageNotReadableException
     * מחזיר הודעות שגיאה עבור JSON לא תקין
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("HTTP message not readable error: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "invalid-json");
        errorResponse.put("message", "Invalid JSON format: " + ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());

        if (ex.getRootCause() != null) {
            errorResponse.put("rootCause", ex.getRootCause().getMessage());
        }



        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * טיפול בשגיאות IllegalArgumentException
     * מחזיר הודעות שגיאה ברורות עבור פרמטרים לא תקינים
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument error: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "invalid-input");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());



        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * טיפול בשגיאות RuntimeException
     * מחזיר הודעות שגיאה עבור שגיאות זמן ריצה
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime error occurred: {}", ex.getMessage(), ex);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "internal-error");
        errorResponse.put("message", "An internal error occurred: " + ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * טיפול בשגיאות כללית
     * מחזיר הודעות שגיאה עבור כל השגיאות שלא טופלו
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "unexpected-error");
        errorResponse.put("message", "An unexpected error occurred");
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
} 