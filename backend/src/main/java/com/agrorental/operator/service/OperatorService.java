package com.agrorental.operator.service;

import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class OperatorService {

    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;

    public OperatorService(
            OperatorRepository operatorRepository,
            PasswordEncoder passwordEncoder) {

        this.operatorRepository = operatorRepository;
        this.passwordEncoder = passwordEncoder;
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
}
