package com.agrorental.partner;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.partner.controller.PartnerController;
import com.agrorental.partner.dto.PartnerChangePasswordRequest;
import com.agrorental.partner.dto.PartnerProfileResponse;
import com.agrorental.partner.dto.PartnerProfileUpdateRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.service.PartnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartnerController Standalone MockMvc Unit Tests")
class PartnerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PartnerService partnerService;

    @InjectMocks
    private PartnerController partnerController;

    private PartnerProfileResponse testProfile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(partnerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testProfile = PartnerProfileResponse.builder()
                .id(1L)
                .fullName("Rajesh Patel")
                .businessName("Patel Agro Fleet")
                .mobileNumber("9876543210")
                .email("rajesh.patel@example.com")
                .address("Agro Hub, Pune")
                .verificationStatus(Partner.VerificationStatus.APPROVED)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("GET /api/partners/{id} - Should return 200 OK with profile")
    void shouldGetPartnerProfile() throws Exception {
        when(partnerService.getPartnerProfile(1L)).thenReturn(testProfile);

        mockMvc.perform(get("/api/partners/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fullName").value("Rajesh Patel"))
                .andExpect(jsonPath("$.data.businessName").value("Patel Agro Fleet"))
                .andExpect(jsonPath("$.data.mobileNumber").value("9876543210"));
    }

    @Test
    @DisplayName("GET /api/partners/{id} - Should return 404 when partner not found")
    void shouldReturn404WhenPartnerNotFound() throws Exception {
        when(partnerService.getPartnerProfile(99L))
                .thenThrow(new ResourceNotFoundException("Partner profile not found with ID: 99"));

        mockMvc.perform(get("/api/partners/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Partner profile not found with ID: 99"));
    }

    @Test
    @DisplayName("PUT /api/partners/{id} - Should return 200 OK on profile update")
    void shouldUpdatePartnerProfile() throws Exception {
        PartnerProfileResponse updatedResponse = PartnerProfileResponse.builder()
                .id(1L)
                .fullName("Rajesh Patel Updated")
                .businessName("Patel & Sons")
                .email("rajesh.updated@example.com")
                .build();

        when(partnerService.updatePartnerProfile(eq(1L), any(PartnerProfileUpdateRequest.class)))
                .thenReturn(updatedResponse);

        String jsonPayload = """
                {
                    "fullName": "Rajesh Patel Updated",
                    "businessName": "Patel & Sons",
                    "email": "rajesh.updated@example.com",
                    "address": "New Address, Pune"
                }
                """;

        mockMvc.perform(put("/api/partners/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Rajesh Patel Updated"))
                .andExpect(jsonPath("$.data.businessName").value("Patel & Sons"));
    }

    @Test
    @DisplayName("PUT /api/partners/{id}/password - Should return 200 OK on password update")
    void shouldChangePassword() throws Exception {
        doNothing().when(partnerService).changePartnerPassword(eq(1L), any(PartnerChangePasswordRequest.class));

        String jsonPayload = """
                {
                    "currentPassword": "old_pass_123",
                    "newPassword": "new_pass_456"
                }
                """;

        mockMvc.perform(put("/api/partners/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }
}
