package com.agrorental.farmer.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object for creating or updating a farm.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmCreateRequest {

    private Long farmerId;

    @NotBlank(message = "Farm name is required")
    @Size(max = 150, message = "Farm name cannot exceed 150 characters")
    private String farmName;

    @NotBlank(message = "Village name is required")
    @Size(max = 100, message = "Village name cannot exceed 100 characters")
    private String village;

    @NotBlank(message = "Taluka name is required")
    @Size(max = 100, message = "Taluka name cannot exceed 100 characters")
    private String taluka;

    @NotBlank(message = "District name is required")
    @Size(max = 100, message = "District name cannot exceed 100 characters")
    private String district;

    @NotBlank(message = "State name is required")
    @Size(max = 100, message = "State name cannot exceed 100 characters")
    private String state;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotNull(message = "Farm area is required")
    @Positive(message = "Farm area must be greater than 0")
    private BigDecimal farmArea;

    @Size(max = 100, message = "Crop type cannot exceed 100 characters")
    private String cropType;
}
