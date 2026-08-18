package com.agrorental.equipment.entity;

import com.agrorental.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Domain entity representing an image asset belonging to an Equipment listing.
 * Maps strictly to requirements defined in FR-15 and FR-06.
 */
@Entity
@Table(name = "equipment_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class EquipmentImage extends BaseEntity {

    @NotBlank(message = "Image URL is mandatory")
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @NotNull
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    @ToString.Exclude
    private Equipment equipment;

    @PrePersist
    protected void onCreate() {
        if (this.isPrimary == null) {
            this.isPrimary = false;
        }
        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }
    }
}
