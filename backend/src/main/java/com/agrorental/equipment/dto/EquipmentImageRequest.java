package com.agrorental.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO representing an equipment image payload during create/update operations.
 * Excludes JPA entities and persistence metadata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentImageRequest {

    @NotBlank(message = "Image URL is mandatory")
    private String imageUrl;

    @NotNull(message = "Primary status flag is mandatory")
    private Boolean isPrimary;

    private Integer displayOrder;
}
