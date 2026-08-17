package com.agrorental.operator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorOtpVerifyRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[6-9][0-9]{9}$",
        message = "Enter a valid 10-digit mobile number"
    )
    private String mobileNumber;

    @NotBlank(message = "OTP is required")
    @Pattern(
        regexp = "^[0-9]{6}$",
        message = "OTP must contain 6 digits"
    )
    private String otp;
}
