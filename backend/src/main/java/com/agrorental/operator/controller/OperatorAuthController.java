package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.operator.dto.OperatorLoginRequest;
import com.agrorental.operator.dto.OperatorLoginResponse;
import com.agrorental.operator.service.OperatorAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operators")
public class OperatorAuthController {

    private final OperatorAuthService operatorAuthService;

    public OperatorAuthController(OperatorAuthService operatorAuthService) {
        this.operatorAuthService = operatorAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<OperatorLoginResponse>> login(
            @Valid @RequestBody OperatorLoginRequest request) {

        OperatorLoginResponse response = operatorAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
