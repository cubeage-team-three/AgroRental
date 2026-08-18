package com.agrorental.equipment;

import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.entity.EquipmentImage;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Equipment & EquipmentImage Entity Domain Unit Tests")
class EquipmentEntityTest {

    @Test
    @DisplayName("Should initialize Equipment entity with correct defaults")
    void shouldInitializeWithDefaults() {
        Equipment equipment = Equipment.builder()
                .name("Mahindra 575 DI")
                .category(EquipmentCategory.TRACTOR)
                .brand("Mahindra")
                .model("575 DI")
                .manufacturingYear(2024)
                .capacity("50 HP")
                .rentalPrice(new BigDecimal("1500.00"))
                .fuelType(FuelType.DIESEL)
                .description("Heavy duty tractor for agricultural operations")
                .locationAddress("Pune, Maharashtra")
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        assertEquals(AvailabilityStatus.AVAILABLE, equipment.getAvailabilityStatus());
        assertFalse(equipment.getIsDisabled());
        assertNotNull(equipment.getImages());
        assertTrue(equipment.getImages().isEmpty());
    }

    @Test
    @DisplayName("Should establish bidirectional relationship using addImage")
    void shouldEstablishBidirectionalRelationshipOnAddImage() {
        Equipment equipment = new Equipment();
        EquipmentImage image = EquipmentImage.builder()
                .imageUrl("https://example.com/tractor.jpg")
                .isPrimary(true)
                .displayOrder(1)
                .build();

        equipment.addImage(image);

        assertEquals(1, equipment.getImages().size());
        assertTrue(equipment.getImages().contains(image));
        assertEquals(equipment, image.getEquipment());
    }

    @Test
    @DisplayName("Should clear relationship using removeImage")
    void shouldClearRelationshipOnRemoveImage() {
        Equipment equipment = new Equipment();
        EquipmentImage image = EquipmentImage.builder()
                .imageUrl("https://example.com/tractor.jpg")
                .build();

        equipment.addImage(image);
        assertEquals(1, equipment.getImages().size());

        equipment.removeImage(image);
        assertTrue(equipment.getImages().isEmpty());
        assertNull(image.getEquipment());
    }
}
