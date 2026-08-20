package com.agrorental.equipment;

import com.agrorental.equipment.dto.EquipmentSearchRequest;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.specification.EquipmentSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("EquipmentSpecification Unit Tests")
class EquipmentSpecificationTest {

    @Test
    @DisplayName("Should build valid Specification with null request")
    void testBuildSpecification_NullRequest() {
        Specification<?> spec = EquipmentSpecification.buildSpecification(null);
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Should build valid Specification with full filter request including date range, rating, and Haversine distance")
    void testBuildSpecification_FullRequest() {
        EquipmentSearchRequest request = EquipmentSearchRequest.builder()
                .category(EquipmentCategory.TRACTOR)
                .minPrice(BigDecimal.valueOf(1000))
                .maxPrice(BigDecimal.valueOf(5000))
                .minHp(40)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .locationAddress("Pune")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .minRating(4.0)
                .userLat(18.5204)
                .userLng(73.8567)
                .maxDistanceKm(50.0)
                .build();

        Specification<?> spec = EquipmentSpecification.buildSpecification(request);
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Should build helper specification predicates safely")
    void testSpecificationHelperMethods() {
        assertNotNull(EquipmentSpecification.hasPriceBetween(BigDecimal.valueOf(500), BigDecimal.valueOf(2500)));
        assertNotNull(EquipmentSpecification.hasMinHp(50));
        assertNotNull(EquipmentSpecification.locationContains("Nashik"));
    }
}
