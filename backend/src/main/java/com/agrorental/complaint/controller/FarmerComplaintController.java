package com.agrorental.complaint.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.complaint.dto.ComplaintCreateRequest;
import com.agrorental.complaint.dto.ComplaintResponse;
import com.agrorental.complaint.service.ComplaintService;
import com.agrorental.security.principal.FarmerPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farmers/complaints")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class FarmerComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<ApiResponse<ComplaintResponse>> createComplaint(
            @AuthenticationPrincipal FarmerPrincipal principal,
            @Valid @RequestBody ComplaintCreateRequest request) {
        request.setFarmerId(principal.getId());
        ComplaintResponse response = complaintService.createComplaint(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Complaint submitted successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> getFarmerComplaints(
            @AuthenticationPrincipal FarmerPrincipal principal) {
        List<ComplaintResponse> response = complaintService.getComplaintsByFarmerId(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Farmer complaints retrieved successfully", response));
    }

    @GetMapping("/{complaintId}")
    public ResponseEntity<ApiResponse<ComplaintResponse>> getComplaintById(@PathVariable Long complaintId) {
        ComplaintResponse response = complaintService.getComplaintById(complaintId);
        return ResponseEntity.ok(ApiResponse.success("Complaint details retrieved successfully", response));
    }
}
