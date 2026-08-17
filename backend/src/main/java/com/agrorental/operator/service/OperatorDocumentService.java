package com.agrorental.operator.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.dto.DocumentVerifyRequest;
import com.agrorental.operator.dto.OperatorDocumentResponse;
import com.agrorental.operator.dto.OperatorDocumentSummaryResponse;
import com.agrorental.operator.entity.DocumentStatus;
import com.agrorental.operator.entity.DocumentType;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorDocument;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorDocumentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OperatorDocumentService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final OperatorRepository operatorRepository;
    private final OperatorDocumentRepository operatorDocumentRepository;
    private final Path rootUploadPath;

    public OperatorDocumentService(
            OperatorRepository operatorRepository,
            OperatorDocumentRepository operatorDocumentRepository,
            @Value("${app.upload.operator-docs-dir:uploads/operator_documents}") String uploadDir) {
        this.operatorRepository = operatorRepository;
        this.operatorDocumentRepository = operatorDocumentRepository;
        this.rootUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.rootUploadPath);
            log.info("Initialized operator documents upload directory at: {}", this.rootUploadPath);
        } catch (IOException e) {
            log.error("Could not initialize upload directory: {}", e.getMessage());
            throw new RuntimeException("Could not initialize upload storage directory", e);
        }
    }

    @Transactional
    public OperatorDocumentResponse uploadDocument(
            String mobileNumber,
            DocumentType documentType,
            String documentNumber,
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a valid file to upload");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported file type. Allowed formats: PDF, JPG, PNG, WEBP");
        }

        Operator operator = operatorRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with mobile number: " + mobileNumber));

        if (!operator.isMobileVerified()) {
            throw new BadRequestException("Please complete mobile OTP verification before uploading documents");
        }

        // Sanitize filename
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Invalid filename path sequence: " + originalFilename);
        }

        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFilename.substring(dotIndex);
        }

        String uniqueFileName = String.format("OP_%d_%s_%s%s",
                operator.getId(),
                documentType.name(),
                UUID.randomUUID().toString().substring(0, 8),
                fileExtension
        );

        Path targetLocation = this.rootUploadPath.resolve(uniqueFileName);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Failed to store file: {}", ex.getMessage());
            throw new RuntimeException("Failed to store file on disk. Please try again.", ex);
        }

        // Check if document of this type already exists for operator
        Optional<OperatorDocument> existingDocOpt = operatorDocumentRepository
                .findByOperatorIdAndDocumentType(operator.getId(), documentType);

        OperatorDocument document;
        if (existingDocOpt.isPresent()) {
            document = existingDocOpt.get();
            document.setFileName(originalFilename);
            document.setFilePath(targetLocation.toString());
            document.setContentType(contentType);
            document.setFileSize(file.getSize());
            if (documentNumber != null && !documentNumber.isBlank()) {
                document.setDocumentNumber(documentNumber);
            }
            document.setVerificationStatus(DocumentStatus.PENDING);
            document.setRejectionReason(null);
            document.setVerifiedAt(null);
            document.setVerifiedBy(null);
        } else {
            String docNum = documentNumber;
            if ((docNum == null || docNum.isBlank())) {
                if (documentType == DocumentType.AADHAAR_CARD) {
                    docNum = operator.getAadhaarNumber();
                } else if (documentType == DocumentType.DRIVING_LICENSE) {
                    docNum = operator.getDrivingLicenseNumber();
                }
            }

            document = OperatorDocument.builder()
                    .operator(operator)
                    .documentType(documentType)
                    .documentNumber(docNum)
                    .fileName(originalFilename)
                    .filePath(targetLocation.toString())
                    .contentType(contentType)
                    .fileSize(file.getSize())
                    .verificationStatus(DocumentStatus.PENDING)
                    .build();
        }

        OperatorDocument savedDoc = operatorDocumentRepository.save(document);

        // Check if mandatory documents (Aadhaar & DL) are present
        boolean hasAadhaar = operatorDocumentRepository.findByOperatorIdAndDocumentType(operator.getId(), DocumentType.AADHAAR_CARD).isPresent();
        boolean hasDl = operatorDocumentRepository.findByOperatorIdAndDocumentType(operator.getId(), DocumentType.DRIVING_LICENSE).isPresent();

        if (hasAadhaar && hasDl) {
            operator.setDocumentsSubmitted(true);
            operatorRepository.save(operator);
        }

        log.info("Document {} uploaded successfully for operator mobile: {}", documentType, mobileNumber);

        return mapToResponse(savedDoc);
    }

    @Transactional(readOnly = true)
    public OperatorDocumentSummaryResponse getOperatorDocumentsSummary(String mobileNumber) {
        Operator operator = operatorRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with mobile number: " + mobileNumber));

        List<OperatorDocument> documents = operatorDocumentRepository.findAllByOperatorId(operator.getId());

        List<OperatorDocumentResponse> docResponses = documents.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return OperatorDocumentSummaryResponse.builder()
                .operatorId(operator.getId())
                .mobileNumber(operator.getMobileNumber())
                .fullName(operator.getFullName())
                .operatorStatus(operator.getStatus())
                .mobileVerified(operator.isMobileVerified())
                .documentsSubmitted(operator.isDocumentsSubmitted())
                .documents(docResponses)
                .build();
    }

    @Transactional
    public OperatorDocumentResponse verifyDocument(Long documentId, DocumentVerifyRequest request) {
        OperatorDocument document = operatorDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator document not found with id: " + documentId));

        if (request.getStatus() == DocumentStatus.REJECTED && (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new BadRequestException("Rejection reason is required when rejecting a document");
        }

        document.setVerificationStatus(request.getStatus());
        document.setRejectionReason(request.getStatus() == DocumentStatus.REJECTED ? request.getRejectionReason() : null);
        document.setVerifiedAt(LocalDateTime.now());
        document.setVerifiedBy(request.getVerifiedBy() != null && !request.getVerifiedBy().isBlank() ? request.getVerifiedBy() : "Admin");

        OperatorDocument savedDoc = operatorDocumentRepository.save(document);

        // Check if all mandatory documents for this operator are VERIFIED
        Operator operator = document.getOperator();
        List<OperatorDocument> allDocs = operatorDocumentRepository.findAllByOperatorId(operator.getId());

        Optional<OperatorDocument> aadhaarDoc = allDocs.stream().filter(d -> d.getDocumentType() == DocumentType.AADHAAR_CARD).findFirst();
        Optional<OperatorDocument> dlDoc = allDocs.stream().filter(d -> d.getDocumentType() == DocumentType.DRIVING_LICENSE).findFirst();

        boolean aadhaarVerified = aadhaarDoc.isPresent() && aadhaarDoc.get().getVerificationStatus() == DocumentStatus.VERIFIED;
        boolean dlVerified = dlDoc.isPresent() && dlDoc.get().getVerificationStatus() == DocumentStatus.VERIFIED;

        if (aadhaarVerified && dlVerified) {
            operator.setStatus(OperatorStatus.APPROVED);
            operatorRepository.save(operator);
            log.info("Operator {} status updated to APPROVED after document verification", operator.getMobileNumber());
        }

        return mapToResponse(savedDoc);
    }

    @Transactional(readOnly = true)
    public Resource loadDocumentAsResource(Long documentId) {
        OperatorDocument document = operatorDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        try {
            Path filePath = Paths.get(document.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found or unreadable for document id: " + documentId);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File path invalid for document id: " + documentId);
        }
    }

    @Transactional(readOnly = true)
    public OperatorDocument getDocument(Long documentId) {
        return operatorDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
    }

    private OperatorDocumentResponse mapToResponse(OperatorDocument doc) {
        return OperatorDocumentResponse.builder()
                .id(doc.getId())
                .documentType(doc.getDocumentType())
                .documentNumber(doc.getDocumentNumber())
                .fileName(doc.getFileName())
                .fileDownloadUrl("/api/operators/documents/" + doc.getId() + "/file")
                .contentType(doc.getContentType())
                .fileSize(doc.getFileSize())
                .verificationStatus(doc.getVerificationStatus())
                .rejectionReason(doc.getRejectionReason())
                .uploadedAt(doc.getCreatedAt())
                .verifiedAt(doc.getVerifiedAt())
                .verifiedBy(doc.getVerifiedBy())
                .build();
    }
}
