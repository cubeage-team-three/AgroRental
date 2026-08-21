package com.agrorental.operator.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload submitted by an authenticated farmer to review an operator after job completion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorReviewCreateRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5 stars")
    @Max(value = 5, message = "Rating must be between 1 and 5 stars")
    private Integer rating;

    @Size(max = 1000, message = "Review comment cannot exceed 1000 characters")
    private String comment;
}
