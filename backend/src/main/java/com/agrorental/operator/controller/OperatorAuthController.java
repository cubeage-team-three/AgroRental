package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.AuthenticatedOperatorResponse;
import com.agrorental.operator.dto.OperatorLoginRequest;
import com.agrorental.operator.dto.OperatorLoginResponse;
import com.agrorental.operator.service.OperatorAuthService;
import com.agrorental.security.principal.OperatorPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller managing Operator authentication and session identity verification (FR-39).
 */
@Slf4j
@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorAuthController {

    private final OperatorAuthService operatorAuthService;

    /**
     * Authenticates an operator and issues a signed JWT access token.
     *
     * @param request Operator login credentials (mobile and password).
     * @return ApiResponse containing JWT token and safe operator profile.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<OperatorLoginResponse>> login(@Valid @RequestBody OperatorLoginRequest request) {
        log.info("REST request to authenticate operator with mobile: [PROTECTED]");
        OperatorLoginResponse response = operatorAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Operator login successful", response));
    }

    /**
     * Returns the safe profile identity of the currently authenticated operator.
     * Requires valid Bearer JWT authentication with ROLE_OPERATOR.
     *
     * @param principal Authenticated OperatorPrincipal injected from SecurityContext.
     * @return ApiResponse containing AuthenticatedOperatorResponse.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthenticatedOperatorResponse>> getCurrentOperator(
            @AuthenticationPrincipal OperatorPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            log.warn("Unauthorized /api/operators/me access attempt without valid principal");
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        log.info("REST request to fetch operator identity for operator ID: {}", principal.getId());
        AuthenticatedOperatorResponse response = operatorAuthService.getCurrentOperator(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Operator profile retrieved successfully", response));
    }
}
