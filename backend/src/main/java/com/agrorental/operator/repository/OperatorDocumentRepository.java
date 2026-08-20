package com.agrorental.operator.repository;

import com.agrorental.operator.entity.OperatorDocument;
import com.agrorental.operator.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Operator KYC and certification documents.
 */
@Repository
public interface OperatorDocumentRepository extends JpaRepository<OperatorDocument, Long> {

    List<OperatorDocument> findByOperatorId(Long operatorId);

    Optional<OperatorDocument> findByOperatorIdAndDocumentType(Long operatorId, DocumentType documentType);
}
