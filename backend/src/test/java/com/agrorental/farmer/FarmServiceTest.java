package com.agrorental.farmer;

import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.dto.FarmCreateRequest;
import com.agrorental.farmer.dto.FarmResponse;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.farmer.service.FarmService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FarmService Business Logic Unit Tests")
class FarmServiceTest {

    @Mock
    private FarmRepository farmRepository;

    @InjectMocks
    private FarmService farmService;

    private Farm sampleFarm;
    private FarmCreateRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleFarm = Farm.builder()
                .id(1L)
                .farmerId(10L)
                .farmName("Green Valley Farm")
                .village("Khed")
                .taluka("Khed")
                .district("Pune")
                .state("Maharashtra")
                .latitude(18.8500)
                .longitude(73.9100)
                .farmArea(new BigDecimal("12.5"))
                .cropType("Wheat")
                .build();

        sampleRequest = FarmCreateRequest.builder()
                .farmerId(10L)
                .farmName("Green Valley Farm")
                .village("Khed")
                .taluka("Khed")
                .district("Pune")
                .state("Maharashtra")
                .latitude(18.8500)
                .longitude(73.9100)
                .farmArea(new BigDecimal("12.5"))
                .cropType("Wheat")
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new farm")
    void shouldCreateFarmSuccessfully() {
        when(farmRepository.save(any(Farm.class))).thenReturn(sampleFarm);

        FarmResponse response = farmService.createFarm(sampleRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Green Valley Farm", response.getFarmName());
        assertEquals(new BigDecimal("12.5"), response.getFarmArea());
        verify(farmRepository, times(1)).save(any(Farm.class));
    }

    @Test
    @DisplayName("Should retrieve farms by farmer ID")
    void shouldGetFarmsByFarmerId() {
        when(farmRepository.findByFarmerId(10L)).thenReturn(List.of(sampleFarm));

        List<FarmResponse> responses = farmService.getFarmsByFarmerId(10L);

        assertEquals(1, responses.size());
        assertEquals("Green Valley Farm", responses.get(0).getFarmName());
        verify(farmRepository, times(1)).findByFarmerId(10L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for invalid farm ID")
    void shouldThrowExceptionWhenFarmNotFound() {
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmService.getFarmById(99L));
        verify(farmRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Should delete farm successfully when ID exists")
    void shouldDeleteFarmSuccessfully() {
        when(farmRepository.existsById(1L)).thenReturn(true);
        doNothing().when(farmRepository).deleteById(1L);

        assertDoesNotThrow(() -> farmService.deleteFarm(1L));
        verify(farmRepository, times(1)).deleteById(1L);
    }
}
