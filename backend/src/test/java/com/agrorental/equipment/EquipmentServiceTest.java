package com.agrorental.equipment;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.dto.*;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import com.agrorental.equipment.mapper.EquipmentMapper;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.equipment.service.EquipmentService;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EquipmentService Business Logic Unit Tests")
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private EquipmentMapper equipmentMapper;

    @Mock
    private PartnerRepository partnerRepository;

    @InjectMocks
    private EquipmentService equipmentService;

    private Partner partnerA;
    private Partner partnerB;
    private Equipment equipmentA;

    @BeforeEach
    void setUp() {
        partnerA = new Partner();
        partnerA.setId(1L);

        partnerB = new Partner();
        partnerB.setId(2L);

        equipmentA = Equipment.builder()
                .name("Mahindra 575 DI")
                .category(EquipmentCategory.TRACTOR)
                .brand("Mahindra")
                .model("575 DI")
                .manufacturingYear(2024)
                .capacity("50 HP")
                .rentalPrice(new BigDecimal("1500.00"))
                .fuelType(FuelType.DIESEL)
                .description("Tractor description")
                .partner(partnerA)
                .locationAddress("Pune")
                .latitude(18.5204)
                .longitude(73.8567)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .isDisabled(false)
                .build();
        equipmentA.setId(10L);
    }

    @Test
    @DisplayName("Should create equipment successfully when Partner exists")
    void shouldCreateEquipmentWhenPartnerExists() {
        EquipmentCreateRequest request = EquipmentCreateRequest.builder()
                .partnerId(1L)
                .name("Mahindra 575 DI")
                .build();

        EquipmentResponse expectedResponse = EquipmentResponse.builder()
                .id(10L)
                .name("Mahindra 575 DI")
                .build();

        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partnerA));
        when(equipmentMapper.toEntity(request, partnerA)).thenReturn(equipmentA);
        when(equipmentRepository.save(equipmentA)).thenReturn(equipmentA);
        when(equipmentMapper.toResponse(equipmentA)).thenReturn(expectedResponse);

        EquipmentResponse actualResponse = equipmentService.createEquipment(request);

        assertNotNull(actualResponse);
        assertEquals(10L, actualResponse.getId());
        assertEquals("Mahindra 575 DI", actualResponse.getName());

        verify(partnerRepository).findById(1L);
        verify(equipmentRepository).save(equipmentA);
        verify(equipmentMapper).toResponse(equipmentA);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating equipment for non-existent Partner")
    void shouldThrowExceptionWhenCreatingEquipmentForMissingPartner() {
        EquipmentCreateRequest request = EquipmentCreateRequest.builder()
                .partnerId(99L)
                .build();

        when(partnerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> equipmentService.createEquipment(request));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return equipment by ID when exists")
    void shouldReturnEquipmentById() {
        EquipmentResponse expectedResponse = EquipmentResponse.builder()
                .id(10L)
                .name("Mahindra 575 DI")
                .build();

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipmentA));
        when(equipmentMapper.toResponse(equipmentA)).thenReturn(expectedResponse);

        EquipmentResponse response = equipmentService.getEquipmentById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Mahindra 575 DI", response.getName());

        verify(equipmentRepository).findById(10L);
        verify(equipmentMapper).toResponse(equipmentA);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when equipment ID does not exist")
    void shouldThrowExceptionWhenEquipmentNotFound() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> equipmentService.getEquipmentById(99L));
        verify(equipmentMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should allow Partner A to update Equipment A")
    void shouldAllowPartnerAToUpdateOwnEquipment() {
        EquipmentUpdateRequest updateRequest = EquipmentUpdateRequest.builder()
                .name("Updated Mahindra")
                .build();

        EquipmentResponse updatedResponse = EquipmentResponse.builder()
                .id(10L)
                .name("Updated Mahindra")
                .build();

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipmentA));
        when(equipmentRepository.save(equipmentA)).thenReturn(equipmentA);
        when(equipmentMapper.toResponse(equipmentA)).thenReturn(updatedResponse);

        EquipmentResponse response = equipmentService.updateEquipment(10L, 1L, updateRequest);

        assertNotNull(response);
        assertEquals("Updated Mahindra", response.getName());
        verify(equipmentMapper).updateEntity(updateRequest, equipmentA);
        verify(equipmentRepository).save(equipmentA);
    }

    @Test
    @DisplayName("Should reject Partner B from updating Partner A's equipment with BadRequestException")
    void shouldPreventPartnerBFromUpdatingPartnerAEquipment() {
        EquipmentUpdateRequest updateRequest = EquipmentUpdateRequest.builder().build();

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipmentA));

        assertThrows(BadRequestException.class, () -> equipmentService.updateEquipment(10L, 2L, updateRequest));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject manual update of availabilityStatus to BOOKED via EquipmentUpdateRequest")
    void shouldRejectManualTransitionToBookedStatus() {
        EquipmentUpdateRequest updateRequest = EquipmentUpdateRequest.builder()
                .availabilityStatus(AvailabilityStatus.BOOKED)
                .build();

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipmentA));

        assertThrows(BadRequestException.class, () -> equipmentService.updateEquipment(10L, 1L, updateRequest));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject Partner B from deleting Partner A's equipment with BadRequestException")
    void shouldPreventPartnerBFromDeletingPartnerAEquipment() {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipmentA));

        assertThrows(BadRequestException.class, () -> equipmentService.deleteEquipment(10L, 2L));
        verify(equipmentRepository, never()).delete(any(Equipment.class));
    }

    @Test
    @DisplayName("Should set isDisabled to true on disableEquipment")
    void shouldDisableEquipment() {
        EquipmentResponse response = EquipmentResponse.builder().id(10L).isDisabled(true).build();

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipmentA));
        when(equipmentRepository.save(equipmentA)).thenReturn(equipmentA);
        when(equipmentMapper.toResponse(equipmentA)).thenReturn(response);

        EquipmentResponse result = equipmentService.disableEquipment(10L, 1L);

        assertTrue(equipmentA.getIsDisabled());
        assertTrue(result.getIsDisabled());
        verify(equipmentRepository).save(equipmentA);
    }

    @Test
    @DisplayName("Should set isDisabled to false on enableEquipment")
    void shouldEnableEquipment() {
        equipmentA.setIsDisabled(true);
        EquipmentResponse response = EquipmentResponse.builder().id(10L).isDisabled(false).build();

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipmentA));
        when(equipmentRepository.save(equipmentA)).thenReturn(equipmentA);
        when(equipmentMapper.toResponse(equipmentA)).thenReturn(response);

        EquipmentResponse result = equipmentService.enableEquipment(10L, 1L);

        assertFalse(equipmentA.getIsDisabled());
        assertFalse(result.getIsDisabled());
        verify(equipmentRepository).save(equipmentA);
    }

    @Test
    @DisplayName("Should execute search query using EquipmentSpecification")
    void shouldExecuteSearchUsingSpecification() {
        EquipmentSearchRequest request = EquipmentSearchRequest.builder()
                .category(EquipmentCategory.TRACTOR)
                .build();

        EquipmentSummaryResponse summary = EquipmentSummaryResponse.builder()
                .id(10L)
                .name("Mahindra 575 DI")
                .build();

        when(equipmentRepository.findAll(any(Specification.class))).thenReturn(List.of(equipmentA));
        when(equipmentMapper.toSummaryResponse(equipmentA)).thenReturn(summary);

        List<EquipmentSummaryResponse> results = equipmentService.searchEquipment(request);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).getId());

        verify(equipmentRepository).findAll(any(Specification.class));
        verify(equipmentMapper).toSummaryResponse(equipmentA);
    }
}
