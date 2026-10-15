package com.agrorental.security;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.dto.EligibleOperatorResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorAssignmentService;
import com.agrorental.operator.service.OperatorService;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.jwt.AccountType;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.security.principal.CurrentCaller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BIZ-23 regression tests for operator personal-data exposure, against the real SecurityConfig,
 * JwtAuthenticationFilter and signed tokens. Partner, farmer, operator and admin #1 share numeric ID 1.
 * Service authorization is also exercised directly (independent of the URL rules).
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:biz23_operator_data_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=biz23-operator-data-secret-key-32-chars-min-hmac",
    "server.port=0"
})
@DisplayName("BIZ-23: Operator personal-data exposure")
class OperatorDataExposureIntegrationTest {

    private static final String X_MOBILE = "9700000301";
    private static final String X_ADDRESS = "12 Hidden Orchard Lane, Pune";
    private static final String Y_MOBILE = "9700000302";
    private static final String PENDING_MOBILE = "9700000303";
    private static final String REJECTED_MOBILE = "9700000304";
    private static final String AADHAAR = "567856785678";
    private static final String LICENCE_PREFIX = "MH-12-DL-";

    @Autowired private WebApplicationContext context;
    @Autowired private JwtService jwtService;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private OperatorRepository operatorRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private OperatorJobAssignmentRepository assignmentRepository;
    @Autowired private OperatorAssignmentService assignmentService;
    @Autowired private OperatorService operatorService;

    private MockMvc mockMvc;

    private Partner partnerA;
    private Partner partnerB;
    private Farmer farmer1;
    private Operator operatorX;
    private Operator operatorY;
    private Admin admin;
    private Booking bookingA;
    private Booking bookingB;

    @BeforeAll
    void createFixtures() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        partnerA = partnerRepository.save(partner("Partner A", "9600000301"));
        partnerB = partnerRepository.save(partner("Partner B", "9600000302"));
        farmer1 = farmerRepository.save(Farmer.builder().fullName("Farmer One").mobileNumber("9800000301")
                .email("farmer1@biz23.test").accountStatus("ACTIVE").build());
        operatorX = operatorRepository.save(operator("Xavier Tractorman", X_MOBILE, X_ADDRESS, OperatorStatus.APPROVED, partnerA));
        operatorY = operatorRepository.save(operator("Yash Harvester", Y_MOBILE, "Lane Y, Nashik", OperatorStatus.APPROVED, partnerB));
        operatorRepository.save(operator("Pending Operator", PENDING_MOBILE, "Lane P", OperatorStatus.PENDING, partnerA));
        operatorRepository.save(operator("Rejected Operator", REJECTED_MOBILE, "Lane R", OperatorStatus.REJECTED, partnerA));
        admin = adminRepository.save(Admin.builder().fullName("Admin").email("admin@biz23.test").password("x").build());

