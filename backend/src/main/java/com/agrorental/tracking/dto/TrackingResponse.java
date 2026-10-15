package com.agrorental.tracking.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingResponse {
    private Long id;
    private Long bookingId;
    private Long operatorId;
    private String operatorName;
    private String operatorMobile;
    private String equipmentName;
    private Double latitude;
    private Double longitude;
    private String eta;
    private String routeInformation;
    private Integer workProgress;
    private String status;
    private LocalDateTime lastUpdated;

    /** True only while the booking is in the field and a recorded (GPS or tracking-record) position exists. */
    private boolean trackingAvailable;

    /**
     * Where latitude/longitude come from: OPERATOR_GPS (operator device), TRACKING_RECORD (stored tracking record),
     * EQUIPMENT_BASE (the equipment's registered location, shown as a reference while no position has been
     * recorded yet; not a live position), or null when no position is returned.
     */
    private String locationSource;

    /** Human-readable explanation of the tracking state (e.g. why no live position is available). */
    private String message;
}
