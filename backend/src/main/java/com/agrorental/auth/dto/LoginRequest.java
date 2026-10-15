package com.agrorental.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password", "otp"})
public class LoginRequest {

    @NotBlank(message = "Mobile number or email address is mandatory")
    private String mobileOrEmail;

    private String password;

    private String role; // "FARMER", "PARTNER", "OPERATOR", "ADMIN"

    /** "PASSWORD" (default). "OTP" is rejected: OTP login is no longer supported. */
    private String loginType;

    /** Ignored: OTP login is no longer supported (kept so older clients still deserialize). */
    private String otp;
}
