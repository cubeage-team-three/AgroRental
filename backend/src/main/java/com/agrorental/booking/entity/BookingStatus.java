package com.agrorental.booking.entity;

/**
 * Enumeration representing the lifecycle states of an equipment rental booking.
 */
public enum BookingStatus {
    /** Reservation requested by farmer; awaiting confirmation or system processing. */
    PENDING,

    /** Reservation accepted by partner. */
    ACCEPTED,

    /** Reservation confirmed; equipment availability status is set to BOOKED. */
    CONFIRMED,

    /** Certified operator assigned to confirmed booking. */
    OPERATOR_ASSIGNED,

    /** Machine in field and work actively started. */
    WORK_STARTED,

    /** Rental completed successfully; equipment status restored to AVAILABLE. */
    COMPLETED,

    /** Reservation cancelled by farmer or partner prior to completion. */
    CANCELLED,

    /** Reservation rejected by partner or system administration. */
    REJECTED
}
