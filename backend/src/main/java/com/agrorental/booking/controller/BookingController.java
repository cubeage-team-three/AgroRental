package com.agrorental.booking.controller;

import com.agrorental.booking.dto.BookingCreateRequest;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.dto.ApiResponse;
import com.agrorental.security.principal.PartnerPrincipal;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller exposing HTTP APIs for Equipment Rental Booking module.
 */
@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:5173")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    private void validatePartnerOwnership(Long partnerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof PartnerPrincipal partnerPrincipal) {
            if (partnerPrincipal.getId().equals(partnerId)) {
                return;
            }
        }

        throw new ForbiddenException("Access is denied. You do not have the required permissions.");
    }

    /**
     * Creates a new machinery rental reservation for a farmer.
     *
     * @param request Validated booking creation payload
     * @param principal Authenticated farmer principal (optional, extracted from JWT)
     * @return ResponseEntity containing HTTP 201 Created and BookingResponse payload
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal com.agrorental.security.principal.FarmerPrincipal principal,
            @Valid @RequestBody BookingCreateRequest request) {

        if (principal != null && request.getFarmerId() == null) {
            request.setFarmerId(principal.getId());
        }

        BookingResponse response = bookingService.createBooking(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking reservation created successfully", response));
    }

    /**
     * Retrieves a booking reservation by its primary key.
     *
     * @param id Booking identifier
     * @return ResponseEntity containing HTTP 200 OK and BookingResponse payload
     */
    /**
     * Retrieves a booking reservation by its primary key.
     *
     * @param id Booking identifier
     * @param principal Authenticated principal (optional for admin, checked for farmer/partner/operator)
     * @return ResponseEntity containing HTTP 200 OK and BookingResponse payload
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal Object principal) {

        BookingResponse response = bookingService.getBookingById(id);

        if (principal instanceof com.agrorental.security.principal.FarmerPrincipal farmerPrincipal) {
            if (response.getFarmerId() != null && !response.getFarmerId().equals(farmerPrincipal.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access is denied. You are not authorized to view another farmer's booking.");
            }
        }

        return ResponseEntity.ok(
                ApiResponse.success("Booking retrieved successfully", response));
    }

    /**
     * Retrieves all bookings created by a specific farmer.
     *
     * @param farmerId Farmer identifier
     * @param principal Authenticated farmer principal
     * @return ResponseEntity containing HTTP 200 OK and List of BookingResponse
     */
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByFarmer(
            @PathVariable Long farmerId,
            @AuthenticationPrincipal com.agrorental.security.principal.FarmerPrincipal principal) {

        Long targetFarmerId = (principal != null) ? principal.getId() : farmerId;
        if (principal != null && !principal.getId().equals(farmerId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access is denied. You are not authorized to view another farmer's bookings.");
        }
        List<BookingResponse> response = bookingService.getBookingsByFarmer(targetFarmerId);

        return ResponseEntity.ok(
                ApiResponse.success("Farmer bookings retrieved successfully", response));
    }

    /**
     * Retrieves all booking requests for equipment owned by a specific partner.
     *
     * @param partnerId Partner identifier
     * @return ResponseEntity containing HTTP 200 OK and List of BookingResponse
     */
    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByPartner(
            @PathVariable Long partnerId) {

        validatePartnerOwnership(partnerId);
        List<BookingResponse> response = bookingService.getBookingsByPartner(partnerId);

        return ResponseEntity.ok(
                ApiResponse.success("Partner bookings retrieved successfully", response));
    }

    /**
     * Retrieves all booking reservations assigned to a specific operator.
     *
     * @param operatorId Operator identifier
     * @return ResponseEntity containing HTTP 200 OK and List of BookingResponse
     */
    @GetMapping("/operator/{operatorId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByOperator(
            @PathVariable Long operatorId) {

        List<BookingResponse> response = bookingService.getBookingsByOperator(operatorId);

        return ResponseEntity.ok(
                ApiResponse.success("Operator bookings retrieved successfully", response));
    }

    /**
     * Cancels an existing booking reservation.
     *
     * @param id Booking identifier
     * @param principal Authenticated principal
     * @return ResponseEntity containing HTTP 200 OK and updated BookingResponse
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal Object principal) {

        Long farmerId = null;
        if (principal instanceof com.agrorental.security.principal.FarmerPrincipal farmerPrincipal) {
            farmerId = farmerPrincipal.getId();
        }

        BookingResponse response = bookingService.cancelBooking(id, farmerId);

        return ResponseEntity.ok(
                ApiResponse.success("Booking cancelled successfully", response));
    }

    /**
     * Updates the status of a booking or assigns an operator.
     *
     * @param id Booking identifier
     * @param request Status update payload
     * @return ResponseEntity containing HTTP 200 OK and updated BookingResponse
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusUpdateRequest request) {

        BookingResponse response = bookingService.updateBookingStatus(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Booking status updated successfully", response));
    }

    /**
     * Partner accepts a pending booking request.
     *
     * @param id Booking identifier
     * @param principal Authenticated partner, resolved from the JWT (not a client-supplied header/param)
     * @return ResponseEntity containing HTTP 200 OK and updated BookingResponse
     */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<BookingResponse>> acceptBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal PartnerPrincipal principal) {

        BookingResponse response = bookingService.acceptBooking(id, principal.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Booking request accepted successfully", response));
    }

    /**
     * Partner rejects a booking request with a mandatory reason.
     *
     * @param id Booking identifier
     * @param payload Request body containing rejectionReason
     * @param principal Authenticated partner, resolved from the JWT (not a client-supplied header/param)
     * @return ResponseEntity containing HTTP 200 OK and updated BookingResponse
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectBooking(
            @PathVariable Long id,
            @RequestBody(required = false) BookingStatusUpdateRequest payload,
            @AuthenticationPrincipal PartnerPrincipal principal) {

        String reason = payload != null ? payload.getRejectionReason() : null;
        BookingResponse response = bookingService.rejectBooking(id, principal.getId(), reason);

        return ResponseEntity.ok(
                ApiResponse.success("Booking request declined successfully", response));
    }

    /**
     * Partner assigns a qualified operator to a booking.
     *
     * @param id Booking identifier
     * @param payload Request body containing operatorId
     * @param principal Authenticated partner, resolved from the JWT (not a client-supplied header/param)
     * @return ResponseEntity containing HTTP 200 OK and updated BookingResponse
     */
    @PatchMapping("/{id}/assign-operator")
    public ResponseEntity<ApiResponse<BookingResponse>> assignOperator(
            @PathVariable Long id,
            @RequestBody BookingStatusUpdateRequest payload,
            @AuthenticationPrincipal PartnerPrincipal principal) {

        Long operatorId = payload != null ? payload.getOperatorId() : null;
        BookingResponse response = bookingService.assignOperator(id, principal.getId(), operatorId);

        return ResponseEntity.ok(
                ApiResponse.success("Operator assigned successfully", response));
    }
}
