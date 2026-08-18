package com.agrorental.equipment.entity;

import com.agrorental.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "equipment_images")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
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