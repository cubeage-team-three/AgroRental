package com.agrorental.partner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"currentPassword", "newPassword"})
public class PartnerChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /** Must satisfy {@link com.agrorental.auth.service.PasswordPolicy} (checked by the service). */
    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters")
    private String newPassword;
}
