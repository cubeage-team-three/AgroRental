package com.agrorental.farmer.service;

import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.dto.FarmCreateRequest;
import com.agrorental.farmer.dto.FarmResponse;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.repository.FarmRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class handling farm management business operations.
 */
@Service
@Transactional
public class FarmService {

    private final FarmRepository farmRepository;

    public FarmService(FarmRepository farmRepository) {
        this.farmRepository = farmRepository;
    }

    /**
     * Creates a new farm registration.
     */
    public FarmResponse createFarm(FarmCreateRequest request) {
        Long farmerId = request.getFarmerId() != null ? request.getFarmerId() : 1L;

        Farm farm = Farm.builder()
                .farmerId(farmerId)
                .farmName(request.getFarmName())
                .village(request.getVillage())
                .taluka(request.getTaluka())
                .district(request.getDistrict())
                .state(request.getState())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .farmArea(request.getFarmArea())
                .cropType(request.getCropType())
                .build();

        Farm savedFarm = farmRepository.save(farm);
        return mapToResponse(savedFarm);
    }

    /**
     * Retrieves all farms belonging to a specific farmer.
     */
    @Transactional(readOnly = true)
    public List<FarmResponse> getFarmsByFarmerId(Long farmerId) {
        Long targetFarmerId = farmerId != null ? farmerId : 1L;
        return farmRepository.findByFarmerId(targetFarmerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves details for a specific farm by ID.
     */
    @Transactional(readOnly = true)
    public FarmResponse getFarmById(Long id) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + id));
        return mapToResponse(farm);
    }

    /**
     * Updates details of an existing farm.
     */
    public FarmResponse updateFarm(Long id, FarmCreateRequest request) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + id));

        farm.setFarmName(request.getFarmName());
        farm.setVillage(request.getVillage());
        farm.setTaluka(request.getTaluka());
        farm.setDistrict(request.getDistrict());
        farm.setState(request.getState());
        farm.setLatitude(request.getLatitude());
        farm.setLongitude(request.getLongitude());
        farm.setFarmArea(request.getFarmArea());
        farm.setCropType(request.getCropType());

        Farm updatedFarm = farmRepository.save(farm);
        return mapToResponse(updatedFarm);
    }

    /**
     * Deletes a farm record by ID.
     */
    public void deleteFarm(Long id) {
        if (!farmRepository.existsById(id)) {
            throw new ResourceNotFoundException("Farm not found with ID: " + id);
        }
        farmRepository.deleteById(id);
    }

    private FarmResponse mapToResponse(Farm farm) {
        return FarmResponse.builder()
                .id(farm.getId())
                .farmerId(farm.getFarmerId())
                .farmName(farm.getFarmName())
                .village(farm.getVillage())
                .taluka(farm.getTaluka())
                .district(farm.getDistrict())
                .state(farm.getState())
                .latitude(farm.getLatitude())
                .longitude(farm.getLongitude())
                .farmArea(farm.getFarmArea())
                .cropType(farm.getCropType())
                .createdAt(farm.getCreatedAt())
                .updatedAt(farm.getUpdatedAt())
                .build();
    }
}
