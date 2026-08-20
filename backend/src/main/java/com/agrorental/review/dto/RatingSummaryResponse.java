package com.agrorental.review.dto;

import lombok.*;

/**
 * Data Transfer Object carrying average rating and review count statistics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingSummaryResponse {

    private Long targetId;
    private Double averageRating;
    private long totalReviews;
}
