package com.agrorental.security;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.partner.controller.PartnerController;
import com.agrorental.partner.service.PartnerService;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.dto.PartnerDashboardResponse;
import com.agrorental.partner.dto.PartnerProfileResponse;
import com.agrorental.booking.controller.BookingController;
import com.agrorental.booking.service.BookingService;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.user.repository.UserRepository;
import com.agrorental.security.jwt.JwtAuthenticationFilter;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.security.principal.PartnerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Partner Security & JWT Authentication Filter Integration Tests")
class PartnerSecurityIntegrationTest {

    private MockMvc partnerMockMvc;
    private MockMvc bookingMockMvc;

    @Mock
    private JwtService jwtService;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private PartnerService partnerService;

    @Mock
    private BookingService bookingService;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtService, userRepository, operatorRepository, adminRepository, partnerRepository, farmerRepository
        );

        HandlerMethodArgumentResolver partnerPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(PartnerPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof PartnerPrincipal) {
                    return auth.getPrincipal();
                }
                return null;
            }
        };

        PartnerController partnerController = new PartnerController(partnerService);
        partnerMockMvc = MockMvcBuilders.standaloneSetup(partnerController)
                .addFilter(jwtAuthenticationFilter)
                .setCustomArgumentResolvers(partnerPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        BookingController bookingController = new BookingController(bookingService);
        bookingMockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .addFilter(jwtAuthenticationFilter)
                .setCustomArgumentResolvers(partnerPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Anonymous request to partner dashboard returns 401 Unauthorized")
    void testAnonymousAccessToPartnerDashboardReturns401() throws Exception {
        partnerMockMvc.perform(get("/api/partners/1/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("Valid Partner JWT accessing own dashboard returns 200 OK")
    void testPartnerAccessesOwnDashboard() throws Exception {
        Partner partner = Partner.builder()
                .fullName("Rajesh Patel")
                .mobileNumber("9876543210")
                .verificationStatus(Partner.VerificationStatus.APPROVED)
                .build();
        partner.setId(1L);
        partner.setActive(true);

        when(jwtService.validateToken("valid.jwt")).thenReturn(true);
        when(jwtService.extractUserId("valid.jwt")).thenReturn(1L);
        when(jwtService.extractRole("valid.jwt")).thenReturn("PARTNER");
        when(jwtService.extractMobileNumber("valid.jwt")).thenReturn("9876543210");
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));

        PartnerDashboardResponse dashboardResponse = PartnerDashboardResponse.builder()
                .id(1L)
                .fullName("Rajesh Patel")
                .totalMachines(5)
                .build();

        when(partnerService.getPartnerDashboard(1L)).thenReturn(Optional.of(dashboardResponse));

        partnerMockMvc.perform(get("/api/partners/1/dashboard")
                        .header("Authorization", "Bearer valid.jwt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("Rajesh Patel")));
    }

    @Test
    @DisplayName("Partner accessing another partner's dashboard returns 403 Forbidden")
    void testPartnerAccessesOtherDashboardReturns403() throws Exception {
        Partner partner = Partner.builder()
                .fullName("Rajesh Patel")
                .mobileNumber("9876543210")
                .verificationStatus(Partner.VerificationStatus.APPROVED)
                .build();
        partner.setId(1L);
        partner.setActive(true);

        // Authenticated as Partner ID 1
        when(jwtService.validateToken("valid.jwt")).thenReturn(true);
        when(jwtService.extractUserId("valid.jwt")).thenReturn(1L);
        when(jwtService.extractRole("valid.jwt")).thenReturn("PARTNER");
        when(jwtService.extractMobileNumber("valid.jwt")).thenReturn("9876543210");
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));

        // Requesting dashboard of Partner ID 2
        partnerMockMvc.perform(get("/api/partners/2/dashboard")
                        .header("Authorization", "Bearer valid.jwt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Access is denied. You do not have the required permissions.")));
    }

    @Test
    @DisplayName("Valid Partner JWT accessing own bookings returns 200 OK")
    void testPartnerAccessesOwnBookings() throws Exception {
        Partner partner = Partner.builder()
                .fullName("Rajesh Patel")
                .mobileNumber("9876543210")
                .verificationStatus(Partner.VerificationStatus.APPROVED)
                .build();
        partner.setId(1L);
        partner.setActive(true);

        when(jwtService.validateToken("valid.jwt")).thenReturn(true);
        when(jwtService.extractUserId("valid.jwt")).thenReturn(1L);
        when(jwtService.extractRole("valid.jwt")).thenReturn("PARTNER");
        when(jwtService.extractMobileNumber("valid.jwt")).thenReturn("9876543210");
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));

        BookingResponse bookingResponse = BookingResponse.builder()
                .id(100L)
                .farmerId(10L)
                .equipmentId(5L)
                .totalCost(BigDecimal.valueOf(1500))
                .build();

        when(bookingService.getBookingsByPartner(1L)).thenReturn(Collections.singletonList(bookingResponse));

        bookingMockMvc.perform(get("/api/bookings/partner/1")
                        .header("Authorization", "Bearer valid.jwt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].id", is(100)))
                .andExpect(jsonPath("$.data[0].totalCost", is(1500)));
    }

    @Test
    @DisplayName("Partner accessing another partner's bookings returns 403 Forbidden")
    void testPartnerAccessesOtherBookingsReturns403() throws Exception {
        Partner partner = Partner.builder()
                .fullName("Rajesh Patel")
                .mobileNumber("9876543210")
                .verificationStatus(Partner.VerificationStatus.APPROVED)
                .build();
        partner.setId(1L);
        partner.setActive(true);

        // Authenticated as Partner ID 1
        when(jwtService.validateToken("valid.jwt")).thenReturn(true);
        when(jwtService.extractUserId("valid.jwt")).thenReturn(1L);
        when(jwtService.extractRole("valid.jwt")).thenReturn("PARTNER");
        when(jwtService.extractMobileNumber("valid.jwt")).thenReturn("9876543210");
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));

        // Requesting bookings of Partner ID 2
        bookingMockMvc.perform(get("/api/bookings/partner/2")
                        .header("Authorization", "Bearer valid.jwt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Access is denied. You do not have the required permissions.")));
    }
}
