package com.agrorental.review.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a review payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long id;
    private Long bookingId;
    private Long farmerId;
    private String farmerName;
    private Long equipmentId;
    private String equipmentName;
    private String equipmentCategory;
    private Long partnerId;
    private Integer rating;
    private String comment;
    private LocalDate serviceDate;
    private LocalDateTime createdAt;
}
