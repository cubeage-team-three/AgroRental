package com.agrorental.booking.dto;

import com.agrorental.booking.entity.constant.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Flattened outbound representation of a Booking — pulls just the display
 * fields off each related entity instead of serializing full Farmer/
 * Equipment/Partner/Operator graphs, so the API never leaks unrelated data
 * (password hashes, other bookings, etc.) through lazy-loaded associations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDto {

    private Long id;

    private Long farmerId;
    private String farmerName;

    private Long equipmentId;
    private String equipmentName;

    private Long partnerId;
    private String partnerName;

    private Long operatorId;
    private String operatorName;

    private LocalDate bookingDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalAcreage;
    private BigDecimal totalCost;
    private BookingStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
