package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.service.OperatorService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/operators")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://localhost:5176"
})
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<Operator>>> getAllOperators() {
        List<Operator> list = operatorService.getAllOperators();
        return ResponseEntity.ok(ApiResponse.success("Operators retrieved successfully", list));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Operator>>> getAvailableOperators(
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Operator> list = operatorService.getAvailableOperators(partnerId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Available operators retrieved successfully", list));
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<ApiResponse<List<Operator>>> getOperatorsByPartner(@PathVariable Long partnerId) {
        List<Operator> list = operatorService.getOperatorsByPartner(partnerId);
        return ResponseEntity.ok(ApiResponse.success("Partner operators retrieved successfully", list));
    }
}