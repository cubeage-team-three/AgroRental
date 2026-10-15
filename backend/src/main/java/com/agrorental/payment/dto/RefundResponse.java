package com.agrorental.payment.dto;

import com.agrorental.payment.entity.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Admin view of a refund obligation and its audit history. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private Long id;
    private Long paymentId;
    private Long bookingId;
    private Long farmerId;
    private Long partnerId;
    private BigDecimal amount;
    private RefundStatus status;
    private String initiationReason;
    private LocalDateTime initiatedAt;
    private String initiatedBy;
    private Integer attemptCount;
    private String transferReference;
    private LocalDateTime completedAt;
    private String completedBy;
    private String lastFailureReason;
    private LocalDateTime lastFailedAt;
    private String lastFailedBy;
    private List<Event> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Event {
        private RefundStatus fromStatus;
        private RefundStatus toStatus;
        private String actor;
        private String note;
        private String transferReference;
        private LocalDateTime occurredAt;
    }
}
