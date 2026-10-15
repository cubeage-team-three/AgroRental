package com.agrorental.admin.service;

import com.agrorental.admin.dto.AdminBookingStatsResponse;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.partner.entity.Partner;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRefundRepository;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.payment.service.RevenueCalculator;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing Administrator queries, aggregate statistics, and administrative actions for Bookings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    /** Refund records (BIZ-20); headline revenue is net of completed refunds (see RevenueCalculator). */
    @Autowired(required = false)
    private PaymentRefundRepository paymentRefundRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;

    /**
     * Retrieves a paginated and filtered list of bookings platform-wide.
     *
     * @param search Optional search term matching booking ID, equipment name, partner name, or delivery address
     * @param status Optional booking status filter
     * @param startDate Optional filter for bookings ending on or after this date
     * @param endDate Optional filter for bookings starting on or before this date
     * @param pageable Pagination and sorting criteria
     * @return Page of BookingResponse DTOs
     */
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookings(
            String search,
            BookingStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        log.debug("Admin listing bookings: search='{}', status={}, startDate={}, endDate={}, page={}",
                search, status, startDate, endDate, pageable.getPageNumber());

        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                List<Predicate> searchPredicates = new ArrayList<>();

                try {
                    Long numericId = Long.parseLong(search.trim());
                    searchPredicates.add(cb.equal(root.get("id"), numericId));
                    searchPredicates.add(cb.equal(root.get("farmerId"), numericId));
                } catch (NumberFormatException ignored) {}

                Join<Booking, Equipment> equipmentJoin = root.join("equipment", JoinType.LEFT);
                searchPredicates.add(cb.like(cb.lower(equipmentJoin.get("name")), term));
                searchPredicates.add(cb.like(cb.lower(equipmentJoin.get("model")), term));

                Join<Booking, Partner> partnerJoin = root.join("partner", JoinType.LEFT);
                searchPredicates.add(cb.like(cb.lower(partnerJoin.get("fullName")), term));
                searchPredicates.add(cb.like(cb.lower(partnerJoin.get("businessName")), term));

                searchPredicates.add(cb.like(cb.lower(root.get("deliveryAddress")), term));

                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), endDate));
            }

            if (query != null && Long.class != query.getResultType()) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return bookingRepository.findAll(spec, pageable).map(bookingMapper::toResponse);
    }

    /**
     * Aggregates platform-wide booking metrics for administrative dashboard cards.
     *
     * @return Populated AdminBookingStatsResponse DTO
     */
    @Transactional(readOnly = true)
    public AdminBookingStatsResponse getStats() {
        long total = bookingRepository.count();
        long pending = bookingRepository.countByStatus(BookingStatus.PENDING);
        long active = bookingRepository.countByStatusIn(List.of(
                BookingStatus.ACCEPTED,
                BookingStatus.CONFIRMED,
                BookingStatus.OPERATOR_ASSIGNED,
                BookingStatus.ON_THE_WAY,
                BookingStatus.WORK_STARTED
        ));
        long completed = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long cancelled = bookingRepository.countByStatusIn(List.of(
                BookingStatus.CANCELLED,
                BookingStatus.REJECTED
        ));

        RevenueCalculator.Totals revenue = RevenueCalculator.platformTotals(
                paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS), paymentRefundRepository);
        BigDecimal totalAmount = bookingRepository.sumTotalCostByStatusNotIn(List.of(
                BookingStatus.CANCELLED,
                BookingStatus.REJECTED
        ));

        return AdminBookingStatsResponse.builder()
                .totalBookings(total)
                .pendingBookings(pending)
                .activeBookings(active)
                .completedBookings(completed)
                .cancelledBookings(cancelled)
                .totalRevenue(revenue.netRevenue())
                .grossCollected(revenue.grossCollected())
                .refundsPendingAmount(revenue.refundsPendingAmount())
                .refundsPendingCount(revenue.refundsPendingCount())
                .refundsFailedAmount(revenue.refundsFailedAmount())
                .refundsFailedCount(revenue.refundsFailedCount())
                .refundsCompletedAmount(revenue.refundsCompletedAmount())
                .refundsCompletedCount(revenue.refundsCompletedCount())
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .build();
    }

    /**
     * Retrieves a single booking reservation with full details.
     *
     * @param id Booking identifier
     * @return BookingResponse DTO
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        return bookingService.getBookingById(id);
    }

    /**
     * Updates booking status or assigns an operator via existing lifecycle validation.
     * Runs at READ COMMITTED (this transaction is the one BookingService joins) so the overlap check and the
     * equipment availability recount see bookings committed by whoever held the equipment lock before.
     *
     * @param id Booking identifier
     * @param request Status update payload
     * @return Updated BookingResponse DTO
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse updateBookingStatus(Long id, BookingStatusUpdateRequest request) {
        return bookingService.updateBookingStatus(id, request);
    }

    /**
     * Cancels an existing booking reservation, releasing equipment and cleaning up operator assignments.
     * READ COMMITTED for the same reason as {@link #updateBookingStatus}.
     *
     * @param id Booking identifier
     * @return Updated BookingResponse DTO
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse cancelBooking(Long id) {
        return bookingService.cancelBooking(id);
    }
}
