package com.agrorental.operator.service;

import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.AuthenticatedOperatorResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticated operator identity (FR-39). Credential checks and token issuance for operators live in
 * {@link com.agrorental.auth.service.AuthService} (POST /api/auth/login, role OPERATOR).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorAuthService {

    private final OperatorRepository operatorRepository;
    private final OperatorMapper operatorMapper;

    /**
     * Retrieves the safe authenticated operator identity for the currently logged-in principal.
     *
     * @param operatorId Primary key ID of the authenticated operator.
     * @return AuthenticatedOperatorResponse with safe identity fields.
     */
    @Transactional(readOnly = true)
    public AuthenticatedOperatorResponse getCurrentOperator(Long operatorId) {
        if (operatorId == null) {
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new UnauthorizedException("Operator account not found"));

        if (!operator.isActive() || operator.getStatus() != OperatorStatus.APPROVED) {
            throw new ForbiddenException("Operator account is no longer active or approved");
        }

        return operatorMapper.toAuthenticatedResponse(operator);
    }
}
