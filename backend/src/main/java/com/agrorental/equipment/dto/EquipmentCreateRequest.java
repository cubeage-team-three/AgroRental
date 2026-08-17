package com.agrorental.equipment.dto;

import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO representing payload required to create a new Equipment listing.
 * Strict validation applied at request boundary.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentCreateRequest {

    @NotBlank(message = "Equipment name is mandatory")
    private String name;

    @NotNull(message = "Equipment category is mandatory")
    private EquipmentCategory category;

    @NotBlank(message = "Brand is mandatory")
    private String brand;

    @NotBlank(message = "Model is mandatory")
    private String model;

    @NotNull(message = "Manufacturing year is mandatory")
    private Integer manufacturingYear;

    @NotBlank(message = "Capacity is mandatory")
    private String capacity;

    @NotNull(message = "Rental price is mandatory")
    @DecimalMin(value = "0.01", message = "Rental price must be greater than zero")
    private BigDecimal rentalPrice;

    @NotNull(message = "Fuel type is mandatory")
    private FuelType fuelType;

    @NotBlank(message = "Description is mandatory")
    private String description;

    @NotNull(message = "Partner ID is mandatory")
    private Long partnerId;

    @NotBlank(message = "Location address is mandatory")
    private String locationAddress;

    @NotNull(message = "Latitude is mandatory")
    private Double latitude;

    @NotNull(message = "Longitude is mandatory")
    private Double longitude;

    @NotEmpty(message = "At least one equipment image is required")
    @Valid
    private List<EquipmentImageRequest> images;
}
