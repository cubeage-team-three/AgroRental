package com.agrorental.partner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartnerRegistrationRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String businessName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "Mobile number must be 10 digits"
    )
    private String mobileNumber;

    @Email(message = "Invalid email format")
    private String email;

    private String address;

    private String gstNumber;

    private String aadhaarNumber;

    private String panNumber;

    @NotBlank(message = "Password is required")
    private String password;

    private String profilePhoto;
}