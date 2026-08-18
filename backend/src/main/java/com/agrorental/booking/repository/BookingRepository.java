package com.agrorental.booking.repository;

import com.agrorental.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByFarmerId(Long farmerId);

    List<Booking> findAllByEquipmentId(Long equipmentId);

    List<Booking> findAllByPartnerId(Long partnerId);

    List<Booking> findAllByOperatorId(Long operatorId);

    /**
     * Overlap check for a single piece of equipment: two ranges [s1,e1] and
     * [s2,e2] overlap when s1 <= e2 AND e1 >= s2. Cancelled bookings don't
     * hold the equipment, so they're excluded from the conflict check.
     */
    @Query("""
            SELECT COUNT(b) > 0
            FROM Booking b
            WHERE b.equipment.id = :equipmentId
              AND b.status <> com.agrorental.booking.entity.constant.BookingStatus.CANCELLED
              AND b.startDate <= :endDate
              AND b.endDate >= :startDate
            """)
    boolean existsOverlappingBooking(
            @Param("equipmentId") Long equipmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
