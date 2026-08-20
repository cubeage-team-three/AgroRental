package com.agrorental.operator.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating authenticated Operator editable profile fields.
 * Explicitly excludes immutable security and lifecycle fields (id, mobileNumber, password, aadhaarNumber, drivingLicenseNumber, status, active, mobileVerified).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Email(message = "Enter a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Service location address is required")
    @Size(min = 3, max = 255, message = "Address must be between 3 and 255 characters")
    private String address;

    @NotNull(message = "Operational experience is required")
    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 50, message = "Experience cannot exceed 50 years")
    private Integer experience;

    @NotBlank(message = "Machinery skills are required")
    @Size(min = 2, max = 255, message = "Skills description must be between 2 and 255 characters")
    private String skills;

    @Size(max = 500, message = "Profile photo URL must not exceed 500 characters")
    private String profilePhoto;
}
