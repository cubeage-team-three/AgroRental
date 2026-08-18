package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.security.JwtTokenProvider;
import com.agrorental.operator.dto.OperatorProfileResponse;
import com.agrorental.operator.dto.OperatorProfileUpdateRequest;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorRegistrationResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.service.OperatorService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/operators")
public class OperatorController {

    private final OperatorService operatorService;
    private final JwtTokenProvider jwtTokenProvider;

    public OperatorController(
            OperatorService operatorService,
            JwtTokenProvider jwtTokenProvider) {
        this.operatorService = operatorService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OperatorRegistrationResponse>> registerOperator(
            @Valid @RequestBody OperatorRegistrationRequest request) {

        Operator operator = operatorService.registerOperator(request);

        OperatorRegistrationResponse response =
                OperatorRegistrationResponse.builder()
                        .id(operator.getId())
                        .fullName(operator.getFullName())
                        .mobileNumber(operator.getMobileNumber())
                        .email(operator.getEmail())
                        .address(operator.getAddress())
                        .experience(operator.getExperience())
                        .skills(operator.getSkills())
                        .profilePhoto(operator.getProfilePhoto())
                        .status(operator.getStatus())
                        .mobileVerified(operator.isMobileVerified())
                        .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Operator registered successfully",
                        response
                ));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<OperatorProfileResponse>> getProfile(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        Long operatorId = extractOperatorIdFromHeader(authHeader);
        OperatorProfileResponse response = operatorService.getOperatorProfile(operatorId);
        return ResponseEntity.ok(ApiResponse.success("Operator profile fetched successfully", response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<OperatorProfileResponse>> updateProfile(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody OperatorProfileUpdateRequest request) {

        Long operatorId = extractOperatorIdFromHeader(authHeader);
        OperatorProfileResponse response = operatorService.updateOperatorProfile(operatorId, request);
        return ResponseEntity.ok(ApiResponse.success("Operator profile updated successfully", response));
    }

    @PostMapping(value = "/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OperatorProfileResponse>> uploadProfilePhoto(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam("file") MultipartFile file) {

        Long operatorId = extractOperatorIdFromHeader(authHeader);
        OperatorProfileResponse response = operatorService.uploadProfilePhoto(operatorId, file);
        return ResponseEntity.ok(ApiResponse.success("Profile photo uploaded successfully", response));
    }

    @GetMapping("/profile/photo/{fileName}")
    public ResponseEntity<Resource> getProfilePhoto(@PathVariable("fileName") String fileName) {
        Resource resource = operatorService.loadProfilePhotoAsResource(fileName);

        MediaType mediaType = MediaType.IMAGE_JPEG;
        try {
            String probeType = Files.probeContentType(Paths.get(fileName));
            if (probeType != null) {
                mediaType = MediaType.parseMediaType(probeType);
            }
        } catch (IOException ignored) {
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    private Long extractOperatorIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Unauthorized: Missing or invalid Bearer authentication token");
        }

        String token = authHeader.substring(7).trim();
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BadRequestException("Unauthorized: Expired or invalid authentication token");
        }

        Long operatorId = jwtTokenProvider.getOperatorIdFromToken(token);
        if (operatorId == null) {
            throw new BadRequestException("Unauthorized: Token does not contain a valid operator identity");
        }

        return operatorId;
    }
}