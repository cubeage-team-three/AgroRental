package com.agrorental.booking.dto;

import com.agrorental.booking.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for updating booking status or assigning an operator.
 */
public class BookingStatusUpdateRequest {

    @NotNull(message = "Status is mandatory")
    private BookingStatus status;

    private Long operatorId;

    private String rejectionReason;

    public BookingStatusUpdateRequest() {}

    public BookingStatusUpdateRequest(BookingStatus status, Long operatorId, String rejectionReason) {
        this.status = status;
        this.operatorId = operatorId;
        this.rejectionReason = rejectionReason;
    }

    public BookingStatusUpdateRequest(BookingStatus status, Long operatorId) {
        this(status, operatorId, null);
    }

    public static BookingStatusUpdateRequestBuilder builder() {
        return new BookingStatusUpdateRequestBuilder();
    }

    public static class BookingStatusUpdateRequestBuilder {
        private BookingStatus status;
        private Long operatorId;
        private String rejectionReason;

        BookingStatusUpdateRequestBuilder() {}

        public BookingStatusUpdateRequestBuilder status(BookingStatus status) { this.status = status; return this; }
        public BookingStatusUpdateRequestBuilder operatorId(Long operatorId) { this.operatorId = operatorId; return this; }
        public BookingStatusUpdateRequestBuilder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }

        public BookingStatusUpdateRequest build() {
            return new BookingStatusUpdateRequest(status, operatorId, rejectionReason);
        }
    }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
