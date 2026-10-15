package com.agrorental.equipment.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.dto.*;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.entity.EquipmentImage;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.mapper.EquipmentMapper;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.equipment.specification.EquipmentSpecification;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Machine Management (Equipment) module.
 * Orchestrates business logic, partner ownership validation, entity mappings, and persistence operations.
 */
@Service
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentMapper equipmentMapper;
    private final PartnerRepository partnerRepository;
    /** Booking-derived availability (see EquipmentAvailabilityService); absent only in plain unit tests. */
    private EquipmentAvailabilityService equipmentAvailabilityService;

    /**
     * Statuses shown in farmer discovery. BOOKED only means "has reservations": the machine is still bookable for
     * dates no reservation covers (BIZ-13 decides per date range), so it must not disappear from discovery.
     */
    static final List<AvailabilityStatus> DISCOVERABLE_STATUSES = List.of(AvailabilityStatus.AVAILABLE, AvailabilityStatus.BOOKED);

    public EquipmentService(
            EquipmentRepository equipmentRepository,
            EquipmentMapper equipmentMapper,
            PartnerRepository partnerRepository) {

        this.equipmentRepository = equipmentRepository;
        this.equipmentMapper = equipmentMapper;
        this.partnerRepository = partnerRepository;
    }

    @Autowired(required = false)
    public void setEquipmentAvailabilityService(EquipmentAvailabilityService equipmentAvailabilityService) {
        this.equipmentAvailabilityService = equipmentAvailabilityService;
    }

    /**
     * Creates and persists a new Equipment listing owned by a validated Partner.
     *
     * @param request Equipment creation payload containing partnerId and equipment details
     * @return EquipmentResponse DTO representing the created equipment
     */
    @Transactional
    public EquipmentResponse createEquipment(EquipmentCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Equipment creation request cannot be null");
        }

        Partner partner = partnerRepository.findById(request.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found with ID: " + request.getPartnerId()));

        Equipment equipment = equipmentMapper.toEntity(request, partner);
        enforceSinglePrimaryImage(equipment);
        Equipment savedEquipment = equipmentRepository.save(equipment);

        return equipmentMapper.toResponse(savedEquipment);
    }

    /**
     * Retrieves a single Equipment listing by its unique identifier.
     *
     * @param id Unique equipment primary key
     * @return EquipmentResponse DTO representing the equipment details
     */
    public EquipmentResponse getEquipmentById(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + id));

        return equipmentMapper.toResponse(equipment);
    }

    /**
     * Retrieves all equipment listings owned by a specific Partner.
     *
     * @param partnerId Identifier of the owning partner
     * @return List of compact EquipmentSummaryResponse DTOs
     */
    public List<EquipmentSummaryResponse> getEquipmentByPartner(Long partnerId) {
        if (!partnerRepository.existsById(partnerId)) {
            throw new ResourceNotFoundException("Partner not found with ID: " + partnerId);
        }

        List<Equipment> equipmentList = equipmentRepository.findByPartnerId(partnerId);

        return equipmentList.stream()
                .map(equipmentMapper::toSummaryResponse)
                .toList();
    }

    /**
     * Retrieves all equipment listings for administrative catalog management (FR-39),
     * including both active and administratively disabled equipment.
     *
     * @return List of compact EquipmentSummaryResponse DTOs
     */
    public List<EquipmentSummaryResponse> getAllEquipment() {
        List<Equipment> equipmentList = equipmentRepository.findAll();

        return equipmentList.stream()
                .map(equipmentMapper::toSummaryResponse)
                .toList();
    }

    /**
     * Retrieves all equipment listings with database-side pagination for administrative catalog management (FR-39),
     * including both active and administratively disabled equipment.
     *
     * @param pageable Pagination request
     * @return Page of compact EquipmentSummaryResponse DTOs
     */
    public Page<EquipmentSummaryResponse> getAllEquipment(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        Page<Equipment> equipmentPage = equipmentRepository.findAll(safePageable);

        return equipmentPage.map(equipmentMapper::toSummaryResponse);
    }

    /**
     * Retrieves all equipment listings that are in service (AVAILABLE, or BOOKED for some dates) and not
     * administratively disabled. UNDER_MAINTENANCE and INACTIVE equipment is not discoverable.
     *
     * @return List of compact EquipmentSummaryResponse DTOs for discovery
     */
    public List<EquipmentSummaryResponse> getDiscoverableEquipment() {
        List<Equipment> equipmentList = equipmentRepository.findAll(discoverable());

        return equipmentList.stream()
                .map(equipmentMapper::toSummaryResponse)
                .toList();
    }

    /**
     * Retrieves equipment listings for discovery with database-side pagination and maximum page size capping (max 100).
     *
     * @param pageable Pagination request
     * @return Page of compact EquipmentSummaryResponse DTOs
     */
    public Page<EquipmentSummaryResponse> getDiscoverableEquipment(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        Page<Equipment> equipmentPage = equipmentRepository.findAll(discoverable(), safePageable);
        return equipmentPage.map(equipmentMapper::toSummaryResponse);
    }

    /** In service (AVAILABLE or BOOKED), not disabled, and owned by a partner who can still rent it out. */
    private static Specification<Equipment> discoverable() {
        Specification<Equipment> inService = (root, query, cb) -> cb.and(
                root.get("availabilityStatus").in(DISCOVERABLE_STATUSES),
                cb.isFalse(root.get("isDisabled")));
        return inService.and(EquipmentSpecification.ownerCanRent());
    }

    /**
     * Dynamically searches for equipment using multi-criteria JPA Specifications.
     *
     * @param request Search filter DTO containing optional query criteria
     * @return List of compact EquipmentSummaryResponse DTOs matching the criteria
     */
    public List<EquipmentSummaryResponse> searchEquipment(EquipmentSearchRequest request) {
        if (request != null && request.getMinPrice() != null && request.getMaxPrice() != null) {
            if (request.getMinPrice().compareTo(request.getMaxPrice()) > 0) {
                throw new BadRequestException("Minimum price cannot be greater than maximum price");
            }
        }
        validateSearchDates(request);

        Specification<Equipment> spec = EquipmentSpecification.buildSpecification(request);
        List<Equipment> equipmentList = equipmentRepository.findAll(spec);

        return equipmentList.stream()
                .map(equipmentMapper::toSummaryResponse)
                .toList();
    }

    /**
     * Dynamically searches for equipment using multi-criteria JPA Specifications with database-side pagination.
     *
     * @param request Search filter DTO containing optional query criteria
     * @param pageable Pagination request
     * @return Page of compact EquipmentSummaryResponse DTOs matching the criteria
     */
    public Page<EquipmentSummaryResponse> searchEquipment(EquipmentSearchRequest request, Pageable pageable) {
        if (request != null && request.getMinPrice() != null && request.getMaxPrice() != null) {
            if (request.getMinPrice().compareTo(request.getMaxPrice()) > 0) {
                throw new BadRequestException("Minimum price cannot be greater than maximum price");
            }
        }
        validateSearchDates(request);

        Pageable safePageable = sanitizePageable(pageable);
        Specification<Equipment> spec = EquipmentSpecification.buildSpecification(request);
        Page<Equipment> equipmentPage = equipmentRepository.findAll(spec, safePageable);

        return equipmentPage.map(equipmentMapper::toSummaryResponse);
    }

    /** An inverted date range would silently skip the availability filters; reject it like booking creation does. */
    private static void validateSearchDates(EquipmentSearchRequest request) {
        if (request != null && request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
    }

    /**
     * Sanitizes Pageable input to prevent negative page numbers and cap maximum page size at 100 items.
     */
    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        int pageNumber = Math.max(0, pageable.getPageNumber());
        int pageSize = pageable.getPageSize();
        if (pageSize <= 0) {
            pageSize = 20;
        } else if (pageSize > 100) {
            pageSize = 100;
        }
        return PageRequest.of(pageNumber, pageSize, pageable.getSort());
    }

    /**
     * Updates an existing Equipment listing.
     *
     * @param equipmentId Unique identifier of the equipment to update
     * @param request Payload containing updated equipment fields
     * @return EquipmentResponse DTO representing the updated equipment
     */
    @Transactional
    public EquipmentResponse updateEquipment(Long equipmentId, EquipmentUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Equipment update request cannot be null");
        }

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + equipmentId));

        AvailabilityStatus previousStatus = equipment.getAvailabilityStatus();
        AvailabilityStatus requestedStatus = request.getAvailabilityStatus();
        // BOOKED is derived from bookings: it cannot be chosen manually, but the edit form may echo the current BOOKED
        if (requestedStatus == AvailabilityStatus.BOOKED && previousStatus != AvailabilityStatus.BOOKED) {
            throw new BadRequestException("Equipment availability status cannot be manually set to BOOKED via direct update. Equipment status transitions to BOOKED automatically through the Booking module.");
        }

        equipmentMapper.updateEntity(request, equipment);

        // AVAILABLE (or an echoed BOOKED) means "in service": the flag follows the reservations that still exist
        if ((requestedStatus == AvailabilityStatus.AVAILABLE || requestedStatus == AvailabilityStatus.BOOKED)
                && equipmentAvailabilityService != null) {
            equipment.setAvailabilityStatus(EquipmentAvailabilityService.derive(AvailabilityStatus.AVAILABLE,
                    equipmentAvailabilityService.hasReservingBookings(equipment.getId())));
        }

        if (request.getImages() != null) {
            List<EquipmentImage> currentImages = new java.util.ArrayList<>(equipment.getImages());
            for (EquipmentImage oldImg : currentImages) {
                equipment.removeImage(oldImg);
            }
            for (EquipmentImageRequest imgReq : request.getImages()) {
                EquipmentImage image = equipmentMapper.toEntity(imgReq);
                if (image != null) {
                    equipment.addImage(image);
                }
            }
            enforceSinglePrimaryImage(equipment);
        }

        Equipment updatedEquipment = equipmentRepository.save(equipment);

        return equipmentMapper.toResponse(updatedEquipment);
    }

    /**
     * Updates an existing Equipment listing with explicit partner ownership validation.
     *
     * @param equipmentId Unique identifier of the equipment to update
     * @param requestingPartnerId Partner ID attempting the update operation
     * @param request Payload containing updated equipment fields
     * @return EquipmentResponse DTO representing the updated equipment
     */
    @Transactional
    public EquipmentResponse updateEquipment(Long equipmentId, Long requestingPartnerId, EquipmentUpdateRequest request) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + equipmentId));

        validateOwnership(equipment, requestingPartnerId);

        return updateEquipment(equipmentId, request);
    }

    /**
     * Disables an equipment listing for administrative or maintenance lockout.
     *
     * @param equipmentId Unique identifier of the equipment
     * @return Updated EquipmentResponse DTO
     */
    @Transactional
    public EquipmentResponse disableEquipment(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + equipmentId));

        equipment.setIsDisabled(true);
        Equipment savedEquipment = equipmentRepository.save(equipment);

        return equipmentMapper.toResponse(savedEquipment);
    }

    /**
     * Disables an equipment listing with partner ownership validation.
     *
     * @param equipmentId Unique identifier of the equipment
     * @param requestingPartnerId Partner ID requesting disablement
     * @return Updated EquipmentResponse DTO
     */
    @Transactional
    public EquipmentResponse disableEquipment(Long equipmentId, Long requestingPartnerId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + equipmentId));

        validateOwnership(equipment, requestingPartnerId);

        return disableEquipment(equipmentId);
    }

    /**
     * Enables a previously disabled equipment listing.
     *
     * @param equipmentId Unique identifier of the equipment
     * @return Updated EquipmentResponse DTO
     */
    @Transactional
    public EquipmentResponse enableEquipment(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + equipmentId));

        equipment.setIsDisabled(false);
        Equipment savedEquipment = equipmentRepository.save(equipment);

        return equipmentMapper.toResponse(savedEquipment);
    }

    /**
     * Enables a previously disabled equipment listing with partner ownership validation.
     *
     * @param equipmentId Unique identifier of the equipment
     * @param requestingPartnerId Partner ID requesting enablement
     * @return Updated EquipmentResponse DTO
     */
    @Transactional
    public EquipmentResponse enableEquipment(Long equipmentId, Long requestingPartnerId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + equipmentId));

        validateOwnership(equipment, requestingPartnerId);

        return enableEquipment(equipmentId);
    }

    /**
     * Removes an Equipment listing and its associated image assets from the system.
     *
     * @param equipmentId Unique identifier of the equipment to delete
     */
    @Transactional
    public void deleteEquipment(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + equipmentId));

        validateDeletable(equipment);

        equipmentRepository.delete(equipment);
    }

    /**
     * Removes an Equipment listing with explicit partner ownership validation.
     *
     * @param equipmentId Unique identifier of the equipment to delete
     * @param requestingPartnerId Partner ID requesting deletion
     */
    @Transactional
    public void deleteEquipment(Long equipmentId, Long requestingPartnerId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + equipmentId));

        validateOwnership(equipment, requestingPartnerId);

        validateDeletable(equipment);

        equipmentRepository.delete(equipment);
    }

    /**
     * Equipment that holds reservations, or that has any booking history (its bookings, and through them payments,
     * refunds and reviews, refer to it), cannot be deleted; disabling hides it from farmers instead. Refused with a
     * clear 400 rather than a foreign-key failure (409 "Database constraint conflict occurred").
     */
    private void validateDeletable(Equipment equipment) {
        if (equipment.getAvailabilityStatus() == AvailabilityStatus.BOOKED
                || (equipmentAvailabilityService != null && equipmentAvailabilityService.hasReservingBookings(equipment.getId()))) {
            throw new BadRequestException("Cannot delete equipment while it is currently booked or has active bookings");
        }
        if (equipmentAvailabilityService != null && equipmentAvailabilityService.hasAnyBookings(equipment.getId())) {
            throw new BadRequestException("Equipment with booking history cannot be deleted. Disable it instead to hide it from farmers.");
        }
    }

    /**
     * Validates that the specified partner owns the target equipment listing.
     *
     * @param equipment Target equipment entity
     * @param requestingPartnerId Partner ID performing the operation
     */
    private void validateOwnership(Equipment equipment, Long requestingPartnerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null &&
                auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return;
        }
        if (equipment.getPartner() == null || !equipment.getPartner().getId().equals(requestingPartnerId)) {
            throw new BadRequestException("Partner ID " + requestingPartnerId + " is not authorized to modify equipment ID " + equipment.getId());
        }
    }

    /**
     * Deletes a specific EquipmentImage belonging to an Equipment aggregate, subject to partner ownership authorization.
     *
     * @param equipmentId Unique identifier of the target equipment
     * @param imageId Unique identifier of the target image to remove
     * @param requestingPartnerId Optional partner ID for ownership authorization check
     * @return Updated EquipmentResponse DTO
     */
    @Transactional
    public EquipmentResponse deleteEquipmentImage(Long equipmentId, Long imageId, Long requestingPartnerId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + equipmentId));

        if (requestingPartnerId != null) {
            validateOwnership(equipment, requestingPartnerId);
        }

        EquipmentImage targetImage = equipment.getImages().stream()
                .filter(img -> img.getId() != null && img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Image ID " + imageId + " not found on equipment ID " + equipmentId));

        equipment.removeImage(targetImage);
        enforceSinglePrimaryImage(equipment);

        Equipment savedEquipment = equipmentRepository.save(equipment);
        return equipmentMapper.toResponse(savedEquipment);
    }

    /**
     * Enforces the business rule that an Equipment aggregate has at most one primary image.
     * If multiple images are marked primary, the first primary image remains primary and subsequent images are cleared.
     *
     * @param equipment Target Equipment aggregate entity
     */
    private void enforceSinglePrimaryImage(Equipment equipment) {
        if (equipment == null || equipment.getImages() == null || equipment.getImages().isEmpty()) {
            return;
        }
        boolean primaryFound = false;
        for (EquipmentImage image : equipment.getImages()) {
            if (Boolean.TRUE.equals(image.getIsPrimary())) {
                if (!primaryFound) {
                    primaryFound = true;
                } else {
                    image.setIsPrimary(false);
                }
            }
        }
    }
}
