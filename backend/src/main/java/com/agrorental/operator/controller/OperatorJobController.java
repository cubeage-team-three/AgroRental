package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.security.JwtTokenProvider;
import com.agrorental.operator.dto.JobAssignRequest;
import com.agrorental.operator.dto.OperatorJobResponse;
import com.agrorental.operator.dto.OperatorJobSummaryResponse;
import com.agrorental.operator.entity.JobStatus;
import com.agrorental.operator.service.OperatorJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operators/jobs")
public class OperatorJobController {

    private final OperatorJobService operatorJobService;
    private final JwtTokenProvider jwtTokenProvider;

    public OperatorJobController(
            OperatorJobService operatorJobService,
            JwtTokenProvider jwtTokenProvider) {
        this.operatorJobService = operatorJobService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OperatorJobResponse>>> getAssignedJobs(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(value = "status", required = false) JobStatus status) {

        Long operatorId = extractOperatorIdFromHeader(authHeader);
        List<OperatorJobResponse> jobs = operatorJobService.getAssignedJobs(operatorId, status);

        return ResponseEntity.ok(ApiResponse.success("Assigned jobs fetched successfully", jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OperatorJobResponse>> getJobDetails(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable("id") Long id) {

        Long operatorId = extractOperatorIdFromHeader(authHeader);
        OperatorJobResponse job = operatorJobService.getJobDetails(operatorId, id);

        return ResponseEntity.ok(ApiResponse.success("Job details fetched successfully", job));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<OperatorJobSummaryResponse>> getJobsSummary(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        Long operatorId = extractOperatorIdFromHeader(authHeader);
        OperatorJobSummaryResponse summary = operatorJobService.getJobsSummary(operatorId);

        return ResponseEntity.ok(ApiResponse.success("Jobs summary fetched successfully", summary));
    }

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<OperatorJobResponse>> assignJob(
            @Valid @RequestBody JobAssignRequest request) {

        OperatorJobResponse response = operatorJobService.assignJob(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job assigned to operator successfully", response));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<OperatorJobResponse>> acceptJob(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable("id") Long id) {

        Long operatorId = extractOperatorIdFromHeader(authHeader);
        OperatorJobResponse response = operatorJobService.acceptJob(operatorId, id);

        return ResponseEntity.ok(ApiResponse.success("Job accepted successfully", response));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<OperatorJobResponse>> rejectJob(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason) {

        Long operatorId = extractOperatorIdFromHeader(authHeader);
        OperatorJobResponse response = operatorJobService.rejectJob(operatorId, id, reason);

        return ResponseEntity.ok(ApiResponse.success("Job rejected successfully", response));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OperatorJobResponse>> updateJobStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable("id") Long id,
            @Valid @RequestBody com.agrorental.operator.dto.JobStatusUpdateRequest request) {

        Long operatorId = extractOperatorIdFromHeader(authHeader);
        OperatorJobResponse response = operatorJobService.updateJobStatus(
                operatorId, id, request.getStatus(), request.getNotes());

        return ResponseEntity.ok(ApiResponse.success(
                "Job status updated to " + response.getStatus(), response));
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
