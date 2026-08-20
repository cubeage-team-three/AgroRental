package com.agrorental.operator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for Operator login (FR-39).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorLoginRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Enter a valid 10-digit mobile number")
    private String mobileNumber;

    @NotBlank(message = "Password is required")
    private String password;
}
