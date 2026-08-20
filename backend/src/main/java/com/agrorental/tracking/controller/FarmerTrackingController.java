package com.agrorental.tracking.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.tracking.dto.TrackingResponse;
import com.agrorental.tracking.dto.TrackingUpdateRequest;
import com.agrorental.tracking.service.LiveTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/farmers/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class FarmerTrackingController {

    private final LiveTrackingService liveTrackingService;

    @GetMapping("/{bookingId}/tracking")
    public ResponseEntity<ApiResponse<TrackingResponse>> getLiveTracking(@PathVariable Long bookingId) {
        TrackingResponse response = liveTrackingService.getTrackingByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Live tracking retrieved successfully", response));
    }

    @PutMapping("/{bookingId}/tracking")
    public ResponseEntity<ApiResponse<TrackingResponse>> updateLiveTracking(
            @PathVariable Long bookingId,
            @RequestBody TrackingUpdateRequest request) {
        TrackingResponse response = liveTrackingService.updateTracking(bookingId, request);
        return ResponseEntity.ok(ApiResponse.success("Live tracking updated successfully", response));
    }
}
