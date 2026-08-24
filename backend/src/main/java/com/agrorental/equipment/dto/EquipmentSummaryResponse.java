package com.agrorental.equipment.dto;

import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Compact API response DTO representing an equipment listing card for Farmer search/listing discovery.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentSummaryResponse {

    private Long id;
    private String name;
    private EquipmentCategory category;
    private String brand;
    private String model;
    private String capacity;
    private BigDecimal rentalPrice;
    private FuelType fuelType;
    private String locationAddress;
    private Double latitude;
    private Double longitude;
    private AvailabilityStatus availabilityStatus;
    private LocalDate availableFromDate;
    private LocalDate availableToDate;
    private Boolean isDisabled;
    private String primaryImageUrl;
}
