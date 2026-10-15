package com.agrorental.booking.repository;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for Booking entity persistence and query execution.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") Long id);

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
     * Same overlap check, ignoring one booking (the booking being transitioned must not conflict with itself).
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.equipment.id = :equipmentId AND b.id <> :excludedBookingId "
            + "AND b.status IN :activeStatuses AND b.startDate <= :endDate AND b.endDate >= :startDate")
    boolean existsOverlappingBookingExcluding(
            @Param("equipmentId") Long equipmentId,
            @Param("activeStatuses") List<BookingStatus> activeStatuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedBookingId") Long excludedBookingId);

    /**
     * Checks if an equipment has any active reservation matching the specified statuses.
     *
     * @param equipmentId Equipment identifier
     * @param statuses Active statuses
     * @return true if active booking exists
     */
    boolean existsByEquipmentIdAndStatusIn(Long equipmentId, List<BookingStatus> statuses);

    /**
     * Whether a booking other than {@code excludedBookingId} of the equipment is in one of the statuses.
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.equipment.id = :equipmentId AND b.id <> :excludedBookingId "
            + "AND b.status IN :statuses")
    boolean existsOtherBookingInStatuses(
            @Param("equipmentId") Long equipmentId,
            @Param("excludedBookingId") Long excludedBookingId,
            @Param("statuses") List<BookingStatus> statuses);

    /** Whether the equipment has any booking at all (history included). */
    boolean existsByEquipmentId(Long equipmentId);

    /**
     * Retrieves the most recently created bookings platform-wide, for the
     * admin dashboard's recent activity feed.
     *
     * @return Up to 10 most recent bookings, newest first
     */
    List<Booking> findTop10ByOrderByCreatedAtDesc();

    long countByStatus(BookingStatus status);

    long countByStatusIn(Collection<BookingStatus> statuses);

    @Query("SELECT COALESCE(SUM(b.totalCost), 0) FROM Booking b WHERE b.status NOT IN :excludedStatuses")
    BigDecimal sumTotalCostByStatusNotIn(@Param("excludedStatuses") Collection<BookingStatus> excludedStatuses);
}
