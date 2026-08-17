package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.service.OperatorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operators")
public class OperatorController {

    private final OperatorService operatorService;

    public OperatorController(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Operator>> registerOperator(
            @Valid @RequestBody OperatorRegistrationRequest request) {

        Operator operator = operatorService.registerOperator(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Operator registered successfully",
                        operator
                ));
    }
}