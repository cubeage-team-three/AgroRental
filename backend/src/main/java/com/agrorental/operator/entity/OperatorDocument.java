package com.agrorental.operator.entity;

import com.agrorental.common.entity.BaseEntity;
import com.agrorental.operator.enums.DocumentStatus;
import com.agrorental.operator.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA Domain Entity representing uploaded KYC/certification document metadata for an Operator.
 */
@Entity
@Table(name = "operator_documents")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    @ToString.Exclude
    private Operator operator;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    @Builder.Default
    private DocumentStatus verificationStatus = DocumentStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;
}
