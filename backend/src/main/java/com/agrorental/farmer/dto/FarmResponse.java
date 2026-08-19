package com.agrorental.farmer.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing farm details returned in HTTP responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmResponse {

    private Long id;
    private Long farmerId;
    private String farmName;
    private String village;
    private String taluka;
    private String district;
    private String state;
    private Double latitude;
    private Double longitude;
    private BigDecimal farmArea;
    private String cropType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
