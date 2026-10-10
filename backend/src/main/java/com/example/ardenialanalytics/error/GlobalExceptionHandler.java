package com.example.ardenialanalytics.error;

import com.example.ardenialanalytics.claim.InvalidDateRangeException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ApiError> handleInvalidDateRange() {
        return ResponseEntity.badRequest().body(
                new ApiError("INVALID_DATE_RANGE", "startDate must be on or before endDate"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        if ("startDate".equals(exception.getName()) || "endDate".equals(exception.getName())) {
            return ResponseEntity.badRequest().body(
                    new ApiError("INVALID_DATE_FORMAT", "Dates must use the YYYY-MM-DD format"));
        }
        return ResponseEntity.badRequest().body(
                new ApiError("INVALID_QUERY_PARAMETER", "A query parameter has an invalid value"));
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class,
            MethodArgumentNotValidException.class})
    public ResponseEntity<ApiError> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(
                new ApiError("INVALID_QUERY_PARAMETER", "Check page and size values; size must be between 1 and 100"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        logger.error("Unexpected request error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_SERVER_ERROR", "An unexpected server error occurred"));
    }
}
