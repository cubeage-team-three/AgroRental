package com.agrorental.equipment.specification;

import com.agrorental.equipment.dto.EquipmentSearchRequest;
import com.agrorental.equipment.entity.Equipment;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Data JPA Specification builder for dynamic multi-criteria Equipment filtering.
 * Constructs database-side Criteria API predicates based on search request parameters.
 */
public class EquipmentSpecification {

    /**
     * Builds a composable Specification for Equipment based on the provided search request filters.
     * Enforces public discovery rules (isDisabled = false) and ignores null/blank criteria.
     *
     * @param request Search filter DTO containing optional query criteria
     * @return Specification<Equipment> for JpaSpecificationExecutor query execution
     */
    public static Specification<Equipment> buildSpecification(EquipmentSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always enforce non-disabled state for public discovery
            predicates.add(cb.isFalse(root.get("isDisabled")));

            if (request != null) {
                // Filter by Equipment Category
                if (request.getCategory() != null) {
                    predicates.add(cb.equal(root.get("category"), request.getCategory()));
                }

                // Filter by Minimum Rental Price (rentalPrice >= minPrice)
                if (request.getMinPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("rentalPrice"), request.getMinPrice()));
                }

                // Filter by Maximum Rental Price (rentalPrice <= maxPrice)
                if (request.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("rentalPrice"), request.getMaxPrice()));
                }

                // Filter by Availability Status
                if (request.getAvailabilityStatus() != null) {
                    predicates.add(cb.equal(root.get("availabilityStatus"), request.getAvailabilityStatus()));
                }

                // Filter by Location Address (case-insensitive partial match)
                if (StringUtils.hasText(request.getLocationAddress())) {
                    String pattern = "%" + request.getLocationAddress().trim().toLowerCase() + "%";
                    predicates.add(cb.like(cb.lower(root.get("locationAddress")), pattern));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
