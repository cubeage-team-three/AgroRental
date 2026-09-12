package com.agrorental.farmer.service;

import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.dto.FarmCreateRequest;
import com.agrorental.farmer.dto.FarmResponse;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.repository.FarmRepository;
import org.springframework.security.access.AccessDeniedException;
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
     * Creates a new farm registration associated with the given farmer ID.
     */
    public FarmResponse createFarm(Long farmerId, FarmCreateRequest request) {
        if (farmerId == null) {
            throw new AccessDeniedException("Farmer ID is required to create a farm.");
        }

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
     * Overloaded create method utilizing farmer ID from request payload.
     */
    public FarmResponse createFarm(FarmCreateRequest request) {
        if (request == null || request.getFarmerId() == null) {
            throw new AccessDeniedException("Farmer ID is required to create a farm.");
        }
        return createFarm(request.getFarmerId(), request);
    }

    /**
     * Retrieves all farms belonging to a specific farmer.
     */
    @Transactional(readOnly = true)
    public List<FarmResponse> getFarmsByFarmerId(Long farmerId) {
        if (farmerId == null) {
            throw new AccessDeniedException("Farmer ID is required.");
        }
        return farmRepository.findByFarmerId(farmerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves details for a specific farm by ID and verifies owner identity.
     */
    @Transactional(readOnly = true)
    public FarmResponse getFarmByIdAndFarmerId(Long id, Long farmerId) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + id));

        if (farmerId != null && !farm.getFarmerId().equals(farmerId)) {
            throw new AccessDeniedException("Access is denied. You are not authorized to view this farm.");
        }

        return mapToResponse(farm);
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
     * Updates details of an existing farm after verifying ownership.
     */
    public FarmResponse updateFarm(Long id, Long farmerId, FarmCreateRequest request) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + id));

        if (farmerId != null && !farm.getFarmerId().equals(farmerId)) {
            throw new AccessDeniedException("Access is denied. You do not have permission to modify another farmer's farm.");
        }

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
     * Overloaded update method.
     */
    public FarmResponse updateFarm(Long id, FarmCreateRequest request) {
        Long farmerId = request != null ? request.getFarmerId() : null;
        return updateFarm(id, farmerId, request);
    }

    /**
     * Deletes a farm record by ID after verifying ownership.
     */
    public void deleteFarm(Long id, Long farmerId) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + id));

        if (farmerId != null && !farm.getFarmerId().equals(farmerId)) {
            throw new AccessDeniedException("Access is denied. You do not have permission to delete another farmer's farm.");
        }

        farmRepository.delete(farm);
    }

    /**
     * Overloaded delete method.
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

