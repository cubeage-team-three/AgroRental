package com.agrorental.operator;

import com.agrorental.booking.entity.Booking;
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
import com.agrorental.operator.service.OperatorReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorReviewService Unit Tests")
class OperatorReviewServiceTest {

    @Mock
    private OperatorReviewRepository reviewRepository;

    @Mock
    private OperatorJobAssignmentRepository assignmentRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @InjectMocks
    private OperatorReviewService reviewService;

    private Operator operator;
    private Booking booking;
    private OperatorJobAssignment assignment;
    private Farmer farmer;

    @BeforeEach
    void setUp() {
        operator = new Operator();
        operator.setId(1L);
        operator.setFullName("Santosh Gaikwad");

        farmer = new Farmer();
        farmer.setId(10L);
        farmer.setFullName("Ramesh Patil");

        booking = Booking.builder()
                .farmerId(10L)
                .build();
        booking.setId(200L);

        assignment = OperatorJobAssignment.builder()
                .operator(operator)
                .booking(booking)
                .assignmentStatus(OperatorAssignmentStatus.COMPLETED)
                .workStartedAt(LocalDateTime.now().minusHours(6))
                .completedAt(LocalDateTime.now())
                .build();
        assignment.setId(100L);
    }

    @Test
    @DisplayName("Submit valid 5-star review for completed assignment succeeds")
    void testCreateOperatorReview_success() {
        OperatorReviewCreateRequest request = OperatorReviewCreateRequest.builder()
                .rating(5)
                .comment("Excellent work and polite operator.")
                .build();

        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(reviewRepository.existsByAssignmentId(100L)).thenReturn(false);
        when(farmerRepository.findById(10L)).thenReturn(Optional.of(farmer));
        when(reviewRepository.save(any(OperatorReview.class))).thenAnswer(inv -> {
            OperatorReview saved = inv.getArgument(0);
            saved.setId(50L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        OperatorReviewResponse response = reviewService.createOperatorReview(100L, 10L, request);

        assertNotNull(response);
        assertEquals(50L, response.getReviewId());
        assertEquals(100L, response.getAssignmentId());
        assertEquals(1L, response.getOperatorId());
        assertEquals("Santosh Gaikwad", response.getOperatorName());
        assertEquals(10L, response.getFarmerId());
        assertEquals("Ramesh Patil", response.getFarmerName());
        assertEquals(5, response.getRating());
        assertEquals("Excellent work and polite operator.", response.getComment());
        verify(reviewRepository, times(1)).save(any(OperatorReview.class));
    }

    @Test
    @DisplayName("Submit valid 1-star review without comment succeeds")
    void testCreateOperatorReview_minimumRatingNoComment_success() {
        OperatorReviewCreateRequest request = OperatorReviewCreateRequest.builder()
                .rating(1)
                .build();

        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(reviewRepository.existsByAssignmentId(100L)).thenReturn(false);
        when(farmerRepository.findById(10L)).thenReturn(Optional.of(farmer));
        when(reviewRepository.save(any(OperatorReview.class))).thenAnswer(inv -> {
            OperatorReview saved = inv.getArgument(0);
            saved.setId(51L);
            return saved;
        });

        OperatorReviewResponse response = reviewService.createOperatorReview(100L, 10L, request);

        assertNotNull(response);
        assertEquals(1, response.getRating());
        assertNull(response.getComment());
    }

    @Test
    @DisplayName("Rating below 1 or above 5 throws BadRequestException")
    void testCreateOperatorReview_invalidRating_throwsBadRequest() {
        OperatorReviewCreateRequest lowRating = OperatorReviewCreateRequest.builder().rating(0).build();
        assertThrows(BadRequestException.class, () -> reviewService.createOperatorReview(100L, 10L, lowRating));

        OperatorReviewCreateRequest highRating = OperatorReviewCreateRequest.builder().rating(6).build();
        assertThrows(BadRequestException.class, () -> reviewService.createOperatorReview(100L, 10L, highRating));

        OperatorReviewCreateRequest nullRating = OperatorReviewCreateRequest.builder().rating(null).build();
        assertThrows(BadRequestException.class, () -> reviewService.createOperatorReview(100L, 10L, nullRating));
    }

    @Test
    @DisplayName("Review for non-completed assignment throws BadRequestException")
    void testCreateOperatorReview_notCompleted_throwsBadRequest() {
        assignment.setAssignmentStatus(OperatorAssignmentStatus.IN_PROGRESS);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        OperatorReviewCreateRequest request = OperatorReviewCreateRequest.builder().rating(4).build();
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> reviewService.createOperatorReview(100L, 10L, request));
        assertTrue(ex.getMessage().contains("Reviews are only allowed after job completion"));
    }

    @Test
    @DisplayName("Review by different farmer (IDOR) throws ForbiddenException")
    void testCreateOperatorReview_wrongFarmer_throwsForbidden() {
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        OperatorReviewCreateRequest request = OperatorReviewCreateRequest.builder().rating(5).build();
        // Booking belongs to farmer 10, but farmer 99 attempts to submit
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> reviewService.createOperatorReview(100L, 99L, request));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("Operator attempting self-review throws ForbiddenException")
    void testCreateOperatorReview_operatorSelfReview_throwsForbidden() {
        // Operator ID is 1L. If caller farmerId is 1L (same as operator ID):
        Operator selfOperator = new Operator();
        selfOperator.setId(10L);
        assignment.setOperator(selfOperator);

        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        OperatorReviewCreateRequest request = OperatorReviewCreateRequest.builder().rating(5).build();
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> reviewService.createOperatorReview(100L, 10L, request));
        assertTrue(ex.getMessage().contains("Operators cannot submit reviews"));
    }

    @Test
    @DisplayName("Duplicate review for same assignment throws BadRequestException")
    void testCreateOperatorReview_duplicate_throwsBadRequest() {
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(reviewRepository.existsByAssignmentId(100L)).thenReturn(true);

        OperatorReviewCreateRequest request = OperatorReviewCreateRequest.builder().rating(5).build();
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> reviewService.createOperatorReview(100L, 10L, request));
        assertTrue(ex.getMessage().contains("already been submitted"));
    }

