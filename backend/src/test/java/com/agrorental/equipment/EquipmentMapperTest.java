package com.agrorental.equipment;

import com.agrorental.equipment.dto.*;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.entity.EquipmentImage;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import com.agrorental.equipment.mapper.EquipmentMapper;
import com.agrorental.partner.entity.Partner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EquipmentMapper Unit Tests")
class EquipmentMapperTest {

    private EquipmentMapper equipmentMapper;

    @BeforeEach
    void setUp() {
        equipmentMapper = new EquipmentMapper();
    }

    @Test
    @DisplayName("Should map EquipmentCreateRequest and Partner to Equipment entity")
    void shouldMapCreateRequestToEquipmentEntity() {
        Partner partner = new Partner();
        partner.setId(10L);

        EquipmentImageRequest imageRequest = EquipmentImageRequest.builder()
                .imageUrl("https://example.com/tractor.jpg")
                .isPrimary(true)
                .displayOrder(1)
                .build();

        EquipmentCreateRequest request = EquipmentCreateRequest.builder()
                .name("Mahindra 575 DI")
                .category(EquipmentCategory.TRACTOR)
                .brand("Mahindra")
                .model("575 DI")
                .manufacturingYear(2024)
                .capacity("50 HP")
                .rentalPrice(new BigDecimal("1500.00"))
                .fuelType(FuelType.DIESEL)
                .description("Tractor description")
                .partnerId(10L)
                .locationAddress("Pune")
                .latitude(18.5204)
                .longitude(73.8567)
                .images(List.of(imageRequest))
                .build();

        Equipment equipment = equipmentMapper.toEntity(request, partner);

        assertNotNull(equipment);
        assertEquals("Mahindra 575 DI", equipment.getName());
        assertEquals(EquipmentCategory.TRACTOR, equipment.getCategory());
        assertEquals(partner, equipment.getPartner());
        assertEquals(AvailabilityStatus.AVAILABLE, equipment.getAvailabilityStatus());
        assertFalse(equipment.getIsDisabled());
        assertEquals(1, equipment.getImages().size());
        assertEquals(equipment, equipment.getImages().get(0).getEquipment());
    }

    @Test
    @DisplayName("Should update existing Equipment entity from EquipmentUpdateRequest")
    void shouldUpdateEntityFromUpdateRequest() {
        Equipment equipment = Equipment.builder()
                .name("Old Name")
                .category(EquipmentCategory.TRACTOR)
                .brand("Old Brand")
                .model("Old Model")
                .manufacturingYear(2020)
                .capacity("40 HP")
                .rentalPrice(new BigDecimal("1000.00"))
                .fuelType(FuelType.DIESEL)
                .description("Old description")
                .locationAddress("Old location")
                .latitude(10.0)
                .longitude(20.0)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .isDisabled(false)
                .build();
        equipment.setId(5L);

        EquipmentUpdateRequest updateRequest = EquipmentUpdateRequest.builder()
                .name("New Name")
                .category(EquipmentCategory.HARVESTER)
                .brand("New Brand")
                .model("New Model")
                .manufacturingYear(2024)
                .capacity("60 HP")
                .rentalPrice(new BigDecimal("2500.00"))
                .fuelType(FuelType.DIESEL)
                .description("New description")
                .locationAddress("New location")
                .latitude(18.5)
                .longitude(73.8)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .maintenanceNotes("Oil change done")
                .isDisabled(true)
                .build();

        equipmentMapper.updateEntity(updateRequest, equipment);

        assertEquals(5L, equipment.getId()); // ID remains untouched
        assertEquals("New Name", equipment.getName());
        assertEquals(EquipmentCategory.HARVESTER, equipment.getCategory());
        assertEquals(new BigDecimal("2500.00"), equipment.getRentalPrice());
        assertEquals("Oil change done", equipment.getMaintenanceNotes());
        assertTrue(equipment.getIsDisabled());
    }

    @Test
    @DisplayName("Should map Equipment entity to EquipmentResponse DTO")
    void shouldMapEquipmentToResponseDto() {
        Partner partner = new Partner();
        partner.setId(25L);

        Equipment equipment = Equipment.builder()
                .name("Mahindra 575 DI")
                .category(EquipmentCategory.TRACTOR)
                .brand("Mahindra")
                .model("575 DI")
                .manufacturingYear(2024)
                .capacity("50 HP")
                .rentalPrice(new BigDecimal("1500.00"))
                .fuelType(FuelType.DIESEL)
                .description("Description")
                .partner(partner)
                .locationAddress("Pune")
                .latitude(18.5204)
                .longitude(73.8567)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .isDisabled(false)
                .build();
        equipment.setId(100L);
        equipment.setCreatedAt(LocalDateTime.now());
        equipment.setUpdatedAt(LocalDateTime.now());

        EquipmentResponse response = equipmentMapper.toResponse(equipment);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(25L, response.getPartnerId());
        assertEquals("Mahindra 575 DI", response.getName());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    @DisplayName("Should map primary image URL correctly in EquipmentSummaryResponse")
    void shouldMapPrimaryImageInSummaryResponse() {
        Equipment equipment = Equipment.builder()
                .name("Tractor")
                .category(EquipmentCategory.TRACTOR)
                .brand("Brand")
                .model("Model")
                .rentalPrice(new BigDecimal("1200.00"))
                .fuelType(FuelType.DIESEL)
                .locationAddress("Pune")
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .isDisabled(false)
                .build();
        equipment.setId(50L);

        EquipmentImage img1 = EquipmentImage.builder().imageUrl("https://example.com/sec.jpg").isPrimary(false).build();
        EquipmentImage img2 = EquipmentImage.builder().imageUrl("https://example.com/primary.jpg").isPrimary(true).build();
        equipment.addImage(img1);
        equipment.addImage(img2);

        EquipmentSummaryResponse summary = equipmentMapper.toSummaryResponse(equipment);

        assertNotNull(summary);
        assertEquals(50L, summary.getId());
        assertEquals("https://example.com/primary.jpg", summary.getPrimaryImageUrl());
    }
}
