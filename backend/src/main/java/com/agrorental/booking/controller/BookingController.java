package com.agrorental.booking.controller;

import com.agrorental.booking.dto.BookingCreateRequest;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * Creates a new machinery rental reservation for a farmer.
     *
     * @param request Validated booking creation payload
     * @return ResponseEntity containing HTTP 201 Created and BookingResponse payload
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingCreateRequest request) {

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
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Long id) {

        BookingResponse response = bookingService.getBookingById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Booking retrieved successfully", response));
    }

    /**
     * Retrieves all bookings created by a specific farmer.
     *
     * @param farmerId Farmer identifier
     * @return ResponseEntity containing HTTP 200 OK and List of BookingResponse
     */
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByFarmer(
            @PathVariable Long farmerId) {

        List<BookingResponse> response = bookingService.getBookingsByFarmer(farmerId);

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
     * @return ResponseEntity containing HTTP 200 OK and updated BookingResponse
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id) {

        BookingResponse response = bookingService.cancelBooking(id);

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
}
