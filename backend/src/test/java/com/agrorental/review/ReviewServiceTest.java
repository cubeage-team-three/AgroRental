package com.agrorental.review;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.partner.entity.Partner;
import com.agrorental.review.dto.RatingSummaryResponse;
import com.agrorental.review.dto.ReviewCreateRequest;
import com.agrorental.review.dto.ReviewResponse;
import com.agrorental.review.entity.Review;
import com.agrorental.review.repository.ReviewRepository;
import com.agrorental.review.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewService reviewService;

    private Booking testBooking;
    private Equipment testEquipment;
    private Partner testPartner;
    private Review testReview;

    @BeforeEach
    void setUp() {
        testPartner = Partner.builder().fullName("Patil Machinery Fleet").build();
        testPartner.setId(10L);

        testEquipment = Equipment.builder().name("John Deere Harvester").partner(testPartner).build();
        testEquipment.setId(5L);

        testBooking = Booking.builder()
                .farmerId(50L)
                .partner(testPartner)
                .equipment(testEquipment)
                .status(BookingStatus.COMPLETED)
                .build();
        testBooking.setId(100L);

        testReview = Review.builder()
                .id(1L)
                .bookingId(100L)
                .farmerId(50L)
                .equipmentId(5L)
                .partnerId(10L)
                .rating(5)
                .comment("Excellent machinery performance!")
                .build();
    }

    @Test
    @DisplayName("createReview: Saves 5-star review for completed booking and notifies partner")
    void createReview_Valid5Star_Success() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .bookingId(100L)
                .farmerId(50L)
                .rating(5)
                .comment("Excellent machinery performance!")
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBookingId(100L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(farmerRepository.findById(50L)).thenReturn(Optional.of(Farmer.builder().fullName("Farmer Name").build()));

        ReviewResponse response = reviewService.createReview(request);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Excellent machinery performance!", response.getComment());
        verify(notificationService).sendNotification(eq("PARTNER"), eq(10L), anyString(), anyString(), eq("REVIEW_RECEIVED"), eq(100L));
    }

    @Test
    @DisplayName("createReview: Saves 1-star review for completed booking")
    void createReview_Valid1Star_Success() {
        testReview.setRating(1);
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .bookingId(100L)
                .farmerId(50L)
                .rating(1)
                .comment("Poor performance.")
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBookingId(100L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(farmerRepository.findById(50L)).thenReturn(Optional.of(Farmer.builder().fullName("Farmer Name").build()));

        ReviewResponse response = reviewService.createReview(request);

        assertNotNull(response);
        assertEquals(1, response.getRating());
    }

    @Test
    @DisplayName("createReview: Throws BadRequestException for Rating = 0")
    void createReview_RatingZero_ThrowsException() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .bookingId(100L)
                .farmerId(50L)
                .rating(0)
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> reviewService.createReview(request));

        assertTrue(exception.getMessage().contains("Rating must be between 1 and 5"));
    }

    @Test
    @DisplayName("createReview: Throws BadRequestException for Rating > 5")
    void createReview_RatingGreaterThan5_ThrowsException() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .bookingId(100L)
                .farmerId(50L)
                .rating(6)
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> reviewService.createReview(request));

        assertTrue(exception.getMessage().contains("Rating must be between 1 and 5"));
    }

    @Test
    @DisplayName("createReview: Throws BadRequestException if booking status is not COMPLETED")
    void createReview_NonCompletedBooking_ThrowsException() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .bookingId(100L)
                .farmerId(50L)
                .rating(5)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> reviewService.createReview(request));

        assertTrue(exception.getMessage().contains("Only completed bookings can be reviewed"));
    }

    @Test
    @DisplayName("createReview: Throws BadRequestException for unauthorized farmer attempt")
    void createReview_UnauthorizedFarmer_ThrowsException() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .bookingId(100L)
                .farmerId(999L)
                .rating(5)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> reviewService.createReview(request));

        assertTrue(exception.getMessage().contains("Farmer is not authorized"));
    }

    @Test
    @DisplayName("createReview: Throws BadRequestException for duplicate review submission")
    void createReview_DuplicateSubmission_ThrowsException() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .bookingId(100L)
                .farmerId(50L)
                .rating(5)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBookingId(100L)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> reviewService.createReview(request));

        assertTrue(exception.getMessage().contains("A review has already been submitted"));
    }

    @Test
    @DisplayName("getRatingSummaryForEquipment: Returns average rating and review count")
    void getRatingSummaryForEquipment_Success() {
        when(reviewRepository.findAverageRatingByEquipmentId(5L)).thenReturn(4.8333);
        when(reviewRepository.countByEquipmentId(5L)).thenReturn(12L);

        RatingSummaryResponse summary = reviewService.getRatingSummaryForEquipment(5L);

        assertNotNull(summary);
        assertEquals(5L, summary.getTargetId());
        assertEquals(4.8, summary.getAverageRating());
        assertEquals(12L, summary.getTotalReviews());
    }

    @Test
    @DisplayName("getRatingSummaryForEquipment: Zero review fallback case")
    void getRatingSummaryForEquipment_ZeroReviews_ReturnsZero() {
        when(reviewRepository.findAverageRatingByEquipmentId(5L)).thenReturn(0.0);
        when(reviewRepository.countByEquipmentId(5L)).thenReturn(0L);

        RatingSummaryResponse summary = reviewService.getRatingSummaryForEquipment(5L);

        assertNotNull(summary);
        assertEquals(0.0, summary.getAverageRating());
        assertEquals(0L, summary.getTotalReviews());
    }
}
