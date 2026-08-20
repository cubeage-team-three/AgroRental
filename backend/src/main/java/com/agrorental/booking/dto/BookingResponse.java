package com.agrorental.booking.dto;

import com.agrorental.booking.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Detailed Data Transfer Object returned in API responses representing a booking reservation.
 */
public class BookingResponse {

    private Long id;
    private Long farmerId;
    private String farmerName;
    private String farmerMobile;
    private String farmerEmail;
    private Long farmId;
    private String farmName;
    private String farmLocation;
    private Long equipmentId;
    private String equipmentName;
    private String equipmentCategory;
    private String primaryImageUrl;
    private Long partnerId;
    private Long operatorId;
    private String operatorName;
    private String operatorMobile;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalCost;
    private BookingStatus status;
    private String deliveryAddress;
    private String notes;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookingResponse() {}

    public BookingResponse(Long id, Long farmerId, String farmerName, String farmerMobile, String farmerEmail, Long farmId, String farmName, String farmLocation, Long equipmentId, String equipmentName, String equipmentCategory, String primaryImageUrl, Long partnerId, Long operatorId, String operatorName, String operatorMobile, LocalDate startDate, LocalDate endDate, BigDecimal totalCost, BookingStatus status, String deliveryAddress, String notes, String rejectionReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.farmerId = farmerId;
        this.farmerName = farmerName;
        this.farmerMobile = farmerMobile;
        this.farmerEmail = farmerEmail;
        this.farmId = farmId;
        this.farmName = farmName;
        this.farmLocation = farmLocation;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.equipmentCategory = equipmentCategory;
        this.primaryImageUrl = primaryImageUrl;
        this.partnerId = partnerId;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.operatorMobile = operatorMobile;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalCost = totalCost;
        this.status = status;
        this.deliveryAddress = deliveryAddress;
        this.notes = notes;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public BookingResponse(Long id, Long farmerId, Long farmId, String farmName, String farmLocation, Long equipmentId, String equipmentName, String equipmentCategory, String primaryImageUrl, Long partnerId, Long operatorId, LocalDate startDate, LocalDate endDate, BigDecimal totalCost, BookingStatus status, String deliveryAddress, String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, farmerId, null, null, null, farmId, farmName, farmLocation, equipmentId, equipmentName, equipmentCategory, primaryImageUrl, partnerId, operatorId, null, null, startDate, endDate, totalCost, status, deliveryAddress, notes, null, createdAt, updatedAt);
    }

    public BookingResponse(Long id, Long farmerId, Long equipmentId, String equipmentName, String equipmentCategory, String primaryImageUrl, Long partnerId, Long operatorId, LocalDate startDate, LocalDate endDate, BigDecimal totalCost, BookingStatus status, String deliveryAddress, String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, farmerId, null, null, null, null, null, null, equipmentId, equipmentName, equipmentCategory, primaryImageUrl, partnerId, operatorId, null, null, startDate, endDate, totalCost, status, deliveryAddress, notes, null, createdAt, updatedAt);
    }

    public static BookingResponseBuilder builder() {
        return new BookingResponseBuilder();
    }

    public static class BookingResponseBuilder {
        private Long id;
        private Long farmerId;
        private String farmerName;
        private String farmerMobile;
        private String farmerEmail;
        private Long farmId;
        private String farmName;
        private String farmLocation;
        private Long equipmentId;
        private String equipmentName;
        private String equipmentCategory;
        private String primaryImageUrl;
        private Long partnerId;
        private Long operatorId;
        private String operatorName;
        private String operatorMobile;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal totalCost;
        private BookingStatus status;
        private String deliveryAddress;
        private String notes;
        private String rejectionReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        BookingResponseBuilder() {}

        public BookingResponseBuilder id(Long id) { this.id = id; return this; }
        public BookingResponseBuilder farmerId(Long farmerId) { this.farmerId = farmerId; return this; }
        public BookingResponseBuilder farmerName(String farmerName) { this.farmerName = farmerName; return this; }
        public BookingResponseBuilder farmerMobile(String farmerMobile) { this.farmerMobile = farmerMobile; return this; }
        public BookingResponseBuilder farmerEmail(String farmerEmail) { this.farmerEmail = farmerEmail; return this; }
        public BookingResponseBuilder farmId(Long farmId) { this.farmId = farmId; return this; }
        public BookingResponseBuilder farmName(String farmName) { this.farmName = farmName; return this; }
        public BookingResponseBuilder farmLocation(String farmLocation) { this.farmLocation = farmLocation; return this; }
        public BookingResponseBuilder equipmentId(Long equipmentId) { this.equipmentId = equipmentId; return this; }
        public BookingResponseBuilder equipmentName(String equipmentName) { this.equipmentName = equipmentName; return this; }
        public BookingResponseBuilder equipmentCategory(String equipmentCategory) { this.equipmentCategory = equipmentCategory; return this; }
        public BookingResponseBuilder primaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; return this; }
        public BookingResponseBuilder partnerId(Long partnerId) { this.partnerId = partnerId; return this; }
        public BookingResponseBuilder operatorId(Long operatorId) { this.operatorId = operatorId; return this; }
        public BookingResponseBuilder operatorName(String operatorName) { this.operatorName = operatorName; return this; }
        public BookingResponseBuilder operatorMobile(String operatorMobile) { this.operatorMobile = operatorMobile; return this; }
        public BookingResponseBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public BookingResponseBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public BookingResponseBuilder totalCost(BigDecimal totalCost) { this.totalCost = totalCost; return this; }
        public BookingResponseBuilder status(BookingStatus status) { this.status = status; return this; }
        public BookingResponseBuilder deliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; return this; }
        public BookingResponseBuilder notes(String notes) { this.notes = notes; return this; }
        public BookingResponseBuilder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public BookingResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public BookingResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public BookingResponse build() {
            return new BookingResponse(id, farmerId, farmerName, farmerMobile, farmerEmail, farmId, farmName, farmLocation, equipmentId, equipmentName, equipmentCategory, primaryImageUrl, partnerId, operatorId, operatorName, operatorMobile, startDate, endDate, totalCost, status, deliveryAddress, notes, rejectionReason, createdAt, updatedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFarmerId() { return farmerId; }
    public void setFarmerId(Long farmerId) { this.farmerId = farmerId; }

    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }

    public String getFarmerMobile() { return farmerMobile; }
    public void setFarmerMobile(String farmerMobile) { this.farmerMobile = farmerMobile; }

    public String getFarmerEmail() { return farmerEmail; }
    public void setFarmerEmail(String farmerEmail) { this.farmerEmail = farmerEmail; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }

    public String getFarmLocation() { return farmLocation; }
    public void setFarmLocation(String farmLocation) { this.farmLocation = farmLocation; }

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getEquipmentCategory() { return equipmentCategory; }
    public void setEquipmentCategory(String equipmentCategory) { this.equipmentCategory = equipmentCategory; }

    public String getPrimaryImageUrl() { return primaryImageUrl; }
    public void setPrimaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; }

    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public String getOperatorMobile() { return operatorMobile; }
    public void setOperatorMobile(String operatorMobile) { this.operatorMobile = operatorMobile; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
