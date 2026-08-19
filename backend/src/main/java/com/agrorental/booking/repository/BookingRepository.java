package com.agrorental.booking.repository;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA Repository interface for Booking entity persistence and query execution.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Retrieves all bookings for a specific farmer.
     *
     * @param farmerId Farmer identifier
     * @return List of matching bookings
     */
    List<Booking> findByFarmerId(Long farmerId);

    /**
     * Retrieves all booking requests for equipment owned by a specific partner.
     *
     * @param partnerId Partner identifier
     * @return List of matching bookings
     */
    List<Booking> findByPartnerId(Long partnerId);

    /**
     * Retrieves all bookings for a specific piece of equipment.
     *
     * @param equipmentId Equipment identifier
     * @return List of matching bookings
     */
    List<Booking> findByEquipmentId(Long equipmentId);

    /**
     * Retrieves all bookings assigned to a specific operator.
     *
     * @param operatorId Operator identifier
     * @return List of matching bookings
     */
    List<Booking> findByOperatorId(Long operatorId);

    /**
     * Checks if any non-cancelled, non-rejected booking exists for the equipment within overlapping date ranges.
     *
     * @param equipmentId Equipment identifier
     * @param activeStatuses List of active booking statuses (e.g. PENDING, CONFIRMED)
     * @param startDate Requested start date
     * @param endDate Requested end date
     * @return true if an overlapping reservation exists; false otherwise
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.equipment.id = :equipmentId AND b.status IN :activeStatuses AND b.startDate <= :endDate AND b.endDate >= :startDate")
    boolean existsOverlappingBooking(
            @Param("equipmentId") Long equipmentId,
            @Param("activeStatuses") List<BookingStatus> activeStatuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Checks if an equipment has any active reservation matching the specified statuses.
     *
     * @param equipmentId Equipment identifier
     * @param statuses Active statuses
     * @return true if active booking exists
     */
    boolean existsByEquipmentIdAndStatusIn(Long equipmentId, List<BookingStatus> statuses);
}
