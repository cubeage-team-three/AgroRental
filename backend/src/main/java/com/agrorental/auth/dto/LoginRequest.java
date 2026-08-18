package com.agrorental.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Mobile number or email address is mandatory")
    private String mobileOrEmail;

    private String password;

    private String loginType; // "PASSWORD" or "OTP"

    private String otp;
}
