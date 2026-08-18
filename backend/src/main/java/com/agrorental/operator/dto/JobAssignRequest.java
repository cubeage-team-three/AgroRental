package com.agrorental.operator.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobAssignRequest {

    @NotNull(message = "Operator ID is required")
    private Long operatorId;

    private Long equipmentId;

    private Long partnerId;

    @NotBlank(message = "Job title is required")
    @Size(min = 3, max = 150, message = "Job title must be between 3 and 150 characters")
    private String jobTitle;

    private String jobType;

    private String jobDescription;

    private String workInstructions;

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    private String customerName;

    @NotBlank(message = "Customer mobile number is required")
    private String customerMobile;

    @NotBlank(message = "Work location address is required")
    @Size(min = 5, max = 255, message = "Work location must be between 5 and 255 characters")
    private String workLocation;

    private Double latitude;

    private Double longitude;

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    private LocalTime scheduledStartTime;

    private LocalTime scheduledEndTime;

    private Double estimatedDurationHours;

    @NotNull(message = "Operator payout amount is required")
    @DecimalMin(value = "0.01", message = "Operator payout must be greater than zero")
    private BigDecimal operatorPayout;

    private String assignedBy;

    private String notes;
}
