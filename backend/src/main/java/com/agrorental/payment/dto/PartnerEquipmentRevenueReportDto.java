package com.agrorental.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerEquipmentRevenueReportDto {
    private Long equipmentId;
    private String equipmentName;
    private String category;
    private String brand;
    private String model;
    private long totalBookings;
    private BigDecimal totalRevenue;
    private BigDecimal dailyRentalPrice;
    private String primaryImageUrl;
}
