package com.agrorental.farmer.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.farmer.dto.FarmerRegisterRequest;
import com.agrorental.farmer.dto.FarmerResponse;
import com.agrorental.farmer.service.FarmerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/farmers")
@RequiredArgsConstructor
public class FarmerAuthController {

    private final FarmerService farmerService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<FarmerResponse>> registerFarmer(@Valid @RequestBody FarmerRegisterRequest request) {
        log.info("Received registration request for farmer: {}", request.getMobileNumber());
        FarmerResponse responseData = farmerService.registerFarmer(request);
        ApiResponse<FarmerResponse> response = ApiResponse.success("Farmer account created successfully. Please verify OTP.", responseData);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
