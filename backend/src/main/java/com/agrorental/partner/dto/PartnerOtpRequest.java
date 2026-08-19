package com.agrorental.partner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PartnerOtpRequest {

    @NotBlank(message = "OTP is required")
    private String otp;
}