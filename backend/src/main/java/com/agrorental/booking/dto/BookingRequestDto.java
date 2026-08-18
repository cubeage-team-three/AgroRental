package com.agrorental.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inbound payload for creating a booking. Deliberately does NOT carry a
 * partnerId — the partner is always derived server-side from the equipment
 * being booked (equipment.getPartner()), so a caller can never book
 * equipment against a partner it doesn't actually belong to.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequestDto {

    @NotNull(message = "Farmer ID is mandatory")
    private Long farmerId;

    @NotNull(message = "Equipment ID is mandatory")
    private Long equipmentId;

    /** Optional — a partner may not provide a dedicated operator. */
    private Long operatorId;

    @NotNull(message = "Start date is mandatory")
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;

    @NotNull(message = "End date is mandatory")
    @FutureOrPresent(message = "End date cannot be in the past")
    private LocalDate endDate;

    @NotNull(message = "Total acreage is mandatory")
    @DecimalMin(value = "0.01", message = "Total acreage must be greater than zero")
    private BigDecimal totalAcreage;
}
