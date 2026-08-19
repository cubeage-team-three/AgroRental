package com.agrorental.partner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String businessName;

    @Email(message = "Invalid email format")
    private String email;

    private String address;

    private String gstNumber;

    private String aadhaarNumber;

    private String panNumber;

    private String profilePhoto;
}
