package com.agrorental.operator.mapper;

import com.agrorental.operator.dto.AuthenticatedOperatorResponse;
import com.agrorental.operator.dto.OperatorDetailResponse;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.dto.OperatorSummaryResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring-managed Mapper component for converting between Operator DTOs and JPA Entities.
 * Contains pure object transformation and security masking logic with zero persistence dependencies.
 */
@Component
@RequiredArgsConstructor
public class OperatorMapper {

    private final OperatorDocumentMapper documentMapper;

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
                .mobileVerified(false)
                .build();
    }

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
                .mobileVerified(operator.isMobileVerified())
                .rejectionReason(operator.getRejectionReason())
                .partnerId(partnerId)
                .active(operator.isActive())
                .createdAt(operator.getCreatedAt())
                .updatedAt(operator.getUpdatedAt())
                .build();
    }

    public OperatorSummaryResponse toSummaryResponse(Operator operator) {
        if (operator == null) {
            return null;
        }

        Long partnerId = operator.getPartner() != null ? operator.getPartner().getId() : null;
        int docCount = operator.getDocuments() != null ? operator.getDocuments().size() : 0;

        return OperatorSummaryResponse.builder()
                .id(operator.getId())
                .fullName(operator.getFullName())
                .mobileNumber(operator.getMobileNumber())
                .email(operator.getEmail())
                .experience(operator.getExperience())
                .skills(operator.getSkills())
                .profilePhoto(operator.getProfilePhoto())
                .status(operator.getStatus())
                .mobileVerified(operator.isMobileVerified())
                .documentsCount(docCount)
                .partnerId(partnerId)
                .active(operator.isActive())
                .createdAt(operator.getCreatedAt())
                .build();
    }

    public List<OperatorSummaryResponse> toSummaryResponseList(List<Operator> operators) {
        if (operators == null || operators.isEmpty()) {
            return Collections.emptyList();
        }
        return operators.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    public OperatorDetailResponse toDetailResponse(Operator operator) {
        if (operator == null) {
            return null;
        }

        Long partnerId = operator.getPartner() != null ? operator.getPartner().getId() : null;
        String partnerName = operator.getPartner() != null ? operator.getPartner().getFullName() : null;

        return OperatorDetailResponse.builder()
                .id(operator.getId())
                .fullName(operator.getFullName())
                .mobileNumber(operator.getMobileNumber())
                .email(operator.getEmail())
                .address(operator.getAddress())
                .maskedAadhaarNumber(maskAadhaar(operator.getAadhaarNumber()))
                .maskedDrivingLicenseNumber(maskDrivingLicense(operator.getDrivingLicenseNumber()))
                .experience(operator.getExperience())
                .skills(operator.getSkills())
                .profilePhoto(operator.getProfilePhoto())
                .status(operator.getStatus())
                .mobileVerified(operator.isMobileVerified())
                .rejectionReason(operator.getRejectionReason())
                .partnerId(partnerId)
                .partnerName(partnerName)
                .active(operator.isActive())
                .documents(documentMapper != null ? documentMapper.toResponseList(operator.getDocuments()) : Collections.emptyList())
                .createdAt(operator.getCreatedAt())
                .updatedAt(operator.getUpdatedAt())
                .build();
    }

    public AuthenticatedOperatorResponse toAuthenticatedResponse(Operator operator) {
        if (operator == null) {
            return null;
        }

        return AuthenticatedOperatorResponse.builder()
                .id(operator.getId())
                .fullName(operator.getFullName())
                .mobileNumber(operator.getMobileNumber())
                .email(operator.getEmail())
                .status(operator.getStatus())
                .mobileVerified(operator.isMobileVerified())
                .active(operator.isActive())
                .role("OPERATOR")
                .build();
    }

    public com.agrorental.operator.dto.OperatorProfileResponse toProfileResponse(Operator operator) {
        if (operator == null) {
            return null;
        }

        Long partnerId = operator.getPartner() != null ? operator.getPartner().getId() : null;
        String partnerName = operator.getPartner() != null ? operator.getPartner().getFullName() : null;

        return com.agrorental.operator.dto.OperatorProfileResponse.builder()
                .id(operator.getId())
                .fullName(operator.getFullName())
                .mobileNumber(operator.getMobileNumber())
                .email(operator.getEmail())
                .address(operator.getAddress())
                .experience(operator.getExperience())
                .skills(operator.getSkills())
                .profilePhoto(operator.getProfilePhoto())
                .maskedAadhaarNumber(maskAadhaar(operator.getAadhaarNumber()))
                .maskedDrivingLicenseNumber(maskDrivingLicense(operator.getDrivingLicenseNumber()))
                .hourlyRate(operator.getHourlyRate())
                .status(operator.getStatus())
                .mobileVerified(operator.isMobileVerified())
                .active(operator.isActive())
                .partnerId(partnerId)
                .partnerName(partnerName)
                .createdAt(operator.getCreatedAt())
                .updatedAt(operator.getUpdatedAt())
                .build();
    }

    public static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.trim().isEmpty()) {
            return null;
        }
        String clean = aadhaar.trim().replaceAll("\\s+", "");
        if (clean.length() <= 4) {
            return "XXXX-XXXX-XXXX";
        }
        return "XXXX-XXXX-" + clean.substring(clean.length() - 4);
    }

    public static String maskDrivingLicense(String dl) {
        if (dl == null || dl.trim().isEmpty()) {
            return null;
        }
        String clean = dl.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        return "DL-XXXX-" + clean.substring(clean.length() - 4);
    }
}
