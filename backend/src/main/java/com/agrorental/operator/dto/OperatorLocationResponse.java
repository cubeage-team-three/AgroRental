package com.agrorental.operator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO delivering the latest operator GPS location and tracking status for an active job assignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorLocationResponse {

    private Long id;
    private Long assignmentId;
    private Long operatorId;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Double speed;
    private Double heading;
    private boolean trackingActive;
    private LocalDateTime recordedAt;
}
