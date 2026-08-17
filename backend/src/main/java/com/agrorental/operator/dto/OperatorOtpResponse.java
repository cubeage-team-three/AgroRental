package com.agrorental.operator.dto;

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
public class OperatorOtpResponse {

    private String mobileNumber;
    private Boolean mobileVerified;
    private String message;
    private Integer expiresInMinutes;
}
