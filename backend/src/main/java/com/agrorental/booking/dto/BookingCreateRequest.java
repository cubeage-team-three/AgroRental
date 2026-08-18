package com.agrorental.booking.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Data Transfer Object payload for creating a new equipment rental booking request.
 */
public class BookingCreateRequest {

    @NotNull(message = "Equipment ID is mandatory")
    private Long equipmentId;

    @NotNull(message = "Farmer ID is mandatory")
    private Long farmerId;

    @NotNull(message = "Start date is mandatory")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is mandatory")
    @FutureOrPresent(message = "End date must be today or in the future")
    private LocalDate endDate;

    private String deliveryAddress;

    private String notes;

    public BookingCreateRequest() {}

    public BookingCreateRequest(Long equipmentId, Long farmerId, LocalDate startDate, LocalDate endDate, String deliveryAddress, String notes) {
        this.equipmentId = equipmentId;
        this.farmerId = farmerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.deliveryAddress = deliveryAddress;
        this.notes = notes;
    }

    public static BookingCreateRequestBuilder builder() {
        return new BookingCreateRequestBuilder();
    }

    public static class BookingCreateRequestBuilder {
        private Long equipmentId;
        private Long farmerId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String deliveryAddress;
        private String notes;

        BookingCreateRequestBuilder() {}

        public BookingCreateRequestBuilder equipmentId(Long equipmentId) { this.equipmentId = equipmentId; return this; }
        public BookingCreateRequestBuilder farmerId(Long farmerId) { this.farmerId = farmerId; return this; }
        public BookingCreateRequestBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public BookingCreateRequestBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public BookingCreateRequestBuilder deliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; return this; }
        public BookingCreateRequestBuilder notes(String notes) { this.notes = notes; return this; }

        public BookingCreateRequest build() {
            return new BookingCreateRequest(equipmentId, farmerId, startDate, endDate, deliveryAddress, notes);
        }
    }

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

    public Long getFarmerId() { return farmerId; }
    public void setFarmerId(Long farmerId) { this.farmerId = farmerId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
