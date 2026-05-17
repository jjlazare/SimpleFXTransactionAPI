package com.example.wex.exception;

import com.example.wex.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        ErrorResponse err = new ErrorResponse();
        err.setError(ex.getCode() != null ? ex.getCode() : "VALIDATION_ERROR");
        err.setMessage(ex.getMessage());

        // Treasury reference cache unavailable (startup load failed or not yet loaded)
        if ("TREASURY_UNAVAILABLE".equals(ex.getCode()) || "TREASURY_REFERENCE_LOAD_FAILED".equals(ex.getCode())) {
            log.error("Treasury API off-line", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(err);
        }

        // Treasury API call failure (external dependency)
        if ("TREASURY_API_ERROR".equals(ex.getCode())) {
            log.error("Treasury API error", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(err);
        }

        // Default validation/conversion errors
        log.error("Treasury API kicked back bad request", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        ErrorResponse err = new ErrorResponse();
        err.setError(ex.getCode() != null ? ex.getCode() : "NOT_FOUND");
        err.setMessage(ex.getMessage());

        log.error("API match not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        ErrorResponse err = new ErrorResponse();
        err.setError("INTERNAL_ERROR");
        err.setMessage("An unexpected error occurred");

        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        ErrorResponse err = new ErrorResponse();
        err.setError("NOT_FOUND");
        err.setMessage("Spring resource not found");

        log.error("Spring resource not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }
}