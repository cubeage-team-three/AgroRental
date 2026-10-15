package com.agrorental.user.dto;

import com.agrorental.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object for User Registration requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 120, message = "Email cannot exceed 120 characters")
    private String email;

    /** Must satisfy {@link com.agrorental.auth.service.PasswordPolicy} (checked by the service). */
    @NotBlank(message = "Password is required")
    @ToString.Exclude
    private String password;

    private Role role;

    private String mobileNumber;

    private String address;

    private String preferredLanguage;
}
