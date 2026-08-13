package com.agrorental.common.exception;

/**
 * Thrown for invalid client input or failed business-rule validation.
 * Handled by GlobalExceptionHandler and mapped to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
