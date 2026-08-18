package com.agrorental.operator.dto;

import com.agrorental.operator.entity.DocumentStatus;
import com.agrorental.operator.entity.DocumentType;
import com.agrorental.operator.entity.OperatorStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorProfileResponse {

    private Long id;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String address;
    private String aadhaarNumber;
    private String drivingLicenseNumber;
    private Integer experience;
    private String skills;
    private String profilePhoto;
    private String profilePhotoUrl;
    private OperatorStatus status;
    private boolean mobileVerified;
    private boolean documentsSubmitted;
    private String partnerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<DocumentStatusSummary> documents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DocumentStatusSummary {
        private Long id;
        private DocumentType documentType;
        private String documentNumber;
        private String fileName;
        private String fileDownloadUrl;
        private DocumentStatus verificationStatus;
        private String rejectionReason;
        private LocalDateTime verifiedAt;
    }
}
