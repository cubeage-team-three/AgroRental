package com.agrorental.equipment.entity;

import com.agrorental.common.entity.BaseEntity;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import com.agrorental.partner.entity.Partner;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Primary domain entity representing agricultural equipment listed on AgroRental.
 * Maps strictly to requirements defined in FR-15, FR-16, FR-05, FR-06, and FR-39.
 */
@Entity
@Table(name = "equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Equipment extends BaseEntity {

    @NotBlank(message = "Equipment name is mandatory")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Equipment category is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private EquipmentCategory category;

    @NotBlank(message = "Brand is mandatory")
    @Column(name = "brand", nullable = false)
    private String brand;

    @NotBlank(message = "Model is mandatory")
    @Column(name = "model", nullable = false)
    private String model;

    @NotNull(message = "Manufacturing year is mandatory")
    @Column(name = "manufacturing_year", nullable = false)
    private Integer manufacturingYear;

    @NotBlank(message = "Capacity is mandatory")
    @Column(name = "capacity", nullable = false)
    private String capacity;

    @NotNull(message = "Rental price is mandatory")
    @DecimalMin(value = "0.01", message = "Rental price must be greater than zero")
    @Column(name = "rental_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal rentalPrice;

    @NotNull(message = "Fuel type is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @NotBlank(message = "Description is mandatory")
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Partner is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    @ToString.Exclude
    private Partner partner;

    @NotBlank(message = "Location address is mandatory")
    @Column(name = "location_address", nullable = false)
    private String locationAddress;

    @NotNull(message = "Latitude is mandatory")
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @NotNull(message = "Longitude is mandatory")
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @NotNull(message = "Availability status is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false)
    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Column(name = "maintenance_notes")
    private String maintenanceNotes;

    @NotNull
    @Column(name = "is_disabled", nullable = false)
    @Builder.Default
    private Boolean isDisabled = false;

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<EquipmentImage> images = new ArrayList<>();

    public void addImage(EquipmentImage image) {
        if (images == null) {
            images = new ArrayList<>();
        }
        images.add(image);
        image.setEquipment(this);
    }

    public void removeImage(EquipmentImage image) {
        if (images != null) {
            images.remove(image);
            image.setEquipment(null);
        }
    }
}
