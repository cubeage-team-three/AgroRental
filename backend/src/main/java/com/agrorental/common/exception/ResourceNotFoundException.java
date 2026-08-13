package com.agrorental.common.exception;

/**
 * Thrown when a requested resource (entity, record, etc.) cannot be found.
 * Handled by GlobalExceptionHandler and mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
