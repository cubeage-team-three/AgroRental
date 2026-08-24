package com.agrorental.operator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Historical record of completed field work, hours logged, and gross compensation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorEarningsHistoryResponse {

    private Long assignmentId;
    private Long bookingId;
    private String equipmentName;
    private String equipmentCategory;
    private String deliveryAddress;
    private LocalDateTime completedAt;
    private Long netWorkMinutes;
    private Double netWorkHours;
    private BigDecimal hourlyRate;
    private BigDecimal grossEarnings;
    private String currency;
}
