package com.agrorental.operator.controller;

import com.agrorental.auth.service.AccountIdentifiers;
import com.agrorental.common.dto.ApiResponse;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.dto.OperatorSummaryResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.service.OperatorService;
import com.agrorental.security.principal.CurrentCaller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller exposing HTTP APIs for Operator Management module.
 */
@Slf4j
@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorService operatorService;
    private final OperatorMapper operatorMapper;

    /**
     * Registers a new operator in the system.
     *
     * @param request Validated registration payload
     * @return ResponseEntity with HTTP 201 Created and safe OperatorResponse payload
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OperatorResponse>> registerOperator(
            @Valid @RequestBody OperatorRegistrationRequest request) {
        log.info("Received operator registration request for mobile: {}", AccountIdentifiers.mask(request.getMobileNumber()));
        OperatorResponse response = operatorService.registerOperator(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Operator registered successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OperatorSummaryResponse>>> getAllOperators(Authentication authentication) {
        List<Operator> list = operatorService.getAllOperatorsForCaller(CurrentCaller.from(authentication));
        List<OperatorSummaryResponse> responseList = operatorMapper.toSummaryResponseList(list);
        return ResponseEntity.ok(ApiResponse.success("Operators retrieved successfully", responseList));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<OperatorSummaryResponse>>> getAvailableOperators(
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        List<Operator> list = operatorService.getAvailableOperatorsForCaller(partnerId, startDate, endDate,
                CurrentCaller.from(authentication));
        List<OperatorSummaryResponse> responseList = operatorMapper.toSummaryResponseList(list);
        return ResponseEntity.ok(ApiResponse.success("Available operators retrieved successfully", responseList));
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<ApiResponse<List<OperatorSummaryResponse>>> getOperatorsByPartner(@PathVariable Long partnerId,
                                                                                         Authentication authentication) {
        List<Operator> list = operatorService.getOperatorsByPartnerForCaller(partnerId, CurrentCaller.from(authentication));
        List<OperatorSummaryResponse> responseList = operatorMapper.toSummaryResponseList(list);
        return ResponseEntity.ok(ApiResponse.success("Partner operators retrieved successfully", responseList));
    }
}