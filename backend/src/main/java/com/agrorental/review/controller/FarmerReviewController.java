package com.agrorental.review.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.review.dto.ReviewCreateRequest;
import com.agrorental.review.dto.ReviewResponse;
import com.agrorental.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farmers")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class FarmerReviewController {

    private final ReviewService reviewService;

    @PostMapping("/bookings/{bookingId}/review")
    public ResponseEntity<ApiResponse<ReviewResponse>> createBookingReview(
            @PathVariable Long bookingId,
            @Valid @RequestBody ReviewCreateRequest request) {
        request.setBookingId(bookingId);
        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.ok(ApiResponse.success("Review submitted successfully", response));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getFarmerReviews(
            @RequestParam(defaultValue = "1") Long farmerId) {
        List<ReviewResponse> response = reviewService.getReviewsForFarmer(farmerId);
        return ResponseEntity.ok(ApiResponse.success("Farmer reviews retrieved successfully", response));
    }
}
