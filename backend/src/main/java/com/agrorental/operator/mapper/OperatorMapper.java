package com.agrorental.operator.mapper;

import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import org.springframework.stereotype.Component;

/**
 * Spring-managed Mapper component for converting between Operator DTOs and JPA Entities.
 * Contains pure object transformation logic with zero business or persistence dependencies.
 */
@Component
public class OperatorMapper {

    /**
     * Maps an OperatorRegistrationRequest DTO and encoded password into an Operator JPA Entity.
     *
     * @param request Validated registration request
     * @param encodedPassword Securely hashed password
     * @return Initialized Operator entity with PENDING status
     */
    public Operator toEntity(OperatorRegistrationRequest request, String encodedPassword) {
        if (request == null) {
            return null;
        }

        return Operator.builder()
                .fullName(request.getFullName() != null ? request.getFullName().trim() : null)
                .mobileNumber(request.getMobileNumber() != null ? request.getMobileNumber().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .aadhaarNumber(request.getAadhaarNumber() != null ? request.getAadhaarNumber().trim() : null)
                .drivingLicenseNumber(request.getDrivingLicenseNumber() != null ? request.getDrivingLicenseNumber().trim() : null)
                .experience(request.getExperience())
                .skills(request.getSkills() != null ? request.getSkills().trim() : null)
                .password(encodedPassword)
                .profilePhoto(request.getProfilePhoto() != null ? request.getProfilePhoto().trim() : null)
                .status(OperatorStatus.PENDING)
                .build();
    }

    /**
     * Maps an Operator JPA Entity to a secure OperatorResponse DTO.
     *
     * @param operator Operator domain entity
     * @return Safe client-facing OperatorResponse DTO
     */
    public OperatorResponse toResponse(Operator operator) {
        if (operator == null) {
            return null;
        }

        Long partnerId = operator.getPartner() != null ? operator.getPartner().getId() : null;

        return OperatorResponse.builder()
                .id(operator.getId())
                .fullName(operator.getFullName())
                .mobileNumber(operator.getMobileNumber())
                .email(operator.getEmail())
                .address(operator.getAddress())
                .experience(operator.getExperience())
                .skills(operator.getSkills())
                .profilePhoto(operator.getProfilePhoto())
                .status(operator.getStatus())
                .partnerId(partnerId)
                .active(operator.isActive())
                .createdAt(operator.getCreatedAt())
                .updatedAt(operator.getUpdatedAt())
                .build();
    }
}
