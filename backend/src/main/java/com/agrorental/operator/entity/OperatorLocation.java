package com.agrorental.operator.entity;

import com.agrorental.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Domain entity representing periodic GPS coordinates and real-time tracking status for an active Operator Job Assignment.
 */
@Entity
@Table(
        name = "operator_locations",
        indexes = {
                @Index(name = "idx_op_loc_assignment", columnList = "assignment_id"),
                @Index(name = "idx_op_loc_operator", columnList = "operator_id"),
                @Index(name = "idx_op_loc_recorded", columnList = "recorded_at"),
                @Index(name = "idx_op_loc_active", columnList = "tracking_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorLocation extends BaseEntity {

    @NotNull(message = "Job assignment reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private OperatorJobAssignment assignment;

    @NotNull(message = "Operator reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @DecimalMin(value = "0.0", message = "Accuracy must be >= 0.0")
    @Column(name = "accuracy")
    private Double accuracy;

    @DecimalMin(value = "0.0", message = "Speed must be >= 0.0")
    @Column(name = "speed")
    private Double speed;

    @DecimalMin(value = "0.0", message = "Heading must be >= 0.0")
    @DecimalMax(value = "359.999999", message = "Heading must be < 360.0")
    @Column(name = "heading")
    private Double heading;

    @NotNull(message = "Recorded timestamp is required")
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Builder.Default
    @Column(name = "tracking_active", nullable = false)
    private boolean trackingActive = true;
}
