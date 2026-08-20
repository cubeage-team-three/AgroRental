package com.agrorental.review.controller;

import com.agrorental.review.dto.RatingSummaryResponse;
import com.agrorental.review.dto.ReviewCreateRequest;
import com.agrorental.review.dto.ReviewResponse;
import com.agrorental.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing reviews and aggregate star ratings.
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.ok(reviewService.createReview(request));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ReviewResponse> getReviewByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(reviewService.getReviewByBookingId(bookingId));
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForEquipment(@PathVariable Long equipmentId) {
        return ResponseEntity.ok(reviewService.getReviewsForEquipment(equipmentId));
    }

    @GetMapping("/equipment/{equipmentId}/summary")
    public ResponseEntity<RatingSummaryResponse> getRatingSummaryForEquipment(@PathVariable Long equipmentId) {
        return ResponseEntity.ok(reviewService.getRatingSummaryForEquipment(equipmentId));
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForPartner(@PathVariable Long partnerId) {
        return ResponseEntity.ok(reviewService.getReviewsForPartner(partnerId));
    }

    @GetMapping("/partner/{partnerId}/summary")
    public ResponseEntity<RatingSummaryResponse> getRatingSummaryForPartner(@PathVariable Long partnerId) {
        return ResponseEntity.ok(reviewService.getRatingSummaryForPartner(partnerId));
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForFarmer(@PathVariable Long farmerId) {
        return ResponseEntity.ok(reviewService.getReviewsForFarmer(farmerId));
    }
}
