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
public class PartnerCustomerRevenueReportDto {
    private Long farmerId;
    private String farmerName;
    private String mobileNumber;
    private String email;
    private String location;
    private long totalBookings;
    private BigDecimal totalRevenue;
}
