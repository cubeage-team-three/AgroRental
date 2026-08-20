package com.agrorental.operator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response payload returned on successful Operator login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorLoginResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn;

    private AuthenticatedOperatorResponse operator;
}
