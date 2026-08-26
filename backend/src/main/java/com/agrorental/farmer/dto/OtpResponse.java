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

    // Populated only on a successful verify-otp response, so the frontend can
    // establish a real session immediately after a fresh registration is verified.
    private String token;
    private Long farmerId;
    private String fullName;
    private String email;
    private String role;
}
