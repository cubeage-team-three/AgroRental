package com.agrorental.operator.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation managing Operator domain logic, registration, and data transformation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorService {

    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperatorMapper operatorMapper;

    /**
     * Registers a new equipment operator with PENDING verification status.
     *
     * @param request Validated registration payload
     * @return Safe OperatorResponse DTO
     */
    @Transactional
    public OperatorResponse registerOperator(OperatorRegistrationRequest request) {
        String mobile = request.getMobileNumber().trim();
        log.info("Processing operator registration for mobile: {}", mobile);

        if (operatorRepository.existsByMobileNumber(mobile)) {
            throw new BadRequestException("Mobile number is already registered");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty() &&
                operatorRepository.existsByEmail(request.getEmail().trim())) {
            throw new BadRequestException("Email is already registered");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Operator operator = operatorMapper.toEntity(request, encodedPassword);

        Operator savedOperator = operatorRepository.save(operator);
        log.info("Successfully registered operator with ID: {}", savedOperator.getId());

        return operatorMapper.toResponse(savedOperator);
    }
}
