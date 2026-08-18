package com.agrorental.equipment.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO representing an equipment image asset for public API consumers.
 * Prevents recursive JPA entity serialization.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentImageResponse {

    private Long id;
    private String imageUrl;
    private Boolean isPrimary;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
