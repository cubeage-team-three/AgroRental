package com.agrorental.operator.dto;

import com.agrorental.operator.entity.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorWorkMilestoneResponse {

    private Long id;
    private JobStatus status;
    private LocalDateTime timestamp;
    private String notes;
}
