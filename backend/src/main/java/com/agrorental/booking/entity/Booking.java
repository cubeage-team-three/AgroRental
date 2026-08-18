package com.agrorental.booking.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.agrorental.booking.entity.constant.BookingStatus;
import com.agrorental.common.entity.BaseEntity;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.operator.entity.Operator;
import com.agrorental.partner.entity.Partner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "bookings")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @NotNull(message = "Booking date is mandatory")
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @NotNull(message = "Start date is mandatory")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is mandatory")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Total acreage is mandatory")
    @Column(name = "total_acreage", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAcreage;

    @NotNull(message = "Total cost is mandatory")
    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @NotNull(message = "Farmer is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    @ToString.Exclude
    private Farmer farmer;

    @NotNull(message = "Equipment is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    @ToString.Exclude
    private Equipment equipment;

    @NotNull(message = "Partner is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    @ToString.Exclude
    private Partner partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    @ToString.Exclude
    private Operator operator;
}
