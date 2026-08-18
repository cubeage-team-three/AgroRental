package com.agrorental.booking.exception;

/**
 * Thrown when requested equipment is disabled, not marked AVAILABLE, or
 * already booked for an overlapping date range.
 * Handled by GlobalExceptionHandler and mapped to HTTP 409 Conflict — the
 * request itself is well-formed, but it conflicts with the equipment's
 * current state, which is what 409 (rather than 400) signals.
 */
public class EquipmentNotAvailableException extends RuntimeException {

    public EquipmentNotAvailableException(String message) {
        super(message);
    }
}
