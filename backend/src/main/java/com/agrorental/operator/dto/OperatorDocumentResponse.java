package com.agrorental.operator.dto;

import com.agrorental.operator.enums.DocumentStatus;
import com.agrorental.operator.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Safe client-facing response DTO representing an Operator KYC/certification document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorDocumentResponse {

    private Long id;
    private DocumentType documentType;
    private String maskedDocumentNumber;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private DocumentStatus verificationStatus;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
