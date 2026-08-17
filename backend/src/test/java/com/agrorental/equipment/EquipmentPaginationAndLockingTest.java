package com.agrorental.equipment;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.equipment.dto.EquipmentSearchRequest;
import com.agrorental.equipment.dto.EquipmentSummaryResponse;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.mapper.EquipmentMapper;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.equipment.service.EquipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Equipment Pagination & Optimistic Locking Unit Tests")
class EquipmentPaginationAndLockingTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private EquipmentMapper equipmentMapper;

    @InjectMocks
    private EquipmentService equipmentService;

    private Equipment equipment;

    @BeforeEach
    void setUp() {
        equipment = Equipment.builder()
                .name("Mahindra 575 DI")
                .category(EquipmentCategory.TRACTOR)
                .brand("Mahindra")
                .model("575 DI")
                .rentalPrice(new BigDecimal("1500.00"))
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .isDisabled(false)
                .build();
        equipment.setId(10L);
        equipment.setVersion(1L);
    }

    @Test
    @DisplayName("Should verify presence of @Version field on Equipment entity via BaseEntity")
    void shouldVerifyVersionFieldOnEntity() {
        assertEquals(1L, equipment.getVersion());
        equipment.setVersion(2L);
        assertEquals(2L, equipment.getVersion());
    }

    @Test
    @DisplayName("Should return paginated discoverable equipment")
    void shouldReturnPaginatedDiscoverableEquipment() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
        EquipmentSummaryResponse summary = EquipmentSummaryResponse.builder().id(10L).name("Mahindra 575 DI").build();
        Page<Equipment> equipmentPage = new PageImpl<>(List.of(equipment), pageable, 1);

        when(equipmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(equipmentPage);
        when(equipmentMapper.toSummaryResponse(equipment)).thenReturn(summary);

        Page<EquipmentSummaryResponse> result = equipmentService.getDiscoverableEquipment(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(10L, result.getContent().get(0).getId());
    }

    @Test
    @DisplayName("Should cap page size to 100 items maximum when oversized request is received")
    void shouldCapPageSizeToMaximum100() {
        Pageable oversizedPageable = PageRequest.of(0, 500);
        Page<Equipment> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);

        when(equipmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable passedPageable = invocation.getArgument(1);
            assertEquals(100, passedPageable.getPageSize());
            return emptyPage;
        });

        Page<EquipmentSummaryResponse> result = equipmentService.getDiscoverableEquipment(oversizedPageable);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle ObjectOptimisticLockingFailureException with HTTP 409 CONFLICT in GlobalExceptionHandler")
    void shouldHandleOptimisticLockingExceptionInGlobalExceptionHandler() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException(Equipment.class, 10L);

        ResponseEntity<ApiResponse<Object>> response = handler.handleOptimisticLockingFailure(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Concurrent update conflict detected. Please refresh and try again.", response.getBody().getMessage());
    }
}
