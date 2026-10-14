package com.agrorental.security;

import com.agrorental.booking.controller.BookingController;
import com.agrorental.booking.dto.BookingCreateRequest;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.farmer.controller.FarmerBookingController;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.jwt.JwtAuthenticationFilter;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.security.principal.FarmerPrincipal;
import com.agrorental.security.principal.PartnerPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("E2E-002: Farmer Booking Creation IDOR Audit and Regression Tests")
class FarmerBookingCreationE2E002RegressionTest {

    private MockMvc mockMvc;

    @Mock
    private JwtService jwtService;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private com.agrorental.user.repository.UserRepository userRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private BookingService bookingService;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final Long FARMER_A_ID = 100L;
    private final Long FARMER_B_ID = 200L;
    private final Long PARTNER_ID = 300L;
    private final Long OPERATOR_ID = 400L;
    private final Long ADMIN_ID = 500L;

    private final String TOKEN_FARMER_A = "bearer.token.farmerA";
    private final String TOKEN_PARTNER = "bearer.token.partner";
    private final String TOKEN_OPERATOR = "bearer.token.operator";
    private final String TOKEN_ADMIN = "bearer.token.admin";
    private final String TOKEN_INVALID = "bearer.token.invalid";

    private BookingResponse sampleResponse;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtService,
                userRepository,
                operatorRepository,
                null, // adminRepository
                partnerRepository,
                farmerRepository
        );

        BookingController bookingController = new BookingController(bookingService);
        FarmerBookingController farmerBookingController = new FarmerBookingController(bookingService);

        mockMvc = MockMvcBuilders.standaloneSetup(bookingController, farmerBookingController)
                .addFilter(jwtAuthenticationFilter)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // Farmer A setup in repository
        Farmer farmerA = Farmer.builder()
                .fullName("Farmer Ramesh")
                .mobileNumber("9876543210")
                .accountStatus("ACTIVE")
                .build();
        farmerA.setId(FARMER_A_ID);
        farmerA.setActive(true);

        lenient().when(jwtService.validateToken(TOKEN_FARMER_A)).thenReturn(true);
        lenient().when(jwtService.extractUserId(TOKEN_FARMER_A)).thenReturn(FARMER_A_ID);
        lenient().when(jwtService.extractRole(TOKEN_FARMER_A)).thenReturn("FARMER");
        lenient().when(jwtService.extractMobileNumber(TOKEN_FARMER_A)).thenReturn("9876543210");
        lenient().when(farmerRepository.findById(FARMER_A_ID)).thenReturn(Optional.of(farmerA));

        // Partner setup in repository
        Partner partner = Partner.builder()
                .fullName("Partner Suresh")
                .mobileNumber("9876543222")
                .businessName("Agro Equipment Hub")
                .build();
        partner.setId(PARTNER_ID);
        partner.setActive(true);

        lenient().when(jwtService.validateToken(TOKEN_PARTNER)).thenReturn(true);
        lenient().when(jwtService.extractUserId(TOKEN_PARTNER)).thenReturn(PARTNER_ID);
        lenient().when(jwtService.extractRole(TOKEN_PARTNER)).thenReturn("PARTNER");
        lenient().when(jwtService.extractMobileNumber(TOKEN_PARTNER)).thenReturn("9876543222");
        lenient().when(partnerRepository.findById(PARTNER_ID)).thenReturn(Optional.of(partner));

        // Operator setup in repository
        Operator operator = Operator.builder()
                .fullName("Operator Mahesh")
                .mobileNumber("9876543233")
                .status(OperatorStatus.APPROVED)
                .build();
        operator.setId(OPERATOR_ID);
        operator.setActive(true);

        lenient().when(jwtService.validateToken(TOKEN_OPERATOR)).thenReturn(true);
        lenient().when(jwtService.extractUserId(TOKEN_OPERATOR)).thenReturn(OPERATOR_ID);
        lenient().when(jwtService.extractRole(TOKEN_OPERATOR)).thenReturn("OPERATOR");
        lenient().when(jwtService.extractMobileNumber(TOKEN_OPERATOR)).thenReturn("9876543233");
        lenient().when(operatorRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

        // Admin setup in repository
        com.agrorental.user.entity.User adminUser = com.agrorental.user.entity.User.builder()
                .name("Admin User")
                .email("admin@agrorent.in")
                .role(com.agrorental.common.enums.Role.ADMIN)
                .enabled(true)
                .build();
        adminUser.setId(ADMIN_ID);

        lenient().when(jwtService.validateToken(TOKEN_ADMIN)).thenReturn(true);
        lenient().when(jwtService.extractUserId(TOKEN_ADMIN)).thenReturn(ADMIN_ID);
        lenient().when(jwtService.extractRole(TOKEN_ADMIN)).thenReturn("ADMIN");
        lenient().when(jwtService.extractMobileNumber(TOKEN_ADMIN)).thenReturn("9876543299");
        lenient().when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminUser));

        // Invalid token
        lenient().when(jwtService.validateToken(TOKEN_INVALID)).thenReturn(false);

        sampleResponse = BookingResponse.builder()
                .id(1001L)
                .farmerId(FARMER_A_ID)
                .equipmentId(10L)
                .equipmentName("John Deere 5050D")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .totalCost(BigDecimal.valueOf(4500))
                .status(BookingStatus.PENDING)
                .deliveryAddress("Farm Plot 12, Pune")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("1. Authenticated Farmer Can Create Valid Booking For Themselves")
    class AuthenticatedFarmerSuccessTests {

        @Test
        @DisplayName("POST /api/bookings: Derives farmer identity from JWT principal when farmerId is omitted")
        void testFarmerCreatesBookingWithOmittedFarmerId() throws Exception {
            when(bookingService.createBooking(any())).thenReturn(sampleResponse);

            String payload = """
                {
                  "equipmentId": 10,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Farm Plot 12, Pune"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + TOKEN_FARMER_A)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(1001)))
                    .andExpect(jsonPath("$.data.farmerId", is(FARMER_A_ID.intValue())));

            ArgumentCaptor<BookingCreateRequest> captor = ArgumentCaptor.forClass(BookingCreateRequest.class);
            verify(bookingService).createBooking(captor.capture());
            assertEquals(FARMER_A_ID, captor.getValue().getFarmerId(),
                    "Service must receive request populated with authenticated farmer's ID");
        }

        @Test
        @DisplayName("POST /api/bookings: Allows request when client farmerId matches authenticated principal")
        void testFarmerCreatesBookingWithMatchingFarmerId() throws Exception {
            when(bookingService.createBooking(any())).thenReturn(sampleResponse);

            String payload = """
                {
                  "equipmentId": 10,
                  "farmerId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Farm Plot 12, Pune"
                }
                """.formatted(FARMER_A_ID, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + TOKEN_FARMER_A)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.farmerId", is(FARMER_A_ID.intValue())));

            ArgumentCaptor<BookingCreateRequest> captor = ArgumentCaptor.forClass(BookingCreateRequest.class);
            verify(bookingService).createBooking(captor.capture());
            assertEquals(FARMER_A_ID, captor.getValue().getFarmerId());
        }

        @Test
        @DisplayName("POST /api/farmers/bookings: Successfully creates booking through farmer-specific route")
        void testFarmerCreatesBookingViaFarmerRoute() throws Exception {
            when(bookingService.createBooking(any())).thenReturn(sampleResponse);

            String payload = """
                {
                  "equipmentId": 10,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Farm Plot 12, Pune"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/farmers/bookings")
                            .header("Authorization", "Bearer " + TOKEN_FARMER_A)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(1001)));

            ArgumentCaptor<BookingCreateRequest> captor = ArgumentCaptor.forClass(BookingCreateRequest.class);
            verify(bookingService).createBooking(captor.capture());
            assertEquals(FARMER_A_ID, captor.getValue().getFarmerId());
        }
    }

    @Nested
    @DisplayName("2. IDOR Prevention: Farmer A Cannot Create Booking As Farmer B")
    class IdorPreventionTests {

        @Test
        @DisplayName("POST /api/bookings: Rejects request with 403 when Farmer A supplies Farmer B's ID")
        void testFarmerCannotCreateBookingForAnotherFarmerOnBookingsRoute() throws Exception {
            String maliciousPayload = """
                {
                  "equipmentId": 10,
                  "farmerId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Attacker Farm Location"
                }
                """.formatted(FARMER_B_ID, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + TOKEN_FARMER_A)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(maliciousPayload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", is("Access is denied. You do not have the required permissions.")));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("POST /api/farmers/bookings: Rejects request with 403 when Farmer A supplies Farmer B's ID")
        void testFarmerCannotCreateBookingForAnotherFarmerOnFarmerBookingsRoute() throws Exception {
            String maliciousPayload = """
                {
                  "equipmentId": 10,
                  "farmerId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Attacker Farm Location"
                }
                """.formatted(FARMER_B_ID, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/farmers/bookings")
                            .header("Authorization", "Bearer " + TOKEN_FARMER_A)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(maliciousPayload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", is("Access is denied. You do not have the required permissions.")));

            verify(bookingService, never()).createBooking(any());
        }
    }

    @Nested
    @DisplayName("3. Unauthenticated and Invalid Authentication Rejected")
    class AuthenticationRejectionTests {

        @Test
        @DisplayName("POST /api/bookings: Rejects unauthenticated request without token")
        void testUnauthenticatedBookingOnBookingsEndpointRejected() throws Exception {
            String payload = """
                {
                  "equipmentId": 10,
                  "farmerId": 100,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Anonymous Attempt"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("POST /api/bookings: Rejects request with invalid JWT token")
        void testInvalidTokenBookingOnBookingsEndpointRejected() throws Exception {
            String payload = """
                {
                  "equipmentId": 10,
                  "farmerId": 100,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Forged Token Attempt"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + TOKEN_INVALID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("POST /api/farmers/bookings: Rejects unauthenticated request without token")
        void testUnauthenticatedBookingRejected() throws Exception {
            String payload = """
                {
                  "equipmentId": 10,
                  "farmerId": 100,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Anonymous Attempt"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/farmers/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("POST /api/farmers/bookings: Rejects request with invalid JWT token")
        void testInvalidTokenBookingRejected() throws Exception {
            String payload = """
                {
                  "equipmentId": 10,
                  "farmerId": 100,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Forged Token Attempt"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/farmers/bookings")
                            .header("Authorization", "Bearer " + TOKEN_INVALID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));

            verify(bookingService, never()).createBooking(any());
        }
    }

    @Nested
    @DisplayName("4. Non-Farmer Roles Cannot Use Farmer Booking Endpoints")
    class RoleAuthorizationTests {

        @Test
        @DisplayName("POST /api/bookings: Rejects partner principal with 403 Forbidden")
        void testPartnerCannotCreateBookingOnBookingsEndpoint() throws Exception {
            String payload = """
                {
                  "equipmentId": 10,
                  "farmerId": 100,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Partner attempting booking"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + TOKEN_PARTNER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("POST /api/bookings: Rejects operator principal with 403 Forbidden")
        void testOperatorCannotCreateBookingOnBookingsEndpoint() throws Exception {
            String payload = """
                {
                  "equipmentId": 10,
                  "farmerId": 100,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Operator attempting booking"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + TOKEN_OPERATOR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("POST /api/bookings: Rejects admin principal with 403 Forbidden")
        void testAdminCannotCreateBookingOnBookingsEndpoint() throws Exception {
            String payload = """
                {
                  "equipmentId": 10,
                  "farmerId": 100,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Admin attempting booking"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + TOKEN_ADMIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("POST /api/farmers/bookings: Rejects partner principal with 403 Forbidden")
        void testPartnerCannotCreateBookingOnFarmerBookingsEndpoint() throws Exception {
            String payload = """
                {
                  "equipmentId": 10,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Partner attempting booking"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/farmers/bookings")
                            .header("Authorization", "Bearer " + TOKEN_PARTNER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("POST /api/farmers/bookings: Rejects operator principal with 403 Forbidden")
        void testOperatorCannotCreateBookingOnFarmerBookingsEndpoint() throws Exception {
            String payload = """
                {
                  "equipmentId": 10,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Operator attempting booking"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/farmers/bookings")
                            .header("Authorization", "Bearer " + TOKEN_OPERATOR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("POST /api/farmers/bookings: Rejects admin principal with 403 Forbidden")
        void testAdminCannotCreateBookingOnFarmerBookingsEndpoint() throws Exception {
            String payload = """
                {
                  "equipmentId": 10,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Admin attempting booking"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/farmers/bookings")
                            .header("Authorization", "Bearer " + TOKEN_ADMIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));

            verify(bookingService, never()).createBooking(any());
        }
    }

    @Nested
    @DisplayName("5. Booking Validation and Availability Business Rules")
    class BookingValidationTests {

        @Test
        @DisplayName("Fails validation when required equipmentId is missing")
        void testMissingEquipmentIdFailsValidation() throws Exception {
            String payload = """
                {
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Pune"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + TOKEN_FARMER_A)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("Service rejects when end date is before start date")
        void testInvalidDateRangeRejected() throws Exception {
            when(bookingService.createBooking(any()))
                    .thenThrow(new BadRequestException("End date cannot be before start date"));

            String payload = """
                {
                  "equipmentId": 10,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Pune"
                }
                """.formatted(LocalDate.now().plusDays(5), LocalDate.now().plusDays(2));

            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + TOKEN_FARMER_A)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", is("End date cannot be before start date")));
        }

        @Test
        @DisplayName("Service rejects overlapping reservation conflict")
        void testOverlappingDatesRejected() throws Exception {
            when(bookingService.createBooking(any()))
                    .thenThrow(new BadRequestException("Equipment is already reserved for the selected date range"));

            String payload = """
                {
                  "equipmentId": 10,
                  "startDate": "%s",
                  "endDate": "%s",
                  "deliveryAddress": "Pune"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + TOKEN_FARMER_A)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", is("Equipment is already reserved for the selected date range")));
        }
    }

    @Nested
    @DisplayName("6. Service-Level Defense-in-Depth Security Tests")
    class ServiceLevelSecurityTests {

        @Test
        @DisplayName("BookingService rejects Farmer A requesting as Farmer B via SecurityContext")
        void testBookingServiceDirectSecurityCheckRejectsMismatch() {
            FarmerPrincipal principalA = FarmerPrincipal.builder().id(FARMER_A_ID).role("FARMER").build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principalA, null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_FARMER")))
            );

            BookingCreateRequest request = BookingCreateRequest.builder()
                    .equipmentId(10L)
                    .farmerId(FARMER_B_ID) // Malicious target ID
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .deliveryAddress("Pune")
                    .build();

            // Re-instantiate actual BookingService with mock repos to test real service method logic
            com.agrorental.booking.repository.BookingRepository mockBookingRepo = mock(com.agrorental.booking.repository.BookingRepository.class);
            com.agrorental.equipment.repository.EquipmentRepository mockEquipRepo = mock(com.agrorental.equipment.repository.EquipmentRepository.class);
            com.agrorental.farmer.repository.FarmRepository mockFarmRepo = mock(com.agrorental.farmer.repository.FarmRepository.class);
            com.agrorental.notification.service.NotificationService mockNotif = mock(com.agrorental.notification.service.NotificationService.class);
            com.agrorental.booking.mapper.BookingMapper mockMapper = mock(com.agrorental.booking.mapper.BookingMapper.class);

            BookingService realService = new BookingService(
                    mockBookingRepo,
                    mockEquipRepo,
                    mock(com.agrorental.operator.repository.OperatorRepository.class),
                    mockFarmRepo,
                    mockNotif,
                    mockMapper
            );

            assertThrows(AccessDeniedException.class, () -> realService.createBooking(request));
        }

        @Test
        @DisplayName("BookingService derives farmerId from SecurityContext when request farmerId is null")
        void testBookingServiceDerivesFarmerIdWhenNull() {
            FarmerPrincipal principalA = FarmerPrincipal.builder().id(FARMER_A_ID).role("FARMER").build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principalA, null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_FARMER")))
            );

            BookingCreateRequest request = BookingCreateRequest.builder()
                    .equipmentId(10L)
                    .farmerId(null) // Omitted
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .deliveryAddress("Pune")
                    .build();

            com.agrorental.equipment.entity.Equipment equipment = com.agrorental.equipment.entity.Equipment.builder()
                    .name("Tractor")
                    .rentalPrice(BigDecimal.valueOf(1500))
                    .availabilityStatus(com.agrorental.equipment.enums.AvailabilityStatus.AVAILABLE)
                    .isDisabled(false)
                    .partner(Partner.builder().build())
                    .build();
            equipment.setId(10L);

            com.agrorental.booking.entity.Booking savedBooking = com.agrorental.booking.entity.Booking.builder()
                    .farmerId(FARMER_A_ID)
                    .equipment(equipment)
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .totalCost(BigDecimal.valueOf(4500))
                    .status(BookingStatus.PENDING)
                    .build();

            com.agrorental.booking.repository.BookingRepository mockBookingRepo = mock(com.agrorental.booking.repository.BookingRepository.class);
            com.agrorental.equipment.repository.EquipmentRepository mockEquipRepo = mock(com.agrorental.equipment.repository.EquipmentRepository.class);
            com.agrorental.farmer.repository.FarmRepository mockFarmRepo = mock(com.agrorental.farmer.repository.FarmRepository.class);
            com.agrorental.notification.service.NotificationService mockNotif = mock(com.agrorental.notification.service.NotificationService.class);
            com.agrorental.booking.mapper.BookingMapper mockMapper = mock(com.agrorental.booking.mapper.BookingMapper.class);

            when(mockEquipRepo.findById(10L)).thenReturn(Optional.of(equipment));
            when(mockBookingRepo.existsOverlappingBooking(any(), any(), any(), any())).thenReturn(false);
            when(mockMapper.toEntity(any(), any(), any(), any(), any())).thenReturn(savedBooking);
            when(mockBookingRepo.save(any())).thenReturn(savedBooking);
            when(mockMapper.toResponse(any())).thenReturn(sampleResponse);

            BookingService realService = new BookingService(
                    mockBookingRepo,
                    mockEquipRepo,
                    mock(com.agrorental.operator.repository.OperatorRepository.class),
                    mockFarmRepo,
                    mockNotif,
                    mockMapper
            );

            realService.createBooking(request);

            assertEquals(FARMER_A_ID, request.getFarmerId(), "Farmer ID must be set to authenticated principal ID");
        }

        @Test
        @DisplayName("BookingService rejects Partner principal attempting to call createBooking")
        void testBookingServiceRejectsNonFarmerPrincipal() {
            PartnerPrincipal partnerPrincipal = PartnerPrincipal.builder().id(PARTNER_ID).role("PARTNER").build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(partnerPrincipal, null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARTNER")))
            );

            BookingCreateRequest request = BookingCreateRequest.builder()
                    .equipmentId(10L)
                    .farmerId(FARMER_A_ID)
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .deliveryAddress("Pune")
                    .build();

            BookingService realService = new BookingService(
                    mock(com.agrorental.booking.repository.BookingRepository.class),
                    mock(com.agrorental.equipment.repository.EquipmentRepository.class),
                    mock(com.agrorental.operator.repository.OperatorRepository.class),
                    mock(com.agrorental.farmer.repository.FarmRepository.class),
                    mock(com.agrorental.notification.service.NotificationService.class),
                    mock(com.agrorental.booking.mapper.BookingMapper.class)
            );

            assertThrows(AccessDeniedException.class, () -> realService.createBooking(request));
        }

        @Test
        @DisplayName("BookingService rejects when no authentication context exists (null SecurityContext)")
        void testBookingServiceRejectsWhenNoAuthenticationContext() {
            SecurityContextHolder.clearContext();

            BookingCreateRequest request = BookingCreateRequest.builder()
                    .equipmentId(10L)
                    .farmerId(FARMER_A_ID)
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .deliveryAddress("Pune")
                    .build();

            BookingService realService = new BookingService(
                    mock(com.agrorental.booking.repository.BookingRepository.class),
                    mock(com.agrorental.equipment.repository.EquipmentRepository.class),
                    mock(com.agrorental.operator.repository.OperatorRepository.class),
                    mock(com.agrorental.farmer.repository.FarmRepository.class),
                    mock(com.agrorental.notification.service.NotificationService.class),
                    mock(com.agrorental.booking.mapper.BookingMapper.class)
            );

            assertThrows(AccessDeniedException.class, () -> realService.createBooking(request));
        }

        @Test
        @DisplayName("BookingService rejects when anonymous principal is present")
        void testBookingServiceRejectsWhenAnonymousPrincipal() {
            org.springframework.security.authentication.AnonymousAuthenticationToken anonToken =
                    new org.springframework.security.authentication.AnonymousAuthenticationToken(
                            "anonKey",
                            "anonymousUser",
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                    );
            SecurityContextHolder.getContext().setAuthentication(anonToken);

            BookingCreateRequest request = BookingCreateRequest.builder()
                    .equipmentId(10L)
                    .farmerId(FARMER_A_ID)
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .deliveryAddress("Pune")
                    .build();

            BookingService realService = new BookingService(
                    mock(com.agrorental.booking.repository.BookingRepository.class),
                    mock(com.agrorental.equipment.repository.EquipmentRepository.class),
                    mock(com.agrorental.operator.repository.OperatorRepository.class),
                    mock(com.agrorental.farmer.repository.FarmRepository.class),
                    mock(com.agrorental.notification.service.NotificationService.class),
                    mock(com.agrorental.booking.mapper.BookingMapper.class)
            );

            assertThrows(AccessDeniedException.class, () -> realService.createBooking(request));
        }

        @Test
        @DisplayName("BookingService rejects when authentication token is unauthenticated")
        void testBookingServiceRejectsWhenUnauthenticatedContext() {
            org.springframework.security.core.Authentication unauth = mock(org.springframework.security.core.Authentication.class);
            when(unauth.isAuthenticated()).thenReturn(false);
            SecurityContextHolder.getContext().setAuthentication(unauth);

            BookingCreateRequest request = BookingCreateRequest.builder()
                    .equipmentId(10L)
                    .farmerId(FARMER_A_ID)
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .deliveryAddress("Pune")
                    .build();

            BookingService realService = new BookingService(
                    mock(com.agrorental.booking.repository.BookingRepository.class),
                    mock(com.agrorental.equipment.repository.EquipmentRepository.class),
                    mock(com.agrorental.operator.repository.OperatorRepository.class),
                    mock(com.agrorental.farmer.repository.FarmRepository.class),
                    mock(com.agrorental.notification.service.NotificationService.class),
                    mock(com.agrorental.booking.mapper.BookingMapper.class)
            );

            assertThrows(AccessDeniedException.class, () -> realService.createBooking(request));
        }

        @Test
        @DisplayName("BookingService rejects Operator principal attempting to call createBooking")
        void testBookingServiceRejectsOperatorPrincipal() {
            com.agrorental.security.principal.OperatorPrincipal operatorPrincipal =
                    com.agrorental.security.principal.OperatorPrincipal.builder().id(OPERATOR_ID).role("OPERATOR").build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(operatorPrincipal, null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_OPERATOR")))
            );

            BookingCreateRequest request = BookingCreateRequest.builder()
                    .equipmentId(10L)
                    .farmerId(FARMER_A_ID)
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .deliveryAddress("Pune")
                    .build();

            BookingService realService = new BookingService(
                    mock(com.agrorental.booking.repository.BookingRepository.class),
                    mock(com.agrorental.equipment.repository.EquipmentRepository.class),
                    mock(com.agrorental.operator.repository.OperatorRepository.class),
                    mock(com.agrorental.farmer.repository.FarmRepository.class),
                    mock(com.agrorental.notification.service.NotificationService.class),
                    mock(com.agrorental.booking.mapper.BookingMapper.class)
            );

            assertThrows(AccessDeniedException.class, () -> realService.createBooking(request));
        }

        @Test
        @DisplayName("BookingService rejects Admin principal attempting to call createBooking")
        void testBookingServiceRejectsAdminPrincipal() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("ADMIN_" + ADMIN_ID, null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
            );

            BookingCreateRequest request = BookingCreateRequest.builder()
                    .equipmentId(10L)
                    .farmerId(FARMER_A_ID)
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .deliveryAddress("Pune")
                    .build();

            BookingService realService = new BookingService(
                    mock(com.agrorental.booking.repository.BookingRepository.class),
                    mock(com.agrorental.equipment.repository.EquipmentRepository.class),
                    mock(com.agrorental.operator.repository.OperatorRepository.class),
                    mock(com.agrorental.farmer.repository.FarmRepository.class),
                    mock(com.agrorental.notification.service.NotificationService.class),
                    mock(com.agrorental.booking.mapper.BookingMapper.class)
            );

            assertThrows(AccessDeniedException.class, () -> realService.createBooking(request));
        }
    }
}
