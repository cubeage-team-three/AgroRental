package com.agrorental.operator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Public response DTO for an operator review and customer feedback record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorReviewResponse {

    private Long reviewId;
    private Long assignmentId;
    private Long bookingId;
    private Long operatorId;
    private String operatorName;
    private Long farmerId;
    private String farmerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
