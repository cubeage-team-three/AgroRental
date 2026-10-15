package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.AuthenticatedOperatorResponse;
import com.agrorental.operator.service.OperatorAuthService;
import com.agrorental.security.principal.OperatorPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator session identity (FR-39). Operators sign in through the shared POST /api/auth/login
 * (role OPERATOR); the former duplicate login endpoint is retired.
 */
@Slf4j
@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorAuthController {

    public static final String LOGIN_RETIRED_MESSAGE =
            "This login endpoint has been retired. Sign in with POST /api/auth/login using role OPERATOR.";

    private final OperatorAuthService operatorAuthService;

    /**
     * Retired duplicate of POST /api/auth/login (no client uses it). Answers 410 Gone without reading the
     * request, so it never authenticates anyone and never reveals whether an account exists.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login() {
        log.info("Rejected call to the retired operator login endpoint");
        return ResponseEntity.status(HttpStatus.GONE).body(ApiResponse.error(LOGIN_RETIRED_MESSAGE));
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
