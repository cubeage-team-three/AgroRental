package com.agrorental.tracking.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingUpdateRequest {
    private Double latitude;
    private Double longitude;
    private String eta;
    private String routeInformation;
    private Integer workProgress;
}
