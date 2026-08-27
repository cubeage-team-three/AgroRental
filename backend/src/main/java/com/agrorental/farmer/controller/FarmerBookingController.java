package com.agrorental.farmer.controller;

import com.agrorental.booking.dto.BookingCreateRequest;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.dto.ApiResponse;
import com.agrorental.security.principal.FarmerPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farmers/bookings")
@CrossOrigin(origins = "http://localhost:5173")
public class FarmerBookingController {

    private final BookingService bookingService;

    public FarmerBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal FarmerPrincipal principal,
            @Valid @RequestBody BookingCreateRequest request) {

        request.setFarmerId(principal.getId());
        BookingResponse response = bookingService.createBooking(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking reservation created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @AuthenticationPrincipal FarmerPrincipal principal,
            @PathVariable Long id) {

        BookingResponse response = bookingService.getBookingById(id);

        if (principal != null && response.getFarmerId() != null && !response.getFarmerId().equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access is denied. You are not authorized to view another farmer's booking.");
        }

        return ResponseEntity.ok(
                ApiResponse.success("Booking retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getFarmerBookings(
            @AuthenticationPrincipal FarmerPrincipal principal,
            @RequestParam(name = "farmerId", required = false) Long paramFarmerId) {

        if (principal == null && paramFarmerId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication is required to view farmer bookings.");
        }

        Long targetFarmerId = (principal != null) ? principal.getId() : paramFarmerId;
        if (principal != null && paramFarmerId != null && !paramFarmerId.equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access is denied. You are not authorized to view another farmer's bookings.");
        }

        List<BookingResponse> response = bookingService.getBookingsByFarmer(targetFarmerId);

        return ResponseEntity.ok(
                ApiResponse.success("Farmer bookings retrieved successfully", response));
    }

    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @AuthenticationPrincipal FarmerPrincipal principal,
            @PathVariable Long id) {

        Long requestingFarmerId = principal != null ? principal.getId() : null;
        BookingResponse response = bookingService.cancelBooking(id, requestingFarmerId);

        return ResponseEntity.ok(
                ApiResponse.success("Booking cancelled successfully", response));
    }
}
