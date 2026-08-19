package com.agrorental.booking.exception;

/**
 * Exception thrown when an equipment reservation is requested for a machine that is unavailable or already booked.
 */
public class EquipmentNotAvailableException extends RuntimeException {

    public EquipmentNotAvailableException(String message) {
        super(message);
    }
}
