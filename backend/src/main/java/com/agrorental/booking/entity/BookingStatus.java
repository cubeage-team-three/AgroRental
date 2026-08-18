package com.agrorental.booking.entity;

/**
 * Enumeration representing the lifecycle states of an equipment rental booking.
 */
public enum BookingStatus {
    /** Reservation requested by farmer; awaiting confirmation or system processing. */
    PENDING,

    /** Reservation confirmed; equipment availability status is set to BOOKED. */
    CONFIRMED,

    /** Reservation cancelled by farmer or partner prior to completion. */
    CANCELLED,

    /** Rental completed successfully; equipment status restored to AVAILABLE. */
    COMPLETED,

    /** Reservation rejected by partner or system administration. */
    REJECTED
}