    @Test
    @DisplayName("Review for non-existent assignment throws ResourceNotFoundException")
    void testCreateOperatorReview_assignmentNotFound_throwsNotFound() {
        when(assignmentRepository.findById(999L)).thenReturn(Optional.empty());

        OperatorReviewCreateRequest request = OperatorReviewCreateRequest.builder().rating(5).build();
        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createOperatorReview(999L, 10L, request));
    }

    @Test
    @DisplayName("Rating summary calculation aggregates average and star counts correctly")
    void testGetOperatorRatingSummary_success() {
        OperatorReview r1 = OperatorReview.builder().rating(5).build();
        OperatorReview r2 = OperatorReview.builder().rating(5).build();
        OperatorReview r3 = OperatorReview.builder().rating(4).build();
        OperatorReview r4 = OperatorReview.builder().rating(3).build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(reviewRepository.findByOperatorId(1L)).thenReturn(List.of(r1, r2, r3, r4));

        OperatorRatingSummaryResponse summary = reviewService.getOperatorRatingSummary(1L);

        assertNotNull(summary);
        assertEquals(1L, summary.getOperatorId());
        assertEquals(4L, summary.getTotalReviews());
        // (5+5+4+3)/4 = 17/4 = 4.25 -> rounded to 1 decimal = 4.3
        assertEquals(4.3, summary.getAverageRating());
        assertEquals(2L, summary.getFiveStarCount());
        assertEquals(1L, summary.getFourStarCount());
        assertEquals(1L, summary.getThreeStarCount());
        assertEquals(0L, summary.getTwoStarCount());
        assertEquals(0L, summary.getOneStarCount());
    }

    @Test
    @DisplayName("Rating summary for operator with zero reviews returns default 0.0 average")
    void testGetOperatorRatingSummary_emptyReviews() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(reviewRepository.findByOperatorId(1L)).thenReturn(List.of());

        OperatorRatingSummaryResponse summary = reviewService.getOperatorRatingSummary(1L);

        assertNotNull(summary);
        assertEquals(0L, summary.getTotalReviews());
        assertEquals(0.0, summary.getAverageRating());
        assertEquals(0L, summary.getFiveStarCount());
    }

    @Test
    @DisplayName("Get operator reviews returns paginated list")
    void testGetOperatorReviews_paginated() {
        OperatorReview rev = OperatorReview.builder()
                .assignment(assignment)
                .operator(operator)
                .booking(booking)
                .farmerId(10L)
                .rating(5)
                .comment("Superb service")
                .build();
        rev.setId(1L);
        rev.setCreatedAt(LocalDateTime.now());

        Page<OperatorReview> page = new PageImpl<>(List.of(rev), PageRequest.of(0, 10), 1);
        when(operatorRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findByOperatorId(eq(1L), any(Pageable.class))).thenReturn(page);
        when(farmerRepository.findById(10L)).thenReturn(Optional.of(farmer));

        Page<OperatorReviewResponse> result = reviewService.getOperatorReviews(1L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(5, result.getContent().get(0).getRating());
        assertEquals("Superb service", result.getContent().get(0).getComment());
    }
}
