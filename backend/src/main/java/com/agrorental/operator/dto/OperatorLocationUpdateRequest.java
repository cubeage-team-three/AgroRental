package com.agrorental.operator.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload request DTO for publishing operator GPS location updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorLocationUpdateRequest {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    private Double longitude;

    @DecimalMin(value = "0.0", message = "Accuracy must be >= 0.0")
    private Double accuracy;

    @DecimalMin(value = "0.0", message = "Speed must be >= 0.0")
    private Double speed;

    @DecimalMin(value = "0.0", message = "Heading must be >= 0.0")
    @DecimalMax(value = "359.999999", message = "Heading must be < 360.0")
    private Double heading;
}
