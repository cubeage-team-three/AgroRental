package com.agrorental.review.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.review.dto.RatingSummaryResponse;
import com.agrorental.review.dto.ReviewCreateRequest;
import com.agrorental.review.dto.ReviewResponse;
import com.agrorental.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing reviews and aggregate star ratings.
 */
@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://localhost:5176"
})
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse res = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Review submitted successfully", res));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success("Review retrieved successfully", reviewService.getReviewByBookingId(bookingId)));
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsForEquipment(@PathVariable Long equipmentId) {
        return ResponseEntity.ok(ApiResponse.success("Equipment reviews retrieved successfully", reviewService.getReviewsForEquipment(equipmentId)));
    }

    @GetMapping("/equipment/{equipmentId}/summary")
    public ResponseEntity<ApiResponse<RatingSummaryResponse>> getRatingSummaryForEquipment(@PathVariable Long equipmentId) {
        return ResponseEntity.ok(ApiResponse.success("Equipment rating summary retrieved successfully", reviewService.getRatingSummaryForEquipment(equipmentId)));
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsForPartner(@PathVariable Long partnerId) {
        return ResponseEntity.ok(ApiResponse.success("Partner reviews retrieved successfully", reviewService.getReviewsForPartner(partnerId)));
    }

    @GetMapping("/partner/{partnerId}/summary")
    public ResponseEntity<ApiResponse<RatingSummaryResponse>> getRatingSummaryForPartner(@PathVariable Long partnerId) {
        return ResponseEntity.ok(ApiResponse.success("Partner rating summary retrieved successfully", reviewService.getRatingSummaryForPartner(partnerId)));
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsForFarmer(@PathVariable Long farmerId) {
        return ResponseEntity.ok(ApiResponse.success("Farmer reviews retrieved successfully", reviewService.getReviewsForFarmer(farmerId)));
    }
}
