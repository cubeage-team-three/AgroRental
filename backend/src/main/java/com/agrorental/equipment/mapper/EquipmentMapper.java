package com.agrorental.equipment.mapper;

import com.agrorental.equipment.dto.*;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.entity.EquipmentImage;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.partner.entity.Partner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Spring-managed Mapper component responsible for converting between
 * Equipment DTOs and JPA Entities.
 */
@Component
public class EquipmentMapper {

    /**
     * Maps EquipmentCreateRequest and Partner into Equipment entity.
     */
    public Equipment toEntity(
            EquipmentCreateRequest request,
            Partner partner) {

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
                .isDisabled(false)
                .build();

        if (request.getImages() != null
                && !request.getImages().isEmpty()) {

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
     * Maps EquipmentImageRequest to EquipmentImage entity.
     */
    public EquipmentImage toEntity(
            EquipmentImageRequest request) {

        if (request == null) {
            return null;
        }

        return EquipmentImage.builder()
                .imageUrl(request.getImageUrl())
                .isPrimary(
                        request.getIsPrimary() != null
                                ? request.getIsPrimary()
                                : false
                )
                .displayOrder(
                        request.getDisplayOrder() != null
                                ? request.getDisplayOrder()
                                : 0
                )
                .build();
    }

    /**
     * Updates an existing Equipment entity.
     */
    public void updateEntity(
            EquipmentUpdateRequest request,
            Equipment equipment) {

        if (request == null || equipment == null) {
            return;
        }

        equipment.setName(request.getName());
        equipment.setCategory(request.getCategory());
        equipment.setBrand(request.getBrand());
        equipment.setModel(request.getModel());
        equipment.setManufacturingYear(
                request.getManufacturingYear()
        );
        equipment.setCapacity(request.getCapacity());
        equipment.setRentalPrice(request.getRentalPrice());
        equipment.setFuelType(request.getFuelType());
        equipment.setDescription(request.getDescription());
        equipment.setLocationAddress(
                request.getLocationAddress()
        );
        equipment.setLatitude(request.getLatitude());
        equipment.setLongitude(request.getLongitude());

        if (request.getAvailabilityStatus() != null) {
            equipment.setAvailabilityStatus(
                    request.getAvailabilityStatus()
            );
        }

        if (request.getMaintenanceNotes() != null) {
            equipment.setMaintenanceNotes(
                    request.getMaintenanceNotes()
            );
        }

        if (request.getIsDisabled() != null) {
            equipment.setIsDisabled(
                    request.getIsDisabled()
            );
        }
    }

    /**
     * Maps Equipment entity to complete EquipmentResponse.
     */
    public EquipmentResponse toResponse(
            Equipment equipment) {

        if (equipment == null) {
            return null;
        }

        Long partnerId = null;

        if (equipment.getPartner() != null) {
            partnerId = equipment.getPartner().getId();
        }

        List<EquipmentImageResponse> imageResponses =
                Collections.emptyList();

        if (equipment.getImages() != null
                && !equipment.getImages().isEmpty()) {

            imageResponses = equipment.getImages()
                    .stream()

                    // Fixed null-safety warning
                    .sorted((image1, image2) -> {

                        Integer order1 =
                                image1.getDisplayOrder();

                        Integer order2 =
                                image2.getDisplayOrder();

                        if (order1 == null && order2 == null) {
                            return 0;
                        }

                        if (order1 == null) {
                            return 1;
                        }

                        if (order2 == null) {
                            return -1;
                        }

                        return order1.compareTo(order2);
                    })

                    .map(this::toImageResponse)
                    .toList();
        }

        return EquipmentResponse.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .category(equipment.getCategory())
                .brand(equipment.getBrand())
                .model(equipment.getModel())
                .manufacturingYear(
                        equipment.getManufacturingYear()
                )
                .capacity(equipment.getCapacity())
                .rentalPrice(equipment.getRentalPrice())
                .fuelType(equipment.getFuelType())
                .description(equipment.getDescription())
                .partnerId(partnerId)
                .locationAddress(
                        equipment.getLocationAddress()
                )
                .latitude(equipment.getLatitude())
                .longitude(equipment.getLongitude())
                .availabilityStatus(
                        equipment.getAvailabilityStatus()
                )
                .maintenanceNotes(
                        equipment.getMaintenanceNotes()
                )
                .isDisabled(equipment.getIsDisabled())
                .images(imageResponses)
                .createdAt(equipment.getCreatedAt())
                .updatedAt(equipment.getUpdatedAt())
                .build();
    }

    /**
     * Maps EquipmentImage entity to EquipmentImageResponse.
     */
    public EquipmentImageResponse toImageResponse(
            EquipmentImage image) {

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
     * Maps Equipment entity to compact EquipmentSummaryResponse.
     */
    public EquipmentSummaryResponse toSummaryResponse(
            Equipment equipment) {

        if (equipment == null) {
            return null;
        }

        String primaryImageUrl = null;

        if (equipment.getImages() != null
                && !equipment.getImages().isEmpty()) {

          primaryImageUrl = equipment.getImages()
        .stream()
        .filter(image ->
                Boolean.TRUE.equals(image.getIsPrimary())
        )
        .map(image -> image.getImageUrl())
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
                .locationAddress(
                        equipment.getLocationAddress()
                )
                .latitude(equipment.getLatitude())
                .longitude(equipment.getLongitude())
                .availabilityStatus(
                        equipment.getAvailabilityStatus()
                )
                .isDisabled(equipment.getIsDisabled())
                .primaryImageUrl(primaryImageUrl)
                .build();
    }
}