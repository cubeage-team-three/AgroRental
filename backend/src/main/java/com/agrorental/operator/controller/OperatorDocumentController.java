package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.operator.dto.OperatorDocumentCreateRequest;
import com.agrorental.operator.dto.OperatorDocumentResponse;
import com.agrorental.operator.service.OperatorDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Operator KYC document submission and inspection.
 */
@Slf4j
@RestController
@RequestMapping("/api/operators/{operatorId}/documents")
@RequiredArgsConstructor
public class OperatorDocumentController {

    private final OperatorDocumentService documentService;

    /**
     * Submits a KYC or certification document record for an Operator.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OperatorDocumentResponse>> uploadDocument(
            @PathVariable Long operatorId,
            @Valid @RequestBody OperatorDocumentCreateRequest request) {
        log.info("Received document upload request for operator ID: {}", operatorId);
        OperatorDocumentResponse response = documentService.addDocument(operatorId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", response));
    }

    /**
     * Lists all uploaded KYC documents for an Operator.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OperatorDocumentResponse>>> getDocuments(
            @PathVariable Long operatorId) {
        List<OperatorDocumentResponse> response = documentService.getOperatorDocuments(operatorId);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", response));
    }
}
