package com.agrorental.payment.dto;

import com.agrorental.payment.entity.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {
    private String invoiceReference;
    private String transactionId;
    private Long bookingId;
    private Long farmerId;
    private String farmerName;
    private String farmerMobile;
    private String equipmentName;
    private String equipmentCategory;
    private String partnerName;
    private LocalDate bookingStartDate;
    private LocalDate bookingEndDate;
    private BigDecimal rentalRatePerDay;
    private Long rentalDays;
    private BigDecimal subtotal;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private LocalDateTime paymentDate;
    private String status;
}
