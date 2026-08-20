package com.agrorental.equipment.dto;

import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Public API response DTO representing a complete Equipment listing details.
 * Prevents JPA entity leakage and circular references.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentResponse {

    private Long id;
    private String name;
    private EquipmentCategory category;
    private String brand;
    private String model;
    private Integer manufacturingYear;
    private String capacity;
    private BigDecimal rentalPrice;
    private FuelType fuelType;
    private String description;
    private Long partnerId;
    private String locationAddress;
    private Double latitude;
    private Double longitude;
    private AvailabilityStatus availabilityStatus;
    private LocalDate availableFromDate;
    private LocalDate availableToDate;
    private String maintenanceNotes;
    private Boolean isDisabled;
    private List<EquipmentImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
