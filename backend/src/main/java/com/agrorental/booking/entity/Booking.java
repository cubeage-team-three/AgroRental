package com.agrorental.booking.entity;

import com.agrorental.common.entity.BaseEntity;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.operator.entity.Operator;
import com.agrorental.partner.entity.Partner;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA Domain Entity representing a machinery rental reservation between a Farmer, Equipment, and Partner.
 */
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @NotNull(message = "Farmer ID is mandatory")
    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @NotNull(message = "Equipment is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @NotNull(message = "Partner is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private Operator operator;

    @NotNull(message = "Start date is mandatory")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is mandatory")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Total cost is mandatory")
    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @NotNull(message = "Booking status is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public Booking() {}

    public Booking(Long farmerId, Farm farm, Equipment equipment, Partner partner, Operator operator, LocalDate startDate, LocalDate endDate, BigDecimal totalCost, BookingStatus status, String deliveryAddress, String notes) {
        this.farmerId = farmerId;
        this.farm = farm;
        this.equipment = equipment;
        this.partner = partner;
        this.operator = operator;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalCost = totalCost;
        this.status = status;
        this.deliveryAddress = deliveryAddress;
        this.notes = notes;
    }

    public Booking(Long farmerId, Equipment equipment, Partner partner, Operator operator, LocalDate startDate, LocalDate endDate, BigDecimal totalCost, BookingStatus status, String deliveryAddress, String notes) {
        this(farmerId, null, equipment, partner, operator, startDate, endDate, totalCost, status, deliveryAddress, notes);
    }

    public static BookingBuilder builder() {
        return new BookingBuilder();
    }

    public static class BookingBuilder {
        private Long farmerId;
        private Farm farm;
        private Equipment equipment;
        private Partner partner;
        private Operator operator;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal totalCost;
        private BookingStatus status;
        private String deliveryAddress;
        private String notes;

        BookingBuilder() {}

        public BookingBuilder farmerId(Long farmerId) {
            this.farmerId = farmerId;
            return this;
        }

        public BookingBuilder farm(Farm farm) {
            this.farm = farm;
            return this;
        }

        public BookingBuilder equipment(Equipment equipment) {
            this.equipment = equipment;
            return this;
        }

        public BookingBuilder partner(Partner partner) {
            this.partner = partner;
            return this;
        }

        public BookingBuilder operator(Operator operator) {
            this.operator = operator;
            return this;
        }

        public BookingBuilder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public BookingBuilder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public BookingBuilder totalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
            return this;
        }

        public BookingBuilder status(BookingStatus status) {
            this.status = status;
            return this;
        }

        public BookingBuilder deliveryAddress(String deliveryAddress) {
            this.deliveryAddress = deliveryAddress;
            return this;
        }

        public BookingBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Booking build() {
            return new Booking(farmerId, farm, equipment, partner, operator, startDate, endDate, totalCost, status, deliveryAddress, notes);
        }
    }

    public Long getFarmerId() { return farmerId; }
    public void setFarmerId(Long farmerId) { this.farmerId = farmerId; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public Partner getPartner() { return partner; }
    public void setPartner(Partner partner) { this.partner = partner; }

    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }

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
}
