package com.agrorental.operator.dto;

import com.agrorental.operator.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for submitting Operator KYC and certification document metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorDocumentCreateRequest {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    private String documentNumber;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File URL is required")
    private String fileUrl;

    private Long fileSize;

    private String mimeType;
}
