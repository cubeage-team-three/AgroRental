package com.agrorental.user.dto;

import com.agrorental.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object returned on successful User Registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean enabled;
    private boolean verified;
    private Long farmerId;
    private LocalDateTime createdAt;
    private String message;
}
