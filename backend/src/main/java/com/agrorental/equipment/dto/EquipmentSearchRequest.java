package com.agrorental.equipment.dto;

import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO representing dynamic Farmer search and filter parameters for Equipment discovery.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentSearchRequest {

    private EquipmentCategory category;

    @DecimalMin(value = "0.00", message = "Minimum price cannot be negative")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.00", message = "Maximum price cannot be negative")
    private BigDecimal maxPrice;

    private AvailabilityStatus availabilityStatus;

    private String locationAddress;
}
