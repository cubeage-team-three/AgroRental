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

    /**
     * Never populated: OTP verification no longer issues a session (farmers log in with their password).
     * The field is kept so the response shape is unchanged.
     */
    private String token;
    private Long farmerId;
    private String fullName;
    private String email;
    private String role;
}
