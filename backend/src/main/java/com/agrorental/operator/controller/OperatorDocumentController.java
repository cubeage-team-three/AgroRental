package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.operator.dto.DocumentVerifyRequest;
import com.agrorental.operator.dto.OperatorDocumentResponse;
import com.agrorental.operator.dto.OperatorDocumentSummaryResponse;
import com.agrorental.operator.entity.DocumentType;
import com.agrorental.operator.entity.OperatorDocument;
import com.agrorental.operator.service.OperatorDocumentService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/operators/documents")
public class OperatorDocumentController {

    private final OperatorDocumentService operatorDocumentService;

    public OperatorDocumentController(OperatorDocumentService operatorDocumentService) {
        this.operatorDocumentService = operatorDocumentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OperatorDocumentResponse>> uploadDocument(
            @RequestParam("mobileNumber") String mobileNumber,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam("file") MultipartFile file) {

        OperatorDocumentResponse response = operatorDocumentService.uploadDocument(
                mobileNumber, documentType, documentNumber, file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OperatorDocumentSummaryResponse>> getOperatorDocuments(
            @RequestParam("mobileNumber") String mobileNumber) {

        OperatorDocumentSummaryResponse response =
                operatorDocumentService.getOperatorDocumentsSummary(mobileNumber);

        return ResponseEntity.ok(ApiResponse.success("Operator documents fetched successfully", response));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getDocumentFile(@PathVariable("id") Long id) {
        OperatorDocument document = operatorDocumentService.getDocument(id);
        Resource resource = operatorDocumentService.loadDocumentAsResource(id);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (document.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(document.getContentType());
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<OperatorDocumentResponse>> verifyDocument(
            @PathVariable("id") Long id,
            @Valid @RequestBody DocumentVerifyRequest request) {

        OperatorDocumentResponse response = operatorDocumentService.verifyDocument(id, request);
        return ResponseEntity.ok(ApiResponse.success("Document verification updated successfully", response));
    }
}
