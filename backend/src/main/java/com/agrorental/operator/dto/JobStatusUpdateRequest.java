package com.agrorental.operator.dto;

import com.agrorental.operator.entity.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusUpdateRequest {

    @NotNull(message = "Job status is required")
    private JobStatus status;

    private String notes;
}
