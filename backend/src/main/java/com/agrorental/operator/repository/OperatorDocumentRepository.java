package com.agrorental.operator.repository;

import com.agrorental.operator.entity.DocumentType;
import com.agrorental.operator.entity.OperatorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorDocumentRepository extends JpaRepository<OperatorDocument, Long> {

    List<OperatorDocument> findAllByOperatorId(Long operatorId);

    Optional<OperatorDocument> findByOperatorIdAndDocumentType(Long operatorId, DocumentType documentType);

    List<OperatorDocument> findAllByOperatorMobileNumber(String mobileNumber);

    Optional<OperatorDocument> findByOperatorMobileNumberAndDocumentType(String mobileNumber, DocumentType documentType);
}
