package com.agrorental.complaint.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.complaint.dto.ComplaintResponse;
import com.agrorental.complaint.dto.ComplaintStatusUpdateRequest;
import com.agrorental.complaint.enums.ComplaintStatus;
import com.agrorental.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-side complaint oversight and resolution. Protected by the blanket
 * /api/admin/** -> hasRole("ADMIN") rule in SecurityConfig.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/complaints")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> getAllComplaints(
            @RequestParam(required = false) ComplaintStatus status) {
        log.info("Admin request to fetch complaints. status filter: {}", status);
        List<ComplaintResponse> response = status != null
                ? complaintService.getComplaintsByStatus(status)
                : complaintService.getAllComplaints();
        return ResponseEntity.ok(ApiResponse.success("Complaints retrieved successfully", response));
    }

    @GetMapping("/{complaintId}")
    public ResponseEntity<ApiResponse<ComplaintResponse>> getComplaintById(@PathVariable Long complaintId) {
        ComplaintResponse response = complaintService.getComplaintById(complaintId);
        return ResponseEntity.ok(ApiResponse.success("Complaint retrieved successfully", response));
    }

    @PatchMapping("/{complaintId}/status")
    public ResponseEntity<ApiResponse<ComplaintResponse>> updateStatus(
            @PathVariable Long complaintId,
            @Valid @RequestBody ComplaintStatusUpdateRequest request) {
        log.info("Admin updating complaint {} to status {}", complaintId, request.getStatus());
        ComplaintResponse response = complaintService.updateStatus(
                complaintId, request.getStatus(), request.getResolutionNote());
        return ResponseEntity.ok(ApiResponse.success("Complaint status updated successfully", response));
    }
}
