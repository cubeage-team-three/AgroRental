package com.agrorental.farmer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpResponse {

    private String mobileNumber;
    private String message;
    private Boolean verified;
    private Integer attemptsRemaining;
    private LocalDateTime expiresAt;
    private String devMockOtp; // Helpful for development & automated testing
}
