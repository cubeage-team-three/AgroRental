package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.operator.dto.OperatorDocumentCreateRequest;
import com.agrorental.operator.dto.OperatorDocumentResponse;
import com.agrorental.operator.service.OperatorDocumentService;
import com.agrorental.security.principal.OperatorOnboardingPrincipal;
import com.agrorental.security.principal.OperatorPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
     * Submits a KYC or certification document record for an Operator. Only the owning operator (an approved
     * operator session, or the registration onboarding token of a still PENDING operator) or an admin may do
     * so; the path operatorId is never proof of ownership.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OperatorDocumentResponse>> uploadDocument(
            @PathVariable Long operatorId,
            @Valid @RequestBody OperatorDocumentCreateRequest request,
            Authentication authentication) {
        boolean administrator = isAdministrator(authentication);
        if (!administrator && !isOwningOperator(authentication, operatorId)) {
            log.warn("Rejected document upload for operator ID {} by a non-owner", operatorId);
            throw new AccessDeniedException("You can only manage your own documents.");
        }
        log.info("Received document upload request for operator ID: {}", operatorId);
        OperatorDocumentResponse response = documentService.addDocument(operatorId, request, administrator);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", response));
    }

    /**
     * Lists all uploaded KYC documents for an Operator. Documents contain identity numbers, so reads
     * are restricted to administrators.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OperatorDocumentResponse>>> getDocuments(
            @PathVariable Long operatorId,
            Authentication authentication) {
        if (!isAdministrator(authentication)) {
            throw new AccessDeniedException("Only administrators can view operator documents.");
        }
        List<OperatorDocumentResponse> response = documentService.getOperatorDocuments(operatorId);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", response));
    }

    private boolean isOwningOperator(Authentication authentication, Long operatorId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OperatorPrincipal operator) {
            return operatorId.equals(operator.getId());
        }
        if (principal instanceof OperatorOnboardingPrincipal onboarding) {
            return operatorId.equals(onboarding.operatorId());
        }
        return false;
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
