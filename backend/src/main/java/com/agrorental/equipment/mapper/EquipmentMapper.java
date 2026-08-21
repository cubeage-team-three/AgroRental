package com.agrorental.equipment.mapper;

import com.agrorental.equipment.dto.*;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.entity.EquipmentImage;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.partner.entity.Partner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Spring-managed Mapper component responsible for converting between Equipment DTOs and JPA Entities.
 * Contains pure object transformation logic with zero database, service, or business dependencies.
 */
@Component
public class EquipmentMapper {

    /**
     * Maps an EquipmentCreateRequest DTO and resolved Partner domain entity into a new Equipment entity.
     *
     * @param request Equipment creation request payload
     * @param partner Resolved Partner entity owning the equipment
     * @return Initialized Equipment entity
     */
    public Equipment toEntity(EquipmentCreateRequest request, Partner partner) {
        if (request == null) {
            return null;
        }

        Equipment equipment = Equipment.builder()
                .name(request.getName())
                .category(request.getCategory())
                .brand(request.getBrand())
                .model(request.getModel())
                .manufacturingYear(request.getManufacturingYear())
                .capacity(request.getCapacity())
                .rentalPrice(request.getRentalPrice())
                .fuelType(request.getFuelType())
                .description(request.getDescription())
                .partner(partner)
                .locationAddress(request.getLocationAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .availableFromDate(request.getAvailableFromDate())
                .availableToDate(request.getAvailableToDate())
                .isDisabled(false)
                .build();

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (EquipmentImageRequest imageRequest : request.getImages()) {
                EquipmentImage image = toEntity(imageRequest);
                if (image != null) {
                    equipment.addImage(image);
                }
            }
        }

        return equipment;
    }

    /**
     * Maps an EquipmentImageRequest DTO to an unlinked EquipmentImage entity.
     *
     * @param request Image request DTO
     * @return Initialized EquipmentImage entity
     */
    public EquipmentImage toEntity(EquipmentImageRequest request) {
        if (request == null) {
            return null;
        }

        return EquipmentImage.builder()
                .imageUrl(request.getImageUrl())
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();
    }

    /**
     * Updates an existing Equipment entity with mutable fields from EquipmentUpdateRequest.
     *
     * @param request Equipment update request DTO
     * @param equipment Existing Equipment entity to be modified
     */
    public void updateEntity(EquipmentUpdateRequest request, Equipment equipment) {
        if (request == null || equipment == null) {
            return;
        }

        equipment.setName(request.getName());
        equipment.setCategory(request.getCategory());
        equipment.setBrand(request.getBrand());
        equipment.setModel(request.getModel());
        equipment.setManufacturingYear(request.getManufacturingYear());
        equipment.setCapacity(request.getCapacity());
        equipment.setRentalPrice(request.getRentalPrice());
        equipment.setFuelType(request.getFuelType());
        equipment.setDescription(request.getDescription());
        equipment.setLocationAddress(request.getLocationAddress());
        equipment.setLatitude(request.getLatitude());
        equipment.setLongitude(request.getLongitude());

        if (request.getAvailabilityStatus() != null) {
            equipment.setAvailabilityStatus(request.getAvailabilityStatus());
        }
        if (request.getAvailableFromDate() != null) {
            equipment.setAvailableFromDate(request.getAvailableFromDate());
        }
        if (request.getAvailableToDate() != null) {
            equipment.setAvailableToDate(request.getAvailableToDate());
        }
        if (request.getMaintenanceNotes() != null) {
            equipment.setMaintenanceNotes(request.getMaintenanceNotes());
        }
        if (request.getIsDisabled() != null) {
            equipment.setIsDisabled(request.getIsDisabled());
        }
    }

    /**
     * Maps an Equipment entity into a complete EquipmentResponse DTO for public API responses.
     *
     * @param equipment Equipment domain entity
     * @return Populated EquipmentResponse DTO
     */
    public EquipmentResponse toResponse(Equipment equipment) {
        if (equipment == null) {
            return null;
        }

        Long partnerId = equipment.getPartner() != null ? equipment.getPartner().getId() : null;

        List<EquipmentImageResponse> imageResponses = Collections.emptyList();
        if (equipment.getImages() != null && !equipment.getImages().isEmpty()) {
            imageResponses = equipment.getImages().stream()
                    .sorted(Comparator.comparing(
                            EquipmentImage::getDisplayOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(this::toImageResponse)
                    .toList();
        }

        return EquipmentResponse.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .category(equipment.getCategory())
                .brand(equipment.getBrand())
                .model(equipment.getModel())
                .manufacturingYear(equipment.getManufacturingYear())
                .capacity(equipment.getCapacity())
                .rentalPrice(equipment.getRentalPrice())
                .fuelType(equipment.getFuelType())
                .description(equipment.getDescription())
                .partnerId(partnerId)
                .locationAddress(equipment.getLocationAddress())
                .latitude(equipment.getLatitude())
                .longitude(equipment.getLongitude())
                .availabilityStatus(equipment.getAvailabilityStatus())
                .availableFromDate(equipment.getAvailableFromDate())
                .availableToDate(equipment.getAvailableToDate())
                .maintenanceNotes(equipment.getMaintenanceNotes())
                .isDisabled(equipment.getIsDisabled())
                .images(imageResponses)
                .createdAt(equipment.getCreatedAt())
                .updatedAt(equipment.getUpdatedAt())
                .build();
    }

    /**
     * Maps an EquipmentImage entity into an EquipmentImageResponse DTO.
     *
     * @param image EquipmentImage domain entity
     * @return Populated EquipmentImageResponse DTO
     */
    public EquipmentImageResponse toImageResponse(EquipmentImage image) {
        if (image == null) {
            return null;
        }

        return EquipmentImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isPrimary(image.getIsPrimary())
                .displayOrder(image.getDisplayOrder())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }

    /**
     * Maps an Equipment entity into a compact EquipmentSummaryResponse DTO for card/search listings.
     *
     * @param equipment Equipment domain entity
     * @return Populated EquipmentSummaryResponse DTO
     */
    public EquipmentSummaryResponse toSummaryResponse(Equipment equipment) {
        if (equipment == null) {
            return null;
        }

        String primaryImageUrl = null;
        if (equipment.getImages() != null && !equipment.getImages().isEmpty()) {
            primaryImageUrl = equipment.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(EquipmentImage::getImageUrl)
                    .findFirst()
                    .orElse(null);
        }

        return EquipmentSummaryResponse.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .category(equipment.getCategory())
                .brand(equipment.getBrand())
                .model(equipment.getModel())
                .rentalPrice(equipment.getRentalPrice())
                .fuelType(equipment.getFuelType())
                .locationAddress(equipment.getLocationAddress())
                .latitude(equipment.getLatitude())
                .longitude(equipment.getLongitude())
                .availabilityStatus(equipment.getAvailabilityStatus())
                .availableFromDate(equipment.getAvailableFromDate())
                .availableToDate(equipment.getAvailableToDate())
                .isDisabled(equipment.getIsDisabled())
                .primaryImageUrl(primaryImageUrl)
                .build();
    }
}
