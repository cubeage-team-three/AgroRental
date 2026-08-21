package com.agrorental.operator.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.dto.OperatorRatingSummaryResponse;
import com.agrorental.operator.dto.OperatorReviewCreateRequest;
import com.agrorental.operator.dto.OperatorReviewResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorReview;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.repository.OperatorReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing operator star ratings, customer feedback reviews, and rating aggregation calculations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorReviewService {

    private final OperatorReviewRepository reviewRepository;
    private final OperatorJobAssignmentRepository assignmentRepository;
    private final OperatorRepository operatorRepository;
    private final FarmerRepository farmerRepository;

    /**
     * Submits a verified rating and review for a completed operator job assignment.
     */
    @Transactional
    public OperatorReviewResponse createOperatorReview(Long assignmentId, Long farmerId, OperatorReviewCreateRequest request) {
        log.info("Farmer {} submitting review for assignment {}", farmerId, assignmentId);

        if (request == null || request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be an integer between 1 and 5 stars");
        }

        OperatorJobAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Job assignment not found with ID: " + assignmentId));

        // 1. Must be in COMPLETED status
        if (assignment.getAssignmentStatus() != OperatorAssignmentStatus.COMPLETED) {
            log.warn("Review rejected: Assignment {} is not completed (current status: {})",
                    assignmentId, assignment.getAssignmentStatus());
            throw new BadRequestException("Reviews are only allowed after job completion. Current status: " + assignment.getAssignmentStatus());
        }

        // 2. Ownership verification: Only the booking's farmer can review
        if (assignment.getBooking() == null || !assignment.getBooking().getFarmerId().equals(farmerId)) {
            log.warn("IDOR violation: Farmer {} attempted to review assignment {} belonging to farmer {}",
                    farmerId, assignmentId, assignment.getBooking() != null ? assignment.getBooking().getFarmerId() : null);
            throw new ForbiddenException("Access denied: You are not authorized to review this assignment");
        }

        // 3. Prevent operator self-review
        if (assignment.getOperator().getId().equals(farmerId)) {
            log.warn("Self-review rejected: Operator {} attempted to review own assignment {}", farmerId, assignmentId);
            throw new ForbiddenException("Operators cannot submit reviews for assignments");
        }

        // 4. Duplicate review prevention (One review per completed assignment)
        if (reviewRepository.existsByAssignmentId(assignmentId)) {
            log.warn("Duplicate review attempt for assignment {}", assignmentId);
            throw new BadRequestException("A review has already been submitted for this assignment");
        }

        OperatorReview review = OperatorReview.builder()
                .assignment(assignment)
                .operator(assignment.getOperator())
                .booking(assignment.getBooking())
                .farmerId(farmerId)
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : null)
                .build();

        OperatorReview saved = reviewRepository.save(review);
        log.info("Successfully recorded review ID {} with {}-star rating for operator {}",
                saved.getId(), saved.getRating(), saved.getOperator().getId());

        return mapToResponse(saved);
    }

    /**
     * Computes the average rating and star distribution metrics for an operator.
     */
    @Transactional(readOnly = true)
    public OperatorRatingSummaryResponse getOperatorRatingSummary(Long operatorId) {
        log.info("Fetching rating summary for operator ID: {}", operatorId);

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operatorId));

        List<OperatorReview> reviews = reviewRepository.findByOperatorId(operatorId);
        long totalReviews = reviews.size();

        long c1 = 0L, c2 = 0L, c3 = 0L, c4 = 0L, c5 = 0L;
        long ratingSum = 0L;

        for (OperatorReview rev : reviews) {
            int r = rev.getRating() != null ? rev.getRating() : 0;
            ratingSum += r;
            switch (r) {
                case 1 -> c1++;
                case 2 -> c2++;
                case 3 -> c3++;
                case 4 -> c4++;
                case 5 -> c5++;
            }
        }

        double averageRating = 0.0;
        if (totalReviews > 0) {
            averageRating = (double) Math.round((ratingSum / (double) totalReviews) * 10.0) / 10.0;
        }

        return OperatorRatingSummaryResponse.builder()
                .operatorId(operator.getId())
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .fiveStarCount(c5)
                .fourStarCount(c4)
                .threeStarCount(c3)
                .twoStarCount(c2)
                .oneStarCount(c1)
                .build();
    }

    /**
     * Retrieves paginated reviews and feedback for an operator.
     */
    @Transactional(readOnly = true)
    public Page<OperatorReviewResponse> getOperatorReviews(Long operatorId, Pageable pageable) {
        log.info("Fetching reviews page for operator ID: {}", operatorId);

        if (!operatorRepository.existsById(operatorId)) {
            throw new ResourceNotFoundException("Operator not found with ID: " + operatorId);
        }

        return reviewRepository.findByOperatorId(operatorId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Retrieves the review for a specific assignment if one exists.
     */
    @Transactional(readOnly = true)
    public OperatorReviewResponse getAssignmentReview(Long assignmentId) {
        return reviewRepository.findByAssignmentId(assignmentId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No review found for assignment ID: " + assignmentId));
    }

    /**
     * Helper mapper for OperatorReview entity to OperatorReviewResponse DTO.
     */
    public OperatorReviewResponse mapToResponse(OperatorReview review) {
        String farmerName = "Verified Farmer #" + review.getFarmerId();
        try {
            Farmer farmer = farmerRepository.findById(review.getFarmerId()).orElse(null);
            if (farmer != null && farmer.getFullName() != null) {
                farmerName = farmer.getFullName();
            }
        } catch (Exception e) {
            log.debug("Could not resolve farmer name for ID {}: {}", review.getFarmerId(), e.getMessage());
        }

        return OperatorReviewResponse.builder()
                .reviewId(review.getId())
                .assignmentId(review.getAssignment().getId())
                .bookingId(review.getBooking().getId())
                .operatorId(review.getOperator().getId())
                .operatorName(review.getOperator().getFullName())
                .farmerId(review.getFarmerId())
                .farmerName(farmerName)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
