package com.agrorental.operator;

import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.partner.entity.Partner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OperatorMapper Unit Tests")
class OperatorMapperTest {

    private OperatorMapper operatorMapper;

    @BeforeEach
    void setUp() {
        operatorMapper = new OperatorMapper();
    }

    @Test
    @DisplayName("toEntity() - Should correctly map OperatorRegistrationRequest and encoded password")
    void shouldMapRegistrationRequestToEntity() {
        OperatorRegistrationRequest request = new OperatorRegistrationRequest();
        request.setFullName("Suresh Shinde");
        request.setMobileNumber("9876543210");
        request.setEmail("suresh@example.com");
        request.setAddress("Village Khed, Pune");
        request.setAadhaarNumber("123456789012");
        request.setDrivingLicenseNumber("MH1220200012345");
        request.setExperience(5);
        request.setSkills("Tractor & Rotavator");
        request.setPassword("SecurePass@123");
        request.setProfilePhoto("photo.jpg");

        Operator operator = operatorMapper.toEntity(request, "encoded_hash_xyz");

        assertNotNull(operator);
        assertEquals("Suresh Shinde", operator.getFullName());
        assertEquals("9876543210", operator.getMobileNumber());
        assertEquals("suresh@example.com", operator.getEmail());
        assertEquals("Village Khed, Pune", operator.getAddress());
        assertEquals("123456789012", operator.getAadhaarNumber());
        assertEquals("MH1220200012345", operator.getDrivingLicenseNumber());
        assertEquals(5, operator.getExperience());
        assertEquals("Tractor & Rotavator", operator.getSkills());
        assertEquals("encoded_hash_xyz", operator.getPassword());
        assertEquals("photo.jpg", operator.getProfilePhoto());
        assertEquals(OperatorStatus.PENDING, operator.getStatus());
    }

    @Test
    @DisplayName("toResponse() - Should correctly map Operator to safe OperatorResponse DTO without sensitive credentials")
    void shouldMapEntityToSafeResponseDto() {
        Partner partner = Partner.builder()
                .fullName("Patel Agro")
                .build();
        partner.setId(10L);

        Operator operator = Operator.builder()
                .fullName("Suresh Shinde")
                .mobileNumber("9876543210")
                .email("suresh@example.com")
                .address("Village Khed, Pune")
                .aadhaarNumber("123456789012")
                .drivingLicenseNumber("MH1220200012345")
                .experience(5)
                .skills("Tractor & Rotavator")
                .password("encoded_hash_xyz")
                .profilePhoto("photo.jpg")
                .status(OperatorStatus.PENDING)
                .partner(partner)
                .build();
        operator.setId(1L);
        operator.setCreatedAt(LocalDateTime.now());
        operator.setUpdatedAt(LocalDateTime.now());
        operator.setActive(true);

        OperatorResponse response = operatorMapper.toResponse(operator);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Suresh Shinde", response.getFullName());
        assertEquals("9876543210", response.getMobileNumber());
        assertEquals("suresh@example.com", response.getEmail());
        assertEquals("Village Khed, Pune", response.getAddress());
        assertEquals(5, response.getExperience());
        assertEquals("Tractor & Rotavator", response.getSkills());
        assertEquals("photo.jpg", response.getProfilePhoto());
        assertEquals(OperatorStatus.PENDING, response.getStatus());
        assertEquals(10L, response.getPartnerId());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    @DisplayName("toEntity() and toResponse() - Should safely handle null inputs")
    void shouldHandleNullInputs() {
        assertNull(operatorMapper.toEntity(null, "pass"));
        assertNull(operatorMapper.toResponse(null));
    }
}
