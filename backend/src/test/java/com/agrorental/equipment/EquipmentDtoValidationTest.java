package com.agrorental.equipment;

import com.agrorental.equipment.dto.*;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Equipment DTO Jakarta Validation Unit Tests")
class EquipmentDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should pass validation for valid EquipmentCreateRequest")
    void shouldPassForValidCreateRequest() {
        EquipmentImageRequest imageRequest = EquipmentImageRequest.builder()
                .imageUrl("https://example.com/tractor.jpg")
                .isPrimary(true)
                .displayOrder(1)
                .build();

        EquipmentCreateRequest request = EquipmentCreateRequest.builder()
                .name("Mahindra 575 DI")
                .category(EquipmentCategory.TRACTOR)
                .brand("Mahindra")
                .model("575 DI")
                .manufacturingYear(2024)
                .capacity("50 HP")
                .rentalPrice(new BigDecimal("1500.00"))
                .fuelType(FuelType.DIESEL)
                .description("Heavy duty tractor for agricultural operations")
                .partnerId(100L)
                .locationAddress("Pune, Maharashtra")
                .latitude(18.5204)
                .longitude(73.8567)
                .images(List.of(imageRequest))
                .build();

        Set<ConstraintViolation<EquipmentCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should fail validation for EquipmentCreateRequest with missing mandatory fields")
    void shouldFailForMissingFieldsInCreateRequest() {
        EquipmentCreateRequest request = EquipmentCreateRequest.builder()
                .rentalPrice(new BigDecimal("-10.00"))
                .build();

        Set<ConstraintViolation<EquipmentCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());

        boolean hasPriceViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("rentalPrice"));
        assertTrue(hasPriceViolation);
    }

    @Test
    @DisplayName("Should pass validation for valid EquipmentUpdateRequest")
    void shouldPassForValidUpdateRequest() {
        EquipmentImageRequest imageRequest = EquipmentImageRequest.builder()
                .imageUrl("https://example.com/tractor.jpg")
                .isPrimary(true)
                .build();

        EquipmentUpdateRequest request = EquipmentUpdateRequest.builder()
                .name("Mahindra 575 DI")
                .category(EquipmentCategory.TRACTOR)
                .brand("Mahindra")
                .model("575 DI")
                .manufacturingYear(2024)
                .capacity("50 HP")
                .rentalPrice(new BigDecimal("1500.00"))
                .fuelType(FuelType.DIESEL)
                .description("Updated description")
                .locationAddress("Pune, Maharashtra")
                .latitude(18.5204)
                .longitude(73.8567)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .isDisabled(false)
                .images(List.of(imageRequest))
                .build();

        Set<ConstraintViolation<EquipmentUpdateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should fail validation for EquipmentImageRequest with blank URL or null primary flag")
    void shouldFailForInvalidImageRequest() {
        EquipmentImageRequest request = EquipmentImageRequest.builder()
                .imageUrl("")
                .isPrimary(null)
                .build();

        Set<ConstraintViolation<EquipmentImageRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());
    }

    @Test
    @DisplayName("Should fail validation for EquipmentSearchRequest with negative prices")
    void shouldFailForNegativePriceSearchRequest() {
        EquipmentSearchRequest request = EquipmentSearchRequest.builder()
                .minPrice(new BigDecimal("-50.00"))
                .build();

        Set<ConstraintViolation<EquipmentSearchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
