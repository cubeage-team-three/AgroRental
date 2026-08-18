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
public class OperatorJobSummaryResponse {

    private Long operatorId;
    private long totalAssigned;
    private long pendingResponse;
    private long accepted;
    private long completed;
    private long rejected;
    private long cancelled;
}
