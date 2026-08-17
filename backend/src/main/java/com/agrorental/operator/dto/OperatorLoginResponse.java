package com.agrorental.operator.dto;

import com.agrorental.operator.entity.OperatorStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorLoginResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresIn;

    private OperatorProfileSummary operator;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OperatorProfileSummary {
        private Long id;
        private String fullName;
        private String mobileNumber;
        private String email;
        private String address;
        private Integer experience;
        private String skills;
        private String profilePhoto;
        private OperatorStatus status;
        private Boolean mobileVerified;
        private Boolean documentsSubmitted;
    }
}
