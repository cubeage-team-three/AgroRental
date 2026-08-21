package com.agrorental.operator.mapper;

import com.agrorental.operator.dto.OperatorDocumentCreateRequest;
import com.agrorental.operator.dto.OperatorDocumentResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorDocument;
import com.agrorental.operator.enums.DocumentStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring-managed Mapper component for Operator KYC/certification documents.
 */
@Component
public class OperatorDocumentMapper {

    public OperatorDocument toEntity(OperatorDocumentCreateRequest request, Operator operator) {
        if (request == null) {
            return null;
        }

        return OperatorDocument.builder()
                .operator(operator)
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber() != null ? request.getDocumentNumber().trim() : null)
                .fileName(request.getFileName() != null ? request.getFileName().trim() : null)
                .fileUrl(request.getFileUrl() != null ? request.getFileUrl().trim() : null)
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType() != null ? request.getMimeType().trim() : null)
                .verificationStatus(DocumentStatus.PENDING)
                .build();
    }

    public OperatorDocumentResponse toResponse(OperatorDocument document) {
        if (document == null) {
            return null;
        }

        return OperatorDocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .maskedDocumentNumber(maskDocumentNumber(document.getDocumentNumber()))
                .fileName(document.getFileName())
                .fileUrl(document.getFileUrl())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .verificationStatus(document.getVerificationStatus())
                .rejectionReason(document.getRejectionReason())
                .createdAt(document.getCreatedAt())
                .build();
    }

    public List<OperatorDocumentResponse> toResponseList(List<OperatorDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        return documents.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public static String maskDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.trim().isEmpty()) {
            return null;
        }
        String clean = documentNumber.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        return "XXXX-XXXX-" + clean.substring(clean.length() - 4);
    }
}
