package com.agrorental.operator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorLoginRequest {

    @NotBlank(message = "Mobile number or email is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
