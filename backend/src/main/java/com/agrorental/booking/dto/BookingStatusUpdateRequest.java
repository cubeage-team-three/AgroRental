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

    public BookingStatusUpdateRequest() {}

    public BookingStatusUpdateRequest(BookingStatus status, Long operatorId) {
        this.status = status;
        this.operatorId = operatorId;
    }

    public static BookingStatusUpdateRequestBuilder builder() {
        return new BookingStatusUpdateRequestBuilder();
    }

    public static class BookingStatusUpdateRequestBuilder {
        private BookingStatus status;
        private Long operatorId;

        BookingStatusUpdateRequestBuilder() {}

        public BookingStatusUpdateRequestBuilder status(BookingStatus status) { this.status = status; return this; }
        public BookingStatusUpdateRequestBuilder operatorId(Long operatorId) { this.operatorId = operatorId; return this; }

        public BookingStatusUpdateRequest build() {
            return new BookingStatusUpdateRequest(status, operatorId);
        }
    }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
}
