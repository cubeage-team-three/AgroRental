package com.agrorental.operator.dto;

import com.agrorental.operator.otp.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for sending an OTP to an Operator's mobile number.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorOtpSendRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[6-9][0-9]{9}$",
        message = "Enter a valid 10-digit mobile number"
    )
    private String mobileNumber;

    @Builder.Default
    private OtpPurpose purpose = OtpPurpose.MOBILE_VERIFICATION;
}
