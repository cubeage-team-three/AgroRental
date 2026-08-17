package com.agrorental.common.exception;

import com.agrorental.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Central exception handling for all controllers.
 *
 * Handles:
 * 1. Resource not found errors       -> 404
 * 2. Bad request errors              -> 400
 * 3. Validation errors               -> 400
 * 4. Business rule errors            -> 409
 * 5. Unexpected errors               -> 500
 *
 * All errors are returned using ApiResponse
 * so the frontend receives a consistent response format.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        log.warn("Resource not found: {}", ex.getMessage());

        ApiResponse<Object> response =
                ApiResponse.error(ex.getMessage());

        return new ResponseEntity<>(
                response,
                HttpStatus.NOT_FOUND
        );
    }

    /**
     * Handles BadRequestException.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(
            BadRequestException ex) {

        log.warn("Bad request: {}", ex.getMessage());

        ApiResponse<Object> response =
                ApiResponse.error(ex.getMessage());

        return new ResponseEntity<>(
                response,
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Handles validation errors from @Valid.
     *
     * Example:
     * {
     *     "fullName": "Full name is required",
     *     "mobileNumber": "Mobile number is required"
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {

                    String fieldName =
                            ((FieldError) error).getField();

                    String errorMessage =
                            error.getDefaultMessage();

                    errors.put(fieldName, errorMessage);
                });

        log.warn("Validation failed: {}", errors);

        ApiResponse<Map<String, String>> response =
                new ApiResponse<>(
                        false,
                        "Validation Error",
                        errors
                );

        return new ResponseEntity<>(
                response,
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Handles business rule violations.
     *
     * Example:
     * Duplicate mobile number
     *
     * Returns HTTP 409 Conflict instead of 500 Internal Server Error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.warn(
                "Business rule violation: {}",
                ex.getMessage()
        );

        ApiResponse<Object> response =
                ApiResponse.error(ex.getMessage());

        return new ResponseEntity<>(
                response,
                HttpStatus.CONFLICT
        );
    }

    /**
     * Handles all unexpected exceptions.
     *
     * The actual exception is logged on the backend,
     * but a safe message is returned to the frontend.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex) {

        log.error(
                "Unhandled exception occurred",
                ex
        );

        ApiResponse<Object> response =
                ApiResponse.error(
                        "An unexpected error occurred. Please try again later."
                );

        return new ResponseEntity<>(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}