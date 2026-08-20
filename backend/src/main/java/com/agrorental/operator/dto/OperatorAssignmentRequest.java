package com.agrorental.operator.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload submitted by Partner or Admin to assign an Operator to a Booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorAssignmentRequest {

    @NotNull(message = "Operator ID is required")
    @Positive(message = "Operator ID must be a positive number")
    private Long operatorId;

    @Size(max = 500, message = "Assignment notes must not exceed 500 characters")
    private String notes;
}
