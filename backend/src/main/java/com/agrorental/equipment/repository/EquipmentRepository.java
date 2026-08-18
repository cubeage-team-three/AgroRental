package com.agrorental.equipment.repository;

import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Spring Data JPA Repository interface for Equipment entity persistence and retrieval.
 * Provides standard CRUD, partner lookup, availability discovery queries, and dynamic specification execution.
 */

public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {

    /**
     * Retrieves all equipment owned by a specific partner.
     * Required for Partner equipment management (FR-15).
     *
     * @param partnerId Identifier of the owning partner
     * @return List of equipment entities belonging to the partner
     */
    List<Equipment> findByPartnerId(Long partnerId);

    /**
     * Retrieves equipment matching a specific availability status and non-disabled administrative state.
     * Primary repository query for farmer discovery (FR-05, FR-16, FR-39) where:
     * availabilityStatus = AVAILABLE AND isDisabled = false.
     *
     * @param availabilityStatus Operational availability state (e.g. AvailabilityStatus.AVAILABLE)
     * @return List of discoverable equipment entities
     */
    List<Equipment> findByAvailabilityStatusAndIsDisabledFalse(AvailabilityStatus availabilityStatus);

    /**
     * Retrieves equipment matching both availability status and explicit disabled state.
     * Supports administrative filtering and targeted status inspections (FR-16, FR-39).
     *
     * @param availabilityStatus Operational availability status
     * @param isDisabled Administrative disabled override flag
     * @return List of matching equipment entities
     */
    List<Equipment> findByAvailabilityStatusAndIsDisabled(AvailabilityStatus availabilityStatus, Boolean isDisabled);
}
