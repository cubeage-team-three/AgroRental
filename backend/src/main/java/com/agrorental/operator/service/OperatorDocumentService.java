package com.agrorental.operator.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.dto.OperatorDocumentCreateRequest;
import com.agrorental.operator.dto.OperatorDocumentResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorDocument;
import com.agrorental.operator.enums.DocumentStatus;
import com.agrorental.operator.mapper.OperatorDocumentMapper;
import com.agrorental.operator.repository.OperatorDocumentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service managing Operator KYC and certification document metadata.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorDocumentService {

    private final OperatorDocumentRepository documentRepository;
    private final OperatorRepository operatorRepository;
    private final OperatorDocumentMapper documentMapper;

    /**
     * Submits or updates a KYC document record for an Operator.
     */
    @Transactional
    public OperatorDocumentResponse addDocument(Long operatorId, OperatorDocumentCreateRequest request) {
        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operatorId));

        // Check if document of this type already exists, if so update metadata
        Optional<OperatorDocument> existingOpt = documentRepository
                .findByOperatorIdAndDocumentType(operatorId, request.getDocumentType());

        OperatorDocument document;
        if (existingOpt.isPresent()) {
            document = existingOpt.get();
            document.setFileName(request.getFileName().trim());
            document.setFileUrl(request.getFileUrl().trim());
            document.setDocumentNumber(request.getDocumentNumber() != null ? request.getDocumentNumber().trim() : document.getDocumentNumber());
            document.setFileSize(request.getFileSize());
            document.setMimeType(request.getMimeType());
            document.setVerificationStatus(DocumentStatus.PENDING);
            document.setRejectionReason(null);
        } else {
            document = documentMapper.toEntity(request, operator);
            operator.addDocument(document);
        }

        OperatorDocument saved = documentRepository.save(document);
        log.info("Saved {} document for operator ID: {}", request.getDocumentType(), operatorId);

        return documentMapper.toResponse(saved);
    }

    /**
     * Retrieves all KYC documents for an Operator.
     */
    @Transactional(readOnly = true)
    public List<OperatorDocumentResponse> getOperatorDocuments(Long operatorId) {
        if (!operatorRepository.existsById(operatorId)) {
            throw new ResourceNotFoundException("Operator not found with ID: " + operatorId);
        }
        List<OperatorDocument> docs = documentRepository.findByOperatorId(operatorId);
        return documentMapper.toResponseList(docs);
    }

    /**
     * Updates an individual document's verification status (e.g. during Admin review).
     */
    @Transactional
    public OperatorDocumentResponse updateDocumentStatus(Long documentId, DocumentStatus status, String rejectionReason) {
        OperatorDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        if (status == null) {
            throw new BadRequestException("Verification status is required.");
        }

        document.setVerificationStatus(status);
        if (status == DocumentStatus.REJECTED) {
            document.setRejectionReason(rejectionReason != null ? rejectionReason.trim() : "Document rejected by Admin.");
        } else {
            document.setRejectionReason(null);
        }

        OperatorDocument saved = documentRepository.save(document);
        return documentMapper.toResponse(saved);
    }
}
