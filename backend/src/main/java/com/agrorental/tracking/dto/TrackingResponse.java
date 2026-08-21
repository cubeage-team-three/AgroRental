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
}
