package com.agrorental.operator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregate summary of an operator's overall rating score and star rating distribution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorRatingSummaryResponse {

    private Long operatorId;
    private Double averageRating;
    private Long totalReviews;
    private Long fiveStarCount;
    private Long fourStarCount;
    private Long threeStarCount;
    private Long twoStarCount;
    private Long oneStarCount;
}
