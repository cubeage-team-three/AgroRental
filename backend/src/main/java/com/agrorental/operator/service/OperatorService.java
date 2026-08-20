package com.agrorental.operator.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OperatorService {

    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingRepository bookingRepository;

    public OperatorService(
            OperatorRepository operatorRepository,
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) BookingRepository bookingRepository) {

        this.operatorRepository = operatorRepository;
        this.passwordEncoder = passwordEncoder;
        this.bookingRepository = bookingRepository;
    }

    public Operator registerOperator(
            @Valid OperatorRegistrationRequest request) {

        if (operatorRepository.existsByMobileNumber(
                request.getMobileNumber())) {

            throw new IllegalArgumentException(
                    "Mobile number is already registered"
            );
        }

        Operator operator = new Operator();

        operator.setFullName(request.getFullName());
        operator.setMobileNumber(request.getMobileNumber());
        operator.setEmail(request.getEmail());
        operator.setAddress(request.getAddress());
        operator.setAadhaarNumber(request.getAadhaarNumber());
        operator.setDrivingLicenseNumber(
                request.getDrivingLicenseNumber()
        );
        operator.setExperience(request.getExperience());
        operator.setSkills(request.getSkills());

        // Store encrypted password
        operator.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        operator.setProfilePhoto(request.getProfilePhoto());

        // New operators require approval
        operator.setStatus(OperatorStatus.PENDING);

        return operatorRepository.save(operator);
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
                                         b.getStatus() == BookingStatus.WORK_STARTED)
                            .anyMatch(b -> !b.getStartDate().isAfter(endDate) && !b.getEndDate().isBefore(startDate));
                    return !hasOverlap;
                })
                .collect(Collectors.toList());
    }
}
