package com.agrorental.admin.controller;

import com.agrorental.admin.dto.AdminBookingStatsResponse;
import com.agrorental.admin.service.AdminBookingService;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller exposing Administrator endpoints for Booking overview, filtering, and lifecycle management.
 * Protected by SecurityConfig's "/api/admin/**" -> hasRole("ADMIN") rule and class-level @PreAuthorize.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    /**
     * Lists bookings platform-wide with optional search, status, date filtering, and pagination.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getBookings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("Admin listing bookings with search={}, status={}, page={}", search, status, pageable.getPageNumber());
        Page<BookingResponse> page = adminBookingService.getBookings(search, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", page));
    }

    /**
     * Retrieves aggregate booking statistics for the summary metrics cards.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminBookingStatsResponse>> getStats() {
        log.debug("Admin fetching booking statistics");
        AdminBookingStatsResponse stats = adminBookingService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Booking stats retrieved successfully", stats));
    }

    /**
     * Retrieves full booking details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long id) {
        log.debug("Admin fetching booking details for ID: {}", id);
        BookingResponse booking = adminBookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", booking));
    }

    /**
     * Updates booking status or assigns an operator following existing lifecycle rules.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusUpdateRequest request) {

        log.info("Admin updating booking ID {} status to {}", id, request.getStatus());
        BookingResponse updated = adminBookingService.updateBookingStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Booking status updated successfully", updated));
    }

    /**
     * Cancels an existing booking reservation, releasing equipment and cleaning up operator assignments.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Long id) {
        log.info("Admin cancelling booking ID {}", id);
        BookingResponse cancelled = adminBookingService.cancelBooking(id);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", cancelled));
    }
}
