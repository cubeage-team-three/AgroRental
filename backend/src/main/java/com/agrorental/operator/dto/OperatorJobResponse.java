package com.agrorental.operator.dto;

import com.agrorental.operator.entity.JobStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorJobResponse {

    private Long id;
    private Long operatorId;
    private String operatorName;
    private String operatorMobile;

    // Equipment Details
    private Long equipmentId;
    private String equipmentName;
    private String equipmentCategory;
    private String equipmentBrand;
    private String equipmentModel;

    // Partner Details
    private Long partnerId;
    private String partnerName;
    private String partnerMobile;

    // Customer & Work Details
    private String customerName;
    private String customerMobile;
    private String jobTitle;
    private String jobType;
    private String jobDescription;
    private String workInstructions;

    // Schedule & Location
    private LocalDate scheduledDate;
    private LocalTime scheduledStartTime;
    private LocalTime scheduledEndTime;
    private Double estimatedDurationHours;
    private String workLocation;
    private Double latitude;
    private Double longitude;

    // Payout & Status
    private BigDecimal operatorPayout;
    private JobStatus status;
    private String assignedBy;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
