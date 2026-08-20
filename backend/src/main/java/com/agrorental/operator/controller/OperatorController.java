package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.service.OperatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller exposing HTTP APIs for Operator Management module.
 */
@Slf4j
@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorService operatorService;

    /**
     * Registers a new operator in the system.
     *
     * @param request Validated registration payload
     * @return ResponseEntity with HTTP 201 Created and safe OperatorResponse payload
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OperatorResponse>> registerOperator(
            @Valid @RequestBody OperatorRegistrationRequest request) {
        log.info("Received operator registration request for mobile: {}", request.getMobileNumber());
        OperatorResponse response = operatorService.registerOperator(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Operator registered successfully", response));
    }
}