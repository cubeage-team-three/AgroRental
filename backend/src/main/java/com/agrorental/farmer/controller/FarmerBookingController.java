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
            @PathVariable Long id) {

        BookingResponse response = bookingService.getBookingById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Booking retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getFarmerBookings(
            @AuthenticationPrincipal FarmerPrincipal principal) {

        List<BookingResponse> response = bookingService.getBookingsByFarmer(principal.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Farmer bookings retrieved successfully", response));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id) {

        BookingResponse response = bookingService.cancelBooking(id);

        return ResponseEntity.ok(
                ApiResponse.success("Booking cancelled successfully", response));
    }
}
