package com.agrorental.booking.entity;

import java.util.List;

/**
 * Enumeration representing the lifecycle states of an equipment rental booking.
 */
public enum BookingStatus {
    /** Reservation requested by farmer; awaiting confirmation or system processing. */
    PENDING,

    /** Reservation accepted by partner. */
    ACCEPTED,

    /** Reservation confirmed; the equipment shows BOOKED while it holds a reservation. */
    CONFIRMED,

    /** Certified operator assigned to confirmed booking. */
    OPERATOR_ASSIGNED,

    /** Machine dispatched and operator en route to field location. */
    ON_THE_WAY,

    /** Machine in field and work actively started. */
    WORK_STARTED,

    /** Rental completed successfully; the equipment is AVAILABLE again unless other reservations remain. */
    COMPLETED,

    /** Reservation cancelled by farmer or partner prior to completion. */
    CANCELLED,

    /** Reservation rejected by partner or system administration. */
    REJECTED;

    /**
     * Statuses that hold the equipment for their date range (BIZ-13): a new booking may not overlap any of them,
     * date-range search hides equipment with such an overlap, and the equipment shows BOOKED while at least one
     * of its bookings is in one of them. Single source of truth for all three rules.
     */
    public static final List<BookingStatus> RESERVING_STATUSES = List.of(PENDING, ACCEPTED, CONFIRMED,
            OPERATOR_ASSIGNED, ON_THE_WAY, WORK_STARTED);

    /** Whether a booking in this status holds the equipment for its dates (see {@link #RESERVING_STATUSES}). */
    public boolean isReserving() {
        return RESERVING_STATUSES.contains(this);
    }
}
