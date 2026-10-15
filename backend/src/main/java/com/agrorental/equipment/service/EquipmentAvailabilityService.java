package com.agrorental.equipment.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single owner of the equipment {@link AvailabilityStatus} writes made on behalf of the booking workflow.
 *
 * <p>Policy:
 * <ul>
 *   <li>UNDER_MAINTENANCE and INACTIVE are operational states set by the partner or an admin; booking events never
 *       change them.</li>
 *   <li>Otherwise the flag is derived from the bookings table: BOOKED while at least one booking of the equipment is
 *       in a reserving status ({@link BookingStatus#RESERVING_STATUSES}), AVAILABLE when none is. It is recomputed
 *       after every booking status change instead of being toggled, so ending one booking never frees a machine
 *       that other bookings still hold.</li>
 *   <li>BOOKED is informational ("has reservations"); whether a date range can be booked is decided by the BIZ-13
 *       overlap check, so BOOKED equipment stays discoverable and bookable for free dates.</li>
 * </ul>
 *
 * <p>Concurrency: the equipment row is locked (SELECT ... FOR UPDATE) before the bookings are counted, so
 * concurrent transitions of bookings of the same equipment are serialized. Callers run at READ COMMITTED so the
 * count sees bookings committed by the transaction that held the lock before them. Lock order everywhere is
 * booking row, then equipment row (as in BIZ-13 acceptance).
 */
@Service
public class EquipmentAvailabilityService {

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;

    public EquipmentAvailabilityService(BookingRepository bookingRepository, EquipmentRepository equipmentRepository) {
        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
    }

    /**
     * The policy as a pure function: the flag for equipment currently flagged {@code current}.
     */
    public static AvailabilityStatus derive(AvailabilityStatus current, boolean hasReservingBookings) {
        if (current == AvailabilityStatus.UNDER_MAINTENANCE || current == AvailabilityStatus.INACTIVE) {
            return current;
        }
        return hasReservingBookings ? AvailabilityStatus.BOOKED : AvailabilityStatus.AVAILABLE;
    }

    /**
     * Re-derives the equipment flag after {@code booking}'s status changed (creation included). Call it after the
     * new status is set on the booking, inside the transaction that changed it.
     *
     * @return the resulting flag, or {@code null} when the booking has no equipment
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AvailabilityStatus refreshAfterBookingChange(Booking booking) {
        if (booking == null || booking.getEquipment() == null || booking.getEquipment().getId() == null) {
            return null;
        }
        Equipment equipment = equipmentRepository.findByIdForUpdate(booking.getEquipment().getId())
                .orElse(booking.getEquipment());
        boolean reserved = (booking.getStatus() != null && booking.getStatus().isReserving())
                || hasOtherReservingBooking(equipment.getId(), booking.getId());
        AvailabilityStatus next = derive(equipment.getAvailabilityStatus(), reserved);
        equipment.setAvailabilityStatus(next);
        equipmentRepository.save(equipment);
        return next;
    }

    /** Whether any booking of the equipment currently holds it (reserving status). */
    public boolean hasReservingBookings(Long equipmentId) {
        return bookingRepository.existsByEquipmentIdAndStatusIn(equipmentId, BookingStatus.RESERVING_STATUSES);
    }

    /** Whether the equipment has any booking at all, past ones included. */
    public boolean hasAnyBookings(Long equipmentId) {
        return bookingRepository.existsByEquipmentId(equipmentId);
    }

    private boolean hasOtherReservingBooking(Long equipmentId, Long bookingId) {
        if (bookingId == null) {
            return hasReservingBookings(equipmentId);
        }
        return bookingRepository.existsOtherBookingInStatuses(equipmentId, bookingId, BookingStatus.RESERVING_STATUSES);
    }
}
