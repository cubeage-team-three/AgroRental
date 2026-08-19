package com.agrorental.review.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.review.dto.RatingSummaryResponse;
import com.agrorental.review.dto.ReviewCreateRequest;
import com.agrorental.review.dto.ReviewResponse;
import com.agrorental.review.entity.Review;
import com.agrorental.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service managing reviews, rating aggregate math, and security checks.
 */
@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final FarmerRepository farmerRepository;
    private final NotificationService notificationService;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            FarmerRepository farmerRepository,
            NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.farmerRepository = farmerRepository;
        this.notificationService = notificationService;
    }

    /**
     * Submits a new review for a completed booking.
     */
    public ReviewResponse createReview(ReviewCreateRequest request) {
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + request.getBookingId()));

        if (!booking.getFarmerId().equals(request.getFarmerId())) {
            throw new BadRequestException("Farmer is not authorized to review this booking");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only completed bookings can be reviewed. Current state: " + booking.getStatus());
        }

        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new BadRequestException("A review has already been submitted for this booking");
        }

        Long equipmentId = booking.getEquipment() != null ? booking.getEquipment().getId() : 1L;
        Long partnerId = booking.getPartner() != null ? booking.getPartner().getId() : 1L;

        Review review = Review.builder()
                .bookingId(booking.getId())
                .farmerId(booking.getFarmerId())
                .equipmentId(equipmentId)
                .partnerId(partnerId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);

        String equipName = booking.getEquipment() != null ? booking.getEquipment().getName() : "Machinery";

        // Dispatch Phase 7 Notification to Partner
        notificationService.sendNotification(
                "PARTNER",
                partnerId,
                "New Farmer Review Received",
                "Farmer submitted a " + saved.getRating() + "-star review for " + equipName + " (Booking #" + booking.getId() + ").",
                "REVIEW_RECEIVED",
                booking.getId()
        );

        return mapToResponse(saved, booking);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReviewByBookingId(Long bookingId) {
        Review review = reviewRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No review found for booking ID: " + bookingId));

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        return mapToResponse(review, booking);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForEquipment(Long equipmentId) {
        return reviewRepository.findByEquipmentIdOrderByCreatedAtDesc(equipmentId).stream()
                .map(r -> mapToResponse(r, bookingRepository.findById(r.getBookingId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForPartner(Long partnerId) {
        return reviewRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .map(r -> mapToResponse(r, bookingRepository.findById(r.getBookingId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForFarmer(Long farmerId) {
        return reviewRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId).stream()
                .map(r -> mapToResponse(r, bookingRepository.findById(r.getBookingId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummaryForEquipment(Long equipmentId) {
        Double avg = reviewRepository.findAverageRatingByEquipmentId(equipmentId);
        long count = reviewRepository.countByEquipmentId(equipmentId);
        double roundedAvg = BigDecimal.valueOf(avg != null ? avg : 0.0)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return RatingSummaryResponse.builder()
                .targetId(equipmentId)
                .averageRating(roundedAvg)
                .totalReviews(count)
                .build();
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummaryForPartner(Long partnerId) {
        Double avg = reviewRepository.findAverageRatingByPartnerId(partnerId);
        long count = reviewRepository.countByPartnerId(partnerId);
        double roundedAvg = BigDecimal.valueOf(avg != null ? avg : 0.0)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return RatingSummaryResponse.builder()
                .targetId(partnerId)
                .averageRating(roundedAvg)
                .totalReviews(count)
                .build();
    }

    private ReviewResponse mapToResponse(Review review, Booking booking) {
        String farmerName = farmerRepository.findById(review.getFarmerId())
                .map(f -> f.getFullName())
                .orElse("Ramesh Yadav");

        String equipmentName = (booking != null && booking.getEquipment() != null)
                ? booking.getEquipment().getName()
                : "Agricultural Machinery";

        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBookingId())
                .farmerId(review.getFarmerId())
                .farmerName(farmerName)
                .equipmentId(review.getEquipmentId())
                .equipmentName(equipmentName)
                .partnerId(review.getPartnerId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
