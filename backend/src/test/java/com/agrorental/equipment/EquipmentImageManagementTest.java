package com.agrorental.equipment;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.dto.EquipmentImageRequest;
import com.agrorental.equipment.dto.EquipmentResponse;
import com.agrorental.equipment.dto.EquipmentSummaryResponse;
import com.agrorental.equipment.dto.EquipmentUpdateRequest;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.entity.EquipmentImage;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import com.agrorental.equipment.mapper.EquipmentMapper;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.equipment.service.EquipmentService;
import com.agrorental.partner.entity.Partner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Equipment Image Management Unit Tests")
class EquipmentImageManagementTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private EquipmentMapper equipmentMapper;

    @InjectMocks
    private EquipmentService equipmentService;

    private Partner partnerOwner;
    private Partner partnerOther;
    private Equipment equipment;
    private EquipmentImage image1;
    private EquipmentImage image2;

    @BeforeEach
    void setUp() {
        partnerOwner = new Partner();
        partnerOwner.setId(100L);

        partnerOther = new Partner();
        partnerOther.setId(200L);

        equipment = Equipment.builder()
                .name("Mahindra 575 DI")
                .category(EquipmentCategory.TRACTOR)
                .brand("Mahindra")
                .model("575 DI")
                .manufacturingYear(2024)
                .capacity("50 HP")
                .rentalPrice(new BigDecimal("1500.00"))
                .fuelType(FuelType.DIESEL)
                .description("Tractor description")
                .partner(partnerOwner)
                .locationAddress("Pune")
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .isDisabled(false)
                .build();
        equipment.setId(1L);

        image1 = EquipmentImage.builder()
                .imageUrl("https://example.com/img1.jpg")
                .isPrimary(true)
                .displayOrder(1)
                .build();
        image1.setId(10L);

        image2 = EquipmentImage.builder()
                .imageUrl("https://example.com/img2.jpg")
                .isPrimary(true) // Intentional duplicate primary
                .displayOrder(2)
                .build();
        image2.setId(20L);

        equipment.addImage(image1);
        equipment.addImage(image2);
    }

    @Test
    @DisplayName("Should enforce single primary image during equipment update")
    void shouldEnforceSinglePrimaryImageOnUpdate() {
        EquipmentUpdateRequest updateRequest = EquipmentUpdateRequest.builder()
                .name("Updated Mahindra")
                .images(List.of(
                        EquipmentImageRequest.builder().imageUrl("https://example.com/a.jpg").isPrimary(true).displayOrder(1).build(),
                        EquipmentImageRequest.builder().imageUrl("https://example.com/b.jpg").isPrimary(true).displayOrder(2).build()
                ))
                .build();

        EquipmentMapper realMapper = new EquipmentMapper();
        Equipment e = realMapper.toEntity(
                com.agrorental.equipment.dto.EquipmentCreateRequest.builder()
                        .name("Test")
                        .category(EquipmentCategory.TRACTOR)
                        .partnerId(100L)
                        .images(updateRequest.getImages())
                        .build(),
                partnerOwner
        );

        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));
        when(equipmentMapper.toEntity(any(EquipmentImageRequest.class)))
                .thenAnswer(inv -> realMapper.toEntity((EquipmentImageRequest) inv.getArgument(0)));
        when(equipmentRepository.save(equipment)).thenReturn(equipment);

        equipmentService.updateEquipment(1L, 100L, updateRequest);

        long primaryCount = equipment.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .count();

        assertEquals(1, primaryCount);
        assertTrue(equipment.getImages().get(0).getIsPrimary());
        assertFalse(equipment.getImages().get(1).getIsPrimary());
    }

    @Test
    @DisplayName("Should delete specific image belonging to Equipment owned by Partner")
    void shouldDeleteSpecificEquipmentImage() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));
        when(equipmentRepository.save(equipment)).thenReturn(equipment);

        equipmentService.deleteEquipmentImage(1L, 10L, 100L);

        assertEquals(1, equipment.getImages().size());
        assertFalse(equipment.getImages().contains(image1));
        assertTrue(equipment.getImages().contains(image2));
    }

    @Test
    @DisplayName("Should reject Partner attempting to delete another Partner's equipment image")
    void shouldRejectCrossPartnerImageDeletion() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));

        assertThrows(BadRequestException.class, () ->
                equipmentService.deleteEquipmentImage(1L, 10L, 200L)
        );

        assertEquals(2, equipment.getImages().size());
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when image ID does not belong to equipment")
    void shouldThrowNotFoundWhenImageDoesNotExistOnEquipment() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));

        assertThrows(ResourceNotFoundException.class, () ->
                equipmentService.deleteEquipmentImage(1L, 999L, 100L)
        );
    }

    @Test
    @DisplayName("Should map primaryImageUrl to null when no primary image is marked")
    void shouldMapNullPrimaryImageUrlWhenNoPrimaryImageMarked() {
        image1.setIsPrimary(false);
        image2.setIsPrimary(false);

        EquipmentMapper realMapper = new EquipmentMapper();
        EquipmentSummaryResponse summary = realMapper.toSummaryResponse(equipment);

        assertNotNull(summary);
        assertNull(summary.getPrimaryImageUrl());
    }

    @Test
    @DisplayName("Should sort images by displayOrder ascending in response DTO")
    void shouldSortImagesByDisplayOrderAscending() {
        EquipmentImage imgLate = EquipmentImage.builder().imageUrl("https://example.com/late.jpg").displayOrder(10).build();
        EquipmentImage imgEarly = EquipmentImage.builder().imageUrl("https://example.com/early.jpg").displayOrder(1).build();

        Equipment eq = Equipment.builder().build();
        eq.addImage(imgLate);
        eq.addImage(imgEarly);

        EquipmentMapper realMapper = new EquipmentMapper();
        EquipmentResponse response = realMapper.toResponse(eq);

        assertEquals(2, response.getImages().size());
        assertEquals("https://example.com/early.jpg", response.getImages().get(0).getImageUrl());
        assertEquals("https://example.com/late.jpg", response.getImages().get(1).getImageUrl());
    }
}
