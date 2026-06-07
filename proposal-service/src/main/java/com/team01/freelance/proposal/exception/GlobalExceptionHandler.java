package com.team01.freelance.proposal.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundException(
            EntityNotFoundException ex, HttpServletRequest request) {
        return errorBody(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Object> handleForbiddenOperationException(
            ForbiddenOperationException ex, HttpServletRequest request) {
        return errorBody(ex.getMessage(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        return errorBody(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
            
        String message = "Malformed JSON request";
        
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof IllegalArgumentException) {
                message = cause.getMessage();
                break;
            }
            cause = cause.getCause();
        }

        return errorBody(message, HttpStatus.BAD_REQUEST, request);
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {
            
        String message = "Database constraint violation";
        String detailedMessage = ex.getMostSpecificCause().getMessage();
        
        if (detailedMessage.contains("unique") || detailedMessage.contains("duplicate")) {
            message = "This record already exists.";
        }

        return errorBody(message, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllOtherExceptions(
            Exception ex, HttpServletRequest request) {
            
        return errorBody("An unexpected error occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private static ResponseEntity<Object> errorBody(String message, HttpStatus status, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }
}
