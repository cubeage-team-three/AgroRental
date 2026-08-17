package com.agrorental.operator.dto;

import com.agrorental.operator.entity.OperatorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorRegistrationResponse {

    private Long id;

    private String fullName;

    private String mobileNumber;

    private String email;

    private String address;

    private Integer experience;

    private String skills;

    private String profilePhoto;

    private OperatorStatus status;

    private boolean mobileVerified;
}
