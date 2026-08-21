package com.agrorental.tracking.entity;

import com.agrorental.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA Entity for storing live tracking coordinates and progress for active equipment bookings.
 */
@Entity
@Table(name = "live_tracking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tracking extends BaseEntity {

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "eta")
    private String eta;

    @Column(name = "route_information")
    private String routeInformation;

    @Column(name = "work_progress")
    private Integer workProgress; // percentage 0-100

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
