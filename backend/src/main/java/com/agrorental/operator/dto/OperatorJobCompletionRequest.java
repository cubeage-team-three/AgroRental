package com.agrorental.operator.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload submitted by an Operator when marking an assigned job completed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorJobCompletionRequest {

    @Size(max = 1000, message = "Completion notes must not exceed 1000 characters")
    private String completionNotes;
}
