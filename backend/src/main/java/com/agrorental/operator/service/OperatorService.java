package com.agrorental.operator.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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
    private final BookingRepository bookingRepository;

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

    @Transactional(readOnly = true)
    public List<Operator> getAllOperators() {
        return operatorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Operator> getApprovedOperators() {
        return operatorRepository.findByStatus(OperatorStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public List<Operator> getOperatorsByPartner(Long partnerId) {
        if (partnerId != null) {
            return operatorRepository.findByPartnerId(partnerId);
        }
        return operatorRepository.findAll();
    }

    /**
     * Retrieves operators who are APPROVED and do NOT have conflicting bookings during requested dates.
     */
    @Transactional(readOnly = true)
    public List<Operator> getAvailableOperators(Long partnerId, LocalDate startDate, LocalDate endDate) {
        List<Operator> approvedOperators;
        if (partnerId != null) {
            approvedOperators = operatorRepository.findAvailableOperatorsForPartner(OperatorStatus.APPROVED, partnerId);
            if (approvedOperators.isEmpty()) {
                approvedOperators = operatorRepository.findByStatus(OperatorStatus.APPROVED);
            }
        } else {
            approvedOperators = operatorRepository.findByStatus(OperatorStatus.APPROVED);
        }

        if (startDate == null || endDate == null || bookingRepository == null) {
            return approvedOperators;
        }

        // Filter out operators who have active bookings overlapping [startDate, endDate]
        return approvedOperators.stream()
                .filter(op -> {
                    List<Booking> activeBookings = bookingRepository.findByOperatorId(op.getId());
                    boolean hasOverlap = activeBookings.stream()
                            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED ||
                                         b.getStatus() == BookingStatus.ACCEPTED ||
                                         b.getStatus() == BookingStatus.OPERATOR_ASSIGNED ||
                                         b.getStatus() == BookingStatus.ON_THE_WAY ||
                                         b.getStatus() == BookingStatus.WORK_STARTED)
                            .anyMatch(b -> !b.getStartDate().isAfter(endDate) && !b.getEndDate().isBefore(startDate));
                    return !hasOverlap;
                })
                .collect(Collectors.toList());
    }
}
