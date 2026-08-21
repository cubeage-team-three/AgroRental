package com.agrorental.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerBookingRevenueReportDto {
    private Long bookingId;
    private String transactionId;
    private String invoiceReference;
    private Long equipmentId;
    private String equipmentName;
    private Long farmerId;
    private String farmerName;
    private String farmerMobile;
    private LocalDateTime paymentDate;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
}
