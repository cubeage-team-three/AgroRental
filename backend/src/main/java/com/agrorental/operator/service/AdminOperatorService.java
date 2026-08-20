package com.agrorental.operator.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.dto.OperatorDetailResponse;
import com.agrorental.operator.dto.OperatorSummaryResponse;
import com.agrorental.operator.dto.OperatorVerificationRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Administrator workflows for Operator review, verification, and lifecycle state transitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOperatorService {

    private final OperatorRepository operatorRepository;
    private final OperatorMapper operatorMapper;

    /**
     * Retrieves a paginated and filtered list of operators for Admin management.
     */
    @Transactional(readOnly = true)
    public Page<OperatorSummaryResponse> getOperators(OperatorStatus status, Boolean mobileVerified, Pageable pageable) {
        Page<Operator> operatorPage;

        if (status != null && mobileVerified != null) {
            operatorPage = operatorRepository.findByStatusAndMobileVerified(status, mobileVerified, pageable);
        } else if (status != null) {
            operatorPage = operatorRepository.findByStatus(status, pageable);
        } else if (mobileVerified != null) {
            operatorPage = operatorRepository.findByMobileVerified(mobileVerified, pageable);
        } else {
            operatorPage = operatorRepository.findAll(pageable);
        }

        return operatorPage.map(operatorMapper::toSummaryResponse);
    }

    /**
     * Retrieves full operator verification details, including masked government identifiers and KYC documents.
     */
    @Transactional(readOnly = true)
    public OperatorDetailResponse getOperatorDetail(Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + id));

        return operatorMapper.toDetailResponse(operator);
    }

    /**
     * Processes Admin approval or rejection for an Operator account.
     * Enforces strict state transitions: only PENDING operators can be approved or rejected.
     */
    @Transactional
    public OperatorDetailResponse verifyOperator(Long id, OperatorVerificationRequest request) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + id));

        if (operator.getStatus() != OperatorStatus.PENDING) {
            throw new BadRequestException("Operator cannot be verified because current status is: " + operator.getStatus());
        }

        OperatorStatus targetStatus = request.getStatus();
        if (targetStatus == null) {
            throw new BadRequestException("Verification status is required (APPROVED or REJECTED).");
        }

        if (targetStatus == OperatorStatus.APPROVED) {
            if (!operator.isMobileVerified()) {
                throw new BadRequestException("Operator mobile number must be verified before approval.");
            }
            operator.setStatus(OperatorStatus.APPROVED);
            operator.setActive(true);
            operator.setRejectionReason(null);
            log.info("Admin approved operator ID: {}", id);
        } else if (targetStatus == OperatorStatus.REJECTED) {
            String reason = request.getRejectionReason();
            if (reason == null || reason.trim().isEmpty()) {
                reason = "Application rejected by Admin.";
            }
            operator.setStatus(OperatorStatus.REJECTED);
            operator.setActive(false);
            operator.setRejectionReason(reason.trim());
            log.info("Admin rejected operator ID: {} with reason: {}", id, reason);
        } else {
            throw new BadRequestException("Invalid target status for verification: " + targetStatus);
        }

        Operator saved = operatorRepository.save(operator);
        return operatorMapper.toDetailResponse(saved);
    }
}
