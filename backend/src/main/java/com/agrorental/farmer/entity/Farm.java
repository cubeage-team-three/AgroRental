package com.agrorental.farmer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a farmer's registered agricultural land parcel.
 */
@Entity
@Table(name = "farms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "farm_id")
    private Long id;

    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @Column(name = "farm_name", nullable = false, length = 150)
    private String farmName;

    @Column(name = "village", nullable = false, length = 100)
    private String village;

    @Column(name = "taluka", nullable = false, length = 100)
    private String taluka;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "farm_area", nullable = false, precision = 10, scale = 2)
    private BigDecimal farmArea;

    @Column(name = "crop_type", length = 100)
    private String cropType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
