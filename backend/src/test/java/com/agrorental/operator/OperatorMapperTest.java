package com.agrorental.operator;

import com.agrorental.operator.dto.OperatorDetailResponse;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.dto.OperatorSummaryResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorDocument;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.DocumentStatus;
import com.agrorental.operator.enums.DocumentType;
import com.agrorental.operator.mapper.OperatorDocumentMapper;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.partner.entity.Partner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OperatorMapper Unit Tests")
class OperatorMapperTest {

    private OperatorMapper operatorMapper;
    private OperatorDocumentMapper documentMapper;

    @BeforeEach
    void setUp() {
        documentMapper = new OperatorDocumentMapper();
        operatorMapper = new OperatorMapper(documentMapper);
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
        assertFalse(operator.isMobileVerified());
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
                .mobileVerified(true)
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
        assertTrue(response.isMobileVerified());
        assertEquals(10L, response.getPartnerId());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    @DisplayName("toSummaryResponse() and toDetailResponse() - Should correctly mask Aadhaar and DL and map documents")
    void shouldMapSummaryAndDetailResponsesWithMasking() {
        Operator operator = Operator.builder()
                .fullName("Suresh Shinde")
                .mobileNumber("9876543210")
                .email("suresh@example.com")
                .address("Village Khed, Pune")
                .aadhaarNumber("123456789012")
                .drivingLicenseNumber("MH1220200012345")
                .experience(5)
                .skills("Tractor")
                .status(OperatorStatus.PENDING)
                .mobileVerified(true)
                .documents(new ArrayList<>())
                .build();
        operator.setId(1L);
        operator.setCreatedAt(LocalDateTime.now());

        OperatorDocument doc = OperatorDocument.builder()
                .documentType(DocumentType.AADHAAR)
                .documentNumber("123456789012")
                .fileName("aadhaar.pdf")
                .fileUrl("https://storage.agrorental.com/aadhaar.pdf")
                .verificationStatus(DocumentStatus.PENDING)
                .build();
        doc.setId(101L);
        doc.setCreatedAt(LocalDateTime.now());
        operator.addDocument(doc);

        OperatorSummaryResponse summary = operatorMapper.toSummaryResponse(operator);
        assertNotNull(summary);
        assertEquals(1L, summary.getId());
        assertEquals("Suresh Shinde", summary.getFullName());
        assertEquals(1, summary.getDocumentsCount());
        assertTrue(summary.isMobileVerified());

        OperatorDetailResponse detail = operatorMapper.toDetailResponse(operator);
        assertNotNull(detail);
        assertEquals(1L, detail.getId());
        assertEquals("XXXX-XXXX-9012", detail.getMaskedAadhaarNumber());
        assertEquals("DL-XXXX-2345", detail.getMaskedDrivingLicenseNumber());
        assertEquals(1, detail.getDocuments().size());
        assertEquals("XXXX-XXXX-9012", detail.getDocuments().get(0).getMaskedDocumentNumber());
    }

    @Test
    @DisplayName("toAuthenticatedResponse() - Should correctly map Operator to AuthenticatedOperatorResponse")
    void shouldMapEntityToAuthenticatedResponse() {
        Operator operator = Operator.builder()
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .email("rajesh@agrorental.com")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        operator.setId(1L);
        operator.setActive(true);

        com.agrorental.operator.dto.AuthenticatedOperatorResponse response = operatorMapper.toAuthenticatedResponse(operator);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Rajesh Shinde", response.getFullName());
        assertEquals("9876543210", response.getMobileNumber());
        assertEquals("rajesh@agrorental.com", response.getEmail());
        assertEquals("OPERATOR", response.getRole());
        assertEquals(OperatorStatus.APPROVED, response.getStatus());
        assertTrue(response.isMobileVerified());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("toProfileResponse() - Should correctly map Operator to safe OperatorProfileResponse")
    void shouldMapEntityToProfileResponse() {
        Operator operator = Operator.builder()
                .fullName("Sunil Jadhav")
                .mobileNumber("9876543210")
                .email("sunil@agrorental.com")
                .address("Nashik, Maharashtra")
                .experience(7)
                .skills("Tractor, Seeder")
                .aadhaarNumber("123456789012")
                .drivingLicenseNumber("MH1520210098765")
                .profilePhoto("photo.jpg")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        operator.setId(1L);
        operator.setActive(true);
        operator.setCreatedAt(LocalDateTime.now());
        operator.setUpdatedAt(LocalDateTime.now());

        com.agrorental.operator.dto.OperatorProfileResponse response = operatorMapper.toProfileResponse(operator);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Sunil Jadhav", response.getFullName());
        assertEquals("9876543210", response.getMobileNumber());
        assertEquals("sunil@agrorental.com", response.getEmail());
        assertEquals("Nashik, Maharashtra", response.getAddress());
        assertEquals(7, response.getExperience());
        assertEquals("Tractor, Seeder", response.getSkills());
        assertEquals("photo.jpg", response.getProfilePhoto());
        assertEquals("XXXX-XXXX-9012", response.getMaskedAadhaarNumber());
        assertEquals("DL-XXXX-8765", response.getMaskedDrivingLicenseNumber());
        assertEquals(OperatorStatus.APPROVED, response.getStatus());
        assertTrue(response.isMobileVerified());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    @DisplayName("toEntity(), toResponse(), toAuthenticatedResponse(), and toProfileResponse() - Should safely handle null inputs")
    void shouldHandleNullInputs() {
        assertNull(operatorMapper.toEntity(null, "pass"));
        assertNull(operatorMapper.toResponse(null));
        assertNull(operatorMapper.toSummaryResponse(null));
        assertNull(operatorMapper.toDetailResponse(null));
        assertNull(operatorMapper.toAuthenticatedResponse(null));
        assertNull(operatorMapper.toProfileResponse(null));
    }
}
