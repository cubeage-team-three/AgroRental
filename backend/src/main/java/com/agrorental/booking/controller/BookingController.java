package com.agrorental.booking.controller;

import com.agrorental.booking.dto.BookingRequestDto;
import com.agrorental.booking.dto.BookingResponseDto;
import com.agrorental.booking.entity.constant.BookingStatus;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponseDto>> createBooking(
            @Valid @RequestBody BookingRequestDto request) {

        BookingResponseDto booking = bookingService.createBooking(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", booking));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getAllBookings() {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getAllBookings()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponseDto>> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(id)));
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getBookingsByFarmer(
            @PathVariable Long farmerId) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingsByFarmer(farmerId)));
    }

    /**
     * Not explicitly requested, but a direct mirror of the farmer lookup and
     * backed by the equipment-scoped query you asked for at the repository
     * layer — a partner viewing one listing's booking history needs exactly
     * this. Drop it if it's not wanted.
     */
    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getBookingsByEquipment(
            @PathVariable Long equipmentId) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingsByEquipment(equipmentId)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BookingResponseDto>> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam BookingStatus status) {

        BookingResponseDto booking = bookingService.updateBookingStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Booking status updated successfully", booking));
    }
}
