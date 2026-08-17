package com.agrorental.operator.dto;

import com.agrorental.operator.entity.DocumentStatus;
import com.agrorental.operator.entity.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorDocumentResponse {

    private Long id;
    private DocumentType documentType;
    private String documentNumber;
    private String fileName;
    private String fileDownloadUrl;
    private String contentType;
    private Long fileSize;
    private DocumentStatus verificationStatus;
    private String rejectionReason;
    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
}