        Equipment equipmentA = equipmentRepository.save(equipment("Tractor A", partnerA));
        Equipment equipmentB = equipmentRepository.save(equipment("Tractor B", partnerB));
        bookingA = bookingRepository.save(booking(partnerA, equipmentA, operatorX));
        bookingB = bookingRepository.save(booking(partnerB, equipmentB, operatorY));
        assignmentRepository.save(OperatorJobAssignment.builder().operator(operatorX).booking(bookingA)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED).assignedAt(LocalDateTime.now()).assignedBy("partner").build());
        assignmentRepository.save(OperatorJobAssignment.builder().operator(operatorY).booking(bookingB)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED).assignedAt(LocalDateTime.now()).assignedBy("partner").build());

        assertThat(partnerA.getId()).isEqualTo(farmer1.getId()).isEqualTo(operatorX.getId()).isEqualTo(admin.getId());
    }

    private static Partner partner(String name, String mobile) {
        return Partner.builder().fullName(name).businessName(name + " Rentals").mobileNumber(mobile).email(mobile + "@biz23.test")
                .password("x").otpVerified(true).verificationStatus(Partner.VerificationStatus.APPROVED).build();
    }

    private static Operator operator(String name, String mobile, String address, OperatorStatus status, Partner partner) {
        Operator operator = new Operator();
        operator.setFullName(name);
        operator.setMobileNumber(mobile);
        operator.setEmail(mobile + "@biz23.test");
        operator.setAddress(address);
        operator.setAadhaarNumber(AADHAAR);
        operator.setDrivingLicenseNumber(LICENCE_PREFIX + mobile);
        operator.setExperience(4);
        operator.setSkills("Tractor Operation");
        operator.setPassword("x");
        operator.setStatus(status);
        operator.setMobileVerified(true);
        operator.setPartner(partner);
        return operator;
    }

    private static Equipment equipment(String name, Partner partner) {
        return Equipment.builder().name(name).category(EquipmentCategory.TRACTOR).brand("Mahindra").model("575 DI")
                .manufacturingYear(2023).capacity("47 HP").rentalPrice(new BigDecimal("2500.00")).fuelType(FuelType.DIESEL)
                .description("Tractor").locationAddress("Pune").latitude(18.52).longitude(73.85)
                .partner(partner).availabilityStatus(AvailabilityStatus.BOOKED).build();
    }

    private Booking booking(Partner partner, Equipment equipment, Operator operator) {
        LocalDate start = LocalDate.of(2026, 12, 1);
        return Booking.builder().farmerId(farmer1.getId()).partner(partner).equipment(equipment).operator(operator)
                .status(BookingStatus.OPERATOR_ASSIGNED).startDate(start).endDate(start.plusDays(2))
                .totalCost(new BigDecimal("7500.00")).deliveryAddress("Farm").build();
    }

    // ---- tokens & helpers ----------------------------------------------------------------------

    private String token(Long id, String role, AccountType type) {
        return "Bearer " + jwtService.generateToken(id, role, "0000000000", JwtService.accountTypeClaims(type));
    }

    private String farmer() { return token(farmer1.getId(), "FARMER", AccountType.FARMER); }
    private String partnerA() { return token(partnerA.getId(), "PARTNER", AccountType.PARTNER); }
    private String partnerB() { return token(partnerB.getId(), "PARTNER", AccountType.PARTNER); }
    private String operatorX() { return token(operatorX.getId(), "OPERATOR", AccountType.OPERATOR); }
    private String admin() { return token(admin.getId(), "ADMIN", AccountType.ADMIN); }

    private ResultActions call(MockHttpServletRequestBuilder request, String authorization) throws Exception {
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return mockMvc.perform(request);
    }

    private String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    /** Denied request: expected status and no operator personal data in the body. */
    private void assertDenied(String path, String authorization, int expectedStatus) throws Exception {
        String body = body(call(get(path), authorization).andExpect(status().is(expectedStatus)));
        for (String secret : new String[]{X_MOBILE, Y_MOBILE, PENDING_MOBILE, REJECTED_MOBILE, X_ADDRESS, AADHAAR, LICENCE_PREFIX}) {
            assertThat(body).as("denied response must not contain %s", secret).doesNotContain(secret);
        }
    }

    /** Identity-document values are never part of any operator list or assignment response. */
    private void assertNoIdentityDocuments(String body) {
        assertThat(body).doesNotContain(AADHAAR).doesNotContain(LICENCE_PREFIX);
    }

    // ---- GET /api/bookings/{id}/operator ----------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/bookings/{id}/operator")
    class BookingAssignment {

        @Test
        @DisplayName("Anonymous 401; farmer, assigned operator and another partner 403 without operator data")
        void denied() throws Exception {
            String path = "/api/bookings/" + bookingA.getId() + "/operator";
            assertDenied(path, null, 401);
            assertDenied(path, farmer(), 403);
            assertDenied(path, operatorX(), 403);
            assertDenied(path, partnerB(), 403);
        }

        @Test
        @DisplayName("The owning partner and admin see the assignment with the assigned operator's contact number")
        void ownerAndAdmin() throws Exception {
            String path = "/api/bookings/" + bookingA.getId() + "/operator";
            for (String authorization : new String[]{partnerA(), admin()}) {
                String body = body(call(get(path), authorization)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.operatorId").value(operatorX.getId()))
                        .andExpect(jsonPath("$.data.operatorMobile").value(X_MOBILE)));
                assertNoIdentityDocuments(body);
            }
        }

        @Test
        @DisplayName("Service refuses a partner for another partner's booking, independent of URL rules")
        void serviceAuthorization() {
            assertThatThrownBy(() -> assignmentService.getBookingAssignmentForCaller(bookingB.getId(),
                    new CurrentCaller(CurrentCaller.Kind.PARTNER, partnerA.getId()))).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> assignmentService.getBookingAssignmentForCaller(bookingA.getId(),
                    new CurrentCaller(CurrentCaller.Kind.FARMER, farmer1.getId()))).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> assignmentService.getBookingAssignmentForCaller(bookingA.getId(),
                    new CurrentCaller(CurrentCaller.Kind.OPERATOR, operatorX.getId()))).isInstanceOf(ForbiddenException.class);
        }
    }

    // ---- GET /api/operators/eligible ----------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/operators/eligible")
    class Eligible {

        @Test
        @DisplayName("Anonymous 401; farmer and operator 403 without operator data")
        void denied() throws Exception {
            assertDenied("/api/operators/eligible", null, 401);
            assertDenied("/api/operators/eligible", farmer(), 403);
            assertDenied("/api/operators/eligible", operatorX(), 403);
        }

        @Test
        @DisplayName("Partner sees only approved operators, with masked mobile, no address and no identity documents")
        void partnerMinimizedView() throws Exception {
            String body = body(call(get("/api/operators/eligible"), partnerB())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].operatorId").value(hasItems(
                            operatorX.getId().intValue(), operatorY.getId().intValue())))
                    .andExpect(jsonPath("$.data.content[*].status").value(everyItem(org.hamcrest.Matchers.is("APPROVED"))))
                    .andExpect(jsonPath("$.data.content[*].mobileNumber").value(hasItem("******0301"))));
            assertThat(body).doesNotContain(X_MOBILE).doesNotContain(Y_MOBILE).doesNotContain(X_ADDRESS)
                    .doesNotContain(PENDING_MOBILE.substring(6)).doesNotContain(REJECTED_MOBILE.substring(6));
            assertNoIdentityDocuments(body);
        }

        @Test
        @DisplayName("Partner search matches name/skills only: mobile and address cannot be probed")
        void partnerSearchCannotProbeContactDetails() throws Exception {
            call(get("/api/operators/eligible").param("search", "Xavier"), partnerB())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].operatorId").value(hasItem(operatorX.getId().intValue())));
            call(get("/api/operators/eligible").param("search", X_MOBILE.substring(3)), partnerB())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty());
            call(get("/api/operators/eligible").param("search", "Hidden Orchard"), partnerB())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }

        @Test
        @DisplayName("Admin keeps the full view and full search")
        void adminFullView() throws Exception {
            String body = body(call(get("/api/operators/eligible").param("search", X_MOBILE), admin())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].mobileNumber").value(X_MOBILE))
                    .andExpect(jsonPath("$.data.content[0].address").value(X_ADDRESS)));
            assertNoIdentityDocuments(body);
        }

        @Test
        @DisplayName("Service refuses farmers/operators and minimizes partner data, independent of URL rules")
        void serviceAuthorization() {
            assertThatThrownBy(() -> assignmentService.findEligibleOperatorsForCaller(null, PageRequest.of(0, 10),
                    new CurrentCaller(CurrentCaller.Kind.FARMER, farmer1.getId()))).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> assignmentService.findEligibleOperatorsForCaller(null, PageRequest.of(0, 10),
                    new CurrentCaller(CurrentCaller.Kind.OPERATOR, operatorX.getId()))).isInstanceOf(ForbiddenException.class);

            Page<EligibleOperatorResponse> page = assignmentService.findEligibleOperatorsForCaller(null, PageRequest.of(0, 10),
                    new CurrentCaller(CurrentCaller.Kind.PARTNER, partnerB.getId()));
            assertThat(page.getContent()).isNotEmpty().allSatisfy(op -> {
                assertThat(op.getAddress()).isNull();
                assertThat(op.getMobileNumber()).startsWith("******").hasSize(10);
            });
        }
    }

    // ---- GET /api/operators, /available, /partner/{id} ---------------------------------------------------

    @Nested
    @DisplayName("Operator directories: GET /api/operators, /available, /partner/{id}")
    class Directories {

        @Test
        @DisplayName("GET /api/operators: admin only; everyone else denied without data")
        void allOperators() throws Exception {
            assertDenied("/api/operators", null, 401);
            assertDenied("/api/operators", farmer(), 403);
            assertDenied("/api/operators", partnerA(), 403);
            assertDenied("/api/operators", operatorX(), 403);
            String body = body(call(get("/api/operators"), admin()).andExpect(status().isOk()));
            assertThat(body).contains(PENDING_MOBILE).contains(REJECTED_MOBILE);
            assertNoIdentityDocuments(body);
        }

        @Test
        @DisplayName("GET /api/operators/available: admin only, even with a forged partnerId")
        void availableOperators() throws Exception {
            String forged = "/api/operators/available?partnerId=" + partnerA.getId();
            assertDenied(forged, null, 401);
            assertDenied(forged, farmer(), 403);
            assertDenied(forged, partnerA(), 403);
            assertDenied(forged, operatorX(), 403);
            assertNoIdentityDocuments(body(call(get(forged), admin()).andExpect(status().isOk())));
        }

        @Test
        @DisplayName("GET /api/operators/partner/{id}: own roster for that partner and admin; forged IDs denied")
        void partnerRoster() throws Exception {
            String rosterA = "/api/operators/partner/" + partnerA.getId();
            assertDenied(rosterA, null, 401);
            assertDenied(rosterA, farmer(), 403);
            assertDenied(rosterA, operatorX(), 403);
            assertDenied(rosterA, partnerB(), 403);

            for (String authorization : new String[]{partnerA(), admin()}) {
                String body = body(call(get(rosterA), authorization)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[*].id").value(hasItem(operatorX.getId().intValue())))
                        .andExpect(jsonPath("$.data[*].id").value(not(hasItem(operatorY.getId().intValue())))));
                assertNoIdentityDocuments(body);
            }
        }

        @Test
        @DisplayName("Service authorization holds independent of URL rules")
        void serviceAuthorization() {
            CurrentCaller partner = new CurrentCaller(CurrentCaller.Kind.PARTNER, partnerA.getId());
            assertThatThrownBy(() -> operatorService.getAllOperatorsForCaller(partner)).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> operatorService.getAvailableOperatorsForCaller(partnerA.getId(), null, null, partner))
                    .isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> operatorService.getOperatorsByPartnerForCaller(partnerB.getId(), partner))
                    .isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> operatorService.getOperatorsByPartnerForCaller(partnerA.getId(),
                    new CurrentCaller(CurrentCaller.Kind.OPERATOR, operatorX.getId()))).isInstanceOf(ForbiddenException.class);
            assertThat(operatorService.getOperatorsByPartnerForCaller(partnerA.getId(), partner)).isNotEmpty();
        }
    }
}
