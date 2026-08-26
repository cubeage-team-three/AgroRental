package com.agrorental.partner;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.partner.controller.PartnerController;
import com.agrorental.partner.dto.PartnerChangePasswordRequest;
import com.agrorental.partner.dto.PartnerProfileResponse;
import com.agrorental.partner.dto.PartnerProfileUpdateRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.service.PartnerService;
import com.agrorental.security.principal.PartnerPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

        // Set up security context with PartnerPrincipal for partner ID 1
        PartnerPrincipal principal = PartnerPrincipal.builder()
                .id(1L)
                .mobileNumber("9876543210")
                .fullName("Rajesh Patel")
                .role("PARTNER")
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/partners - Should return 200 OK with list of all partners")
    void shouldGetAllPartners() throws Exception {
        when(partnerService.getAllPartners()).thenReturn(java.util.List.of(testProfile));

        mockMvc.perform(get("/api/partners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].fullName").value("Rajesh Patel"))
                .andExpect(jsonPath("$.data[0].businessName").value("Patel Agro Fleet"))
                .andExpect(jsonPath("$.data[0].mobileNumber").value("9876543210"));
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
        when(partnerService.getPartnerProfile(1L))
                .thenThrow(new ResourceNotFoundException("Partner profile not found with ID: 1"));

        mockMvc.perform(get("/api/partners/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Partner profile not found with ID: 1"));
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

    @Test
    @DisplayName("POST /api/partners/register - Should return 201 Created on registration")
    void shouldRegisterPartner() throws Exception {
        Partner partner = Partner.builder()
                .fullName("Rajesh Patel")
                .mobileNumber("9876543210")
                .email("rajesh.patel@example.com")
                .build();
        partner.setId(1L);

        when(partnerService.registerPartner(any())).thenReturn(partner);
        when(partnerService.toProfileResponse(any(Partner.class))).thenReturn(testProfile);

        String jsonPayload = """
                {
                    "fullName": "Rajesh Patel",
                    "mobileNumber": "9876543210",
                    "email": "rajesh.patel@example.com",
                    "password": "secret_password"
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/partners/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Partner registered successfully"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("GET /api/partners/{id}/dashboard - Should return 200 OK with dashboard metrics")
    void shouldGetPartnerDashboard() throws Exception {
        com.agrorental.partner.dto.PartnerDashboardResponse dashboard = com.agrorental.partner.dto.PartnerDashboardResponse.builder()
                .id(1L)
                .fullName("Rajesh Patel")
                .totalMachines(5)
                .activeMachines(4)
                .pendingBookings(2)
                .completedBookings(10)
                .monthlyRevenue(java.math.BigDecimal.valueOf(25000))
                .customerRatings(4.9)
                .build();

        when(partnerService.getPartnerDashboard(1L)).thenReturn(java.util.Optional.of(dashboard));

        mockMvc.perform(get("/api/partners/1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Rajesh Patel"))
                .andExpect(jsonPath("$.data.totalMachines").value(5))
                .andExpect(jsonPath("$.data.activeMachines").value(4));
    }

    @Test
    @DisplayName("POST /api/partners/{id}/otp/send - Should return 200 OK with OTP")
    void shouldSendOtp() throws Exception {
        when(partnerService.sendOtp(1L)).thenReturn("123456");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/partners/1/otp/send"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("123456"));
    }

    @Test
    @DisplayName("POST /api/partners/{id}/otp/verify - Should return 200 OK on successful verification")
    void shouldVerifyOtp() throws Exception {
        when(partnerService.verifyOtp(eq(1L), eq("123456"))).thenReturn(testProfile);

        String jsonPayload = """
                {
                    "otp": "123456"
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/partners/1/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    @DisplayName("POST /api/partners/{id}/otp/resend - Should return 200 OK on OTP resend")
    void shouldResendOtp() throws Exception {
        when(partnerService.resendOtp(1L)).thenReturn("654321");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/partners/1/otp/resend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("654321"));
    }

    @Test
    @DisplayName("PUT /api/partners/{id}/kyc/approve - Should return 200 OK on KYC approval")
    void shouldApprovePartnerKyc() throws Exception {
        when(partnerService.approvePartnerKyc(1L)).thenReturn(testProfile);

        mockMvc.perform(put("/api/partners/1/kyc/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Partner KYC approved successfully"));
    }

    @Test
    @DisplayName("PUT /api/partners/{id}/kyc/reject - Should return 200 OK on KYC rejection")
    void shouldRejectPartnerKyc() throws Exception {
        PartnerProfileResponse rejectedProfile = PartnerProfileResponse.builder()
                .id(1L)
                .fullName("Rajesh Patel")
                .verificationStatus(Partner.VerificationStatus.REJECTED)
                .active(false)
                .build();

        when(partnerService.rejectPartnerKyc(1L)).thenReturn(rejectedProfile);

        mockMvc.perform(put("/api/partners/1/kyc/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Partner KYC rejected successfully"));
    }
}
