package com.agrorental.booking.mapper;

import com.agrorental.booking.dto.BookingCreateRequest;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.entity.EquipmentImage;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.partner.entity.Partner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Spring-managed Mapper component for converting between Booking DTOs and JPA Entities.
 */
@Component
public class BookingMapper {

    /**
     * Maps a BookingCreateRequest DTO, Equipment entity, Partner entity, Farm entity, and calculated total cost to a Booking entity.
     */
    public Booking toEntity(BookingCreateRequest request, Equipment equipment, Partner partner, Farm farm, BigDecimal totalCost) {
        if (request == null) {
            return null;
        }

        return Booking.builder()
                .farmerId(request.getFarmerId())
                .farm(farm)
                .equipment(equipment)
                .partner(partner)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalCost(totalCost)
                .status(BookingStatus.CONFIRMED)
                .deliveryAddress(request.getDeliveryAddress())
                .notes(request.getNotes())
                .build();
    }

    public Booking toEntity(BookingCreateRequest request, Equipment equipment, Partner partner, BigDecimal totalCost) {
        return toEntity(request, equipment, partner, null, totalCost);
    }

    /**
     * Maps a Booking entity to a complete BookingResponse DTO.
     *
     * @param booking Booking domain entity
     * @return Populated BookingResponse DTO
     */
    public BookingResponse toResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        Equipment equipment = booking.getEquipment();
        String primaryImageUrl = null;
        String equipmentName = null;
        String equipmentCategory = null;

        if (equipment != null) {
            equipmentName = equipment.getName();
            if (equipment.getCategory() != null) {
                equipmentCategory = equipment.getCategory().name();
            }
            if (equipment.getImages() != null && !equipment.getImages().isEmpty()) {
                primaryImageUrl = equipment.getImages().stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .map(EquipmentImage::getImageUrl)
                        .findFirst()
                        .orElse(equipment.getImages().get(0).getImageUrl());
            }
        }

        Long partnerId = booking.getPartner() != null ? booking.getPartner().getId() : null;
        Long operatorId = booking.getOperator() != null ? booking.getOperator().getId() : null;

        Farm farm = booking.getFarm();
        Long farmId = farm != null ? farm.getId() : null;
        String farmName = farm != null ? farm.getFarmName() : null;
        String farmLocation = null;
        if (farm != null) {
            farmLocation = String.format("%s, %s, %s", farm.getVillage(), farm.getTaluka(), farm.getDistrict());
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .farmerId(booking.getFarmerId())
                .farmId(farmId)
                .farmName(farmName)
                .farmLocation(farmLocation)
                .equipmentId(equipment != null ? equipment.getId() : null)
                .equipmentName(equipmentName)
                .equipmentCategory(equipmentCategory)
                .primaryImageUrl(primaryImageUrl)
                .partnerId(partnerId)
                .operatorId(operatorId)
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .totalCost(booking.getTotalCost())
                .status(booking.getStatus())
                .deliveryAddress(booking.getDeliveryAddress())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
