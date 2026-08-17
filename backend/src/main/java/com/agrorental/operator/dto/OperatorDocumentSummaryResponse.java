package com.agrorental.operator.dto;

import com.agrorental.operator.entity.OperatorStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorDocumentSummaryResponse {

    private Long operatorId;
    private String mobileNumber;
    private String fullName;
    private OperatorStatus operatorStatus;
    private Boolean mobileVerified;
    private Boolean documentsSubmitted;
    private List<OperatorDocumentResponse> documents;
}
