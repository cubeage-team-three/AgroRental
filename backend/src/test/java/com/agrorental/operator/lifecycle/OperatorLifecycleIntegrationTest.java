package com.agrorental.operator.lifecycle;

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
import com.agrorental.notification.entity.Notification;
import com.agrorental.notification.repository.NotificationRepository;
import com.agrorental.operator.dto.OperatorJobRejectionRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorJobPauseInterval;
import com.agrorental.operator.entity.OperatorLocation;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorJobPauseIntervalRepository;
import com.agrorental.operator.repository.OperatorLocationRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorJobLifecycleService;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.jwt.AccountType;
import com.agrorental.security.jwt.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Operator lifecycle end to end against the real SecurityConfig, JWT filter, services and JPA (isolated H2):
 * eligibility (contract v1.1: APPROVED + active, mobileVerified is informational only), assignment rules, the
 * job state machine with its booking / equipment / tracking / dashboard / earnings / notification side effects,
 * operator isolation and concurrent conflicting requests on real threads.
 *
 * <p>Every test builds its own bookings in a date window no other booking uses, so operator schedule-overlap
 * rules never couple tests together. Operators X, Y and Z are shared; E is used only by the full-lifecycle test
 * so its dashboard and earnings totals can be asserted exactly.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:wc_operator_lifecycle_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;LOCK_TIMEOUT=20000",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=wc-operator-lifecycle-test-secret-key-32-chars-minimum",
    "server.port=0"
})
@DisplayName("Operator lifecycle E2E: eligibility, assignment, transitions, side effects, isolation, concurrency")
class OperatorLifecycleIntegrationTest {

    private static final LocalDate BASE_DATE = LocalDate.now().plusYears(2).withDayOfYear(1);
    private static final String DELIVERY_PREFIX = "Private Field Lane ";
    private static final int CONCURRENCY_ROUNDS = 6;

    @Autowired private WebApplicationContext context;
    @Autowired private JwtService jwtService;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private OperatorRepository operatorRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private OperatorJobAssignmentRepository assignmentRepository;
    @Autowired private OperatorJobPauseIntervalRepository pauseIntervalRepository;
    @Autowired private OperatorLocationRepository locationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private OperatorJobLifecycleService lifecycleService;
    @Autowired private PlatformTransactionManager transactionManager;

    private MockMvc mockMvc;
    private TransactionTemplate tx;
    private final AtomicInteger windowCounter = new AtomicInteger();

    private Partner partnerA;
    private Partner partnerB;
    private Farmer farmer1;
    private Farmer farmer2;
    /** APPROVED + active, mobile NOT verified (contract v1.1: fully operational). */
    private Operator operatorX;
    /** APPROVED + active + verified. */
    private Operator operatorY;
    /** APPROVED + active, unverified; concurrency rounds only. */
    private Operator operatorZ;
    /** APPROVED + active, unverified; full-lifecycle test only (exact dashboard / earnings totals). */
    private Operator operatorE;
    private Operator operatorPending;
    private Operator operatorRejected;
    /** APPROVED but account inactive. */
    private Operator operatorInactive;
    private Admin admin;
    private Equipment equipmentA;
    private Equipment equipmentB;

    @BeforeAll
    void createFixtures() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        tx = new TransactionTemplate(transactionManager);

        partnerA = partnerRepository.save(partner("Lifecycle Partner A", "9611100001"));
        partnerB = partnerRepository.save(partner("Lifecycle Partner B", "9611100002"));
        farmer1 = farmerRepository.save(farmer("Lifecycle Farmer One", "9811100001"));
        farmer2 = farmerRepository.save(farmer("Lifecycle Farmer Two", "9811100002"));
        operatorX = operatorRepository.save(operator("Xavier Unverified", "9711100001", OperatorStatus.APPROVED, true, false));
        operatorY = operatorRepository.save(operator("Yash Verified", "9711100002", OperatorStatus.APPROVED, true, true));
        operatorZ = operatorRepository.save(operator("Zoya Racer", "9711100003", OperatorStatus.APPROVED, true, false));
        operatorE = operatorRepository.save(operator("Esha Earner", "9711100007", OperatorStatus.APPROVED, true, false));
        operatorPending = operatorRepository.save(operator("Pending Pradeep", "9711100004", OperatorStatus.PENDING, true, false));
        operatorRejected = operatorRepository.save(operator("Rejected Rohan", "9711100005", OperatorStatus.REJECTED, true, true));
        operatorInactive = operatorRepository.save(operator("Inactive Imran", "9711100006", OperatorStatus.APPROVED, false, true));
        admin = adminRepository.save(Admin.builder().fullName("Lifecycle Admin").email("admin@wc-lifecycle.test").password("x").build());
        equipmentA = equipmentRepository.save(equipment("Lifecycle Tractor A", partnerA));
        equipmentB = equipmentRepository.save(equipment("Lifecycle Tractor B", partnerB));

        assertThat(operatorX.isMobileVerified()).isFalse();
        assertThat(operatorE.isMobileVerified()).isFalse();
    }

    /**
     * Standalone-MockMvc security tests elsewhere in the suite can leave an authentication on this thread, and the
     * JWT filter keeps an existing authentication instead of reading the token. Start every test clean.
     */
    @BeforeEach
    void resetSecurityContext() {
        clearSecurityContext();
    }

    private static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        TestSecurityContextHolder.clearContext();
    }

    // ---- fixtures ------------------------------------------------------------------------------

    private static Partner partner(String name, String mobile) {
        return Partner.builder().fullName(name).businessName(name + " Rentals").mobileNumber(mobile)
                .email(mobile + "@wc-lifecycle.test").password("x").otpVerified(true)
                .verificationStatus(Partner.VerificationStatus.APPROVED).build();
    }

    private static Farmer farmer(String name, String mobile) {
        return Farmer.builder().fullName(name).mobileNumber(mobile).email(mobile + "@wc-lifecycle.test")
                .accountStatus("ACTIVE").build();
    }

    private static Operator operator(String name, String mobile, OperatorStatus status, boolean active, boolean mobileVerified) {
        Operator operator = new Operator();
        operator.setFullName(name);
        operator.setMobileNumber(mobile);
        operator.setEmail(mobile + "@wc-lifecycle.test");
        operator.setAddress("Home of " + name);
        operator.setAadhaarNumber("999988887777");
        operator.setDrivingLicenseNumber("MH-14-LC-" + mobile);
        operator.setExperience(5);
        operator.setSkills("Tractor Operation");
        operator.setPassword("x");
        operator.setHourlyRate(new BigDecimal("600.00"));
        operator.setStatus(status);
        operator.setMobileVerified(mobileVerified);
        operator.setActive(active);
        return operator;
    }

    private static Equipment equipment(String name, Partner partner) {
        return Equipment.builder().name(name).category(EquipmentCategory.TRACTOR).brand("Mahindra").model("575 DI")
                .manufacturingYear(2023).capacity("47 HP").rentalPrice(new BigDecimal("2500.00")).fuelType(FuelType.DIESEL)
                .description("Tractor").locationAddress("Pune").latitude(18.52).longitude(73.85)
                .partner(partner).availabilityStatus(AvailabilityStatus.AVAILABLE).build();
    }

    /** A booking in its own, never-reused 3-day window. */
    private Booking booking(Farmer farmer, Partner partner, Equipment equipment, BookingStatus status) {
        LocalDate start = BASE_DATE.plusDays(10L * windowCounter.incrementAndGet());
        return bookingRepository.save(Booking.builder().farmerId(farmer.getId()).partner(partner).equipment(equipment)
                .status(status).startDate(start).endDate(start.plusDays(2)).totalCost(new BigDecimal("7500.00"))
                .deliveryAddress(DELIVERY_PREFIX + start).build());
    }

    private Booking confirmedA() {
        return booking(farmer1, partnerA, equipmentA, BookingStatus.CONFIRMED);
    }

    // ---- tokens & HTTP helpers -----------------------------------------------------------------

    private String token(Long id, String role, AccountType type) {
        return "Bearer " + jwtService.generateToken(id, role, "0000000000", JwtService.accountTypeClaims(type));
    }

    private String partnerA() { return token(partnerA.getId(), "PARTNER", AccountType.PARTNER); }
    private String partnerB() { return token(partnerB.getId(), "PARTNER", AccountType.PARTNER); }
    private String farmer1() { return token(farmer1.getId(), "FARMER", AccountType.FARMER); }
    private String admin() { return token(admin.getId(), "ADMIN", AccountType.ADMIN); }
    private String op(Operator operator) { return token(operator.getId(), "OPERATOR", AccountType.OPERATOR); }

    private ResultActions call(MockHttpServletRequestBuilder request, String authorization) throws Exception {
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return mockMvc.perform(request);
    }

    private ResultActions assign(Booking booking, Operator operator, String authorization) throws Exception {
        return call(post("/api/bookings/" + booking.getId() + "/operator").contentType(MediaType.APPLICATION_JSON)
                .content("{\"operatorId\":" + operator.getId() + ",\"notes\":\"lifecycle test\"}"), authorization);
    }

    private static MockHttpServletRequestBuilder action(Long assignmentId, String action) {
        MockHttpServletRequestBuilder request = patch("/api/operators/jobs/" + assignmentId + "/" + action)
                .contentType(MediaType.APPLICATION_JSON);
        return switch (action) {
            case "reject" -> request.content("{\"rejectionReason\":\"Tractor clutch is broken\"}");
            case "pause" -> request.content("{\"pauseReason\":\"Rain break\"}");
            case "complete" -> request.content("{\"completionNotes\":\"Field done\"}");
            default -> request;
        };
    }

    private ResultActions act(Long assignmentId, String action, Operator operator) throws Exception {
        return call(action(assignmentId, action), op(operator));
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    /** Assigns via the canonical API as the owning partner A and returns the new assignment ID. */
    private Long assignAsPartnerA(Booking booking, Operator operator) throws Exception {
        String json = body(assign(booking, operator, partnerA()).andExpect(status().isCreated()));
        return ((Number) JsonPath.read(json, "$.data.assignmentId")).longValue();
    }

    /** Drives an assignment through the given actions as its operator, expecting 200 for each. */
    private void drive(Long assignmentId, Operator operator, String... actions) throws Exception {
        for (String a : actions) {
            act(assignmentId, a, operator).andExpect(status().isOk());
        }
    }

    // ---- state readers (each in its own transaction; lazy associations resolved inside) -----------

    private <T> T inTx(Supplier<T> supplier) {
        return tx.execute(status -> supplier.get());
    }

    private BookingStatus bookingStatus(Booking booking) {
        return inTx(() -> bookingRepository.findById(booking.getId()).orElseThrow().getStatus());
    }

    private Long bookingOperatorId(Booking booking) {
        return inTx(() -> {
            Operator operator = bookingRepository.findById(booking.getId()).orElseThrow().getOperator();
            return operator != null ? operator.getId() : null;
        });
    }

    private AvailabilityStatus equipmentStatus(Equipment equipment) {
        return inTx(() -> equipmentRepository.findById(equipment.getId()).orElseThrow().getAvailabilityStatus());
    }

    private OperatorAssignmentStatus assignmentStatus(Long assignmentId) {
        return inTx(() -> assignmentRepository.findById(assignmentId).orElseThrow().getAssignmentStatus());
    }

    private List<OperatorAssignmentStatus> assignmentStatusesFor(Booking booking) {
        return inTx(() -> assignmentRepository.findAllByBookingId(booking.getId()).stream()
                .sorted(Comparator.comparing(OperatorJobAssignment::getId))
                .map(OperatorJobAssignment::getAssignmentStatus).toList());
    }

    private List<OperatorJobPauseInterval> intervals(Long assignmentId) {
        return inTx(() -> pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(assignmentId));
    }

    private List<OperatorLocation> locations(Long assignmentId) {
        return inTx(() -> locationRepository.findAll().stream()
                .filter(l -> l.getAssignment().getId().equals(assignmentId)).toList());
    }

    private List<Notification> notificationsFor(String role, Long recipientId, Booking booking) {
        return notificationRepository.findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc(role, recipientId).stream()
                .filter(n -> booking.getId().equals(n.getBookingId())).toList();
    }

    private void setBookingStatusOutOfBand(Booking booking, BookingStatus status) {
        tx.executeWithoutResult(s -> bookingRepository.findById(booking.getId()).orElseThrow().setStatus(status));
    }

    // =============================================================================================
    // 1. Eligibility
    // =============================================================================================

    @Nested
    @DisplayName("1. GET /api/operators/eligible")
    class Eligibility {

        private List<Integer> ids(ResultActions result) throws Exception {
            return JsonPath.read(body(result), "$.data.content[*].operatorId");
        }

        @Test
        @DisplayName("Partner and admin: APPROVED + active operators are eligible even with an unverified mobile; pending/rejected/inactive never")
        void eligibleSet() throws Exception {
            for (String authorization : new String[]{partnerA(), admin()}) {
                List<Integer> ids = ids(call(get("/api/operators/eligible").param("size", "100"), authorization)
                        .andExpect(status().isOk()));
                assertThat(ids).contains(operatorX.getId().intValue(), operatorY.getId().intValue(), operatorZ.getId().intValue())
                        .doesNotContain(operatorPending.getId().intValue(), operatorRejected.getId().intValue(),
                                operatorInactive.getId().intValue());
            }
        }

        @Test
        @DisplayName("Search finds an unverified operator (partner: name/skills; admin: also mobile); never a pending one")
        void searchIncludesUnverified() throws Exception {
            assertThat(ids(call(get("/api/operators/eligible").param("search", "Xavier"), partnerA()).andExpect(status().isOk())))
                    .containsExactly(operatorX.getId().intValue());
            assertThat(ids(call(get("/api/operators/eligible").param("search", "Pending"), partnerA()).andExpect(status().isOk())))
                    .isEmpty();
            assertThat(ids(call(get("/api/operators/eligible").param("search", operatorX.getMobileNumber()), admin())
                    .andExpect(status().isOk()))).containsExactly(operatorX.getId().intValue());
            assertThat(ids(call(get("/api/operators/eligible").param("search", operatorInactive.getMobileNumber()), admin())
                    .andExpect(status().isOk()))).isEmpty();
        }

        @Test
        @DisplayName("Farmer and operator are refused; anonymous is unauthenticated")
        void refused() throws Exception {
            call(get("/api/operators/eligible"), farmer1()).andExpect(status().isForbidden());
            call(get("/api/operators/eligible"), op(operatorX)).andExpect(status().isForbidden());
            call(get("/api/operators/eligible"), null).andExpect(status().isUnauthorized());
        }
    }

    // =============================================================================================
    // 2. Assignment rules
    // =============================================================================================

    @Nested
    @DisplayName("2. Assignment rules")
    class Assignment {

        private void assertUntouched(Booking booking) {
            assertThat(bookingStatus(booking)).isEqualTo(booking.getStatus());
            assertThat(bookingOperatorId(booking)).isNull();
            assertThat(assignmentStatusesFor(booking)).isEmpty();
        }

        @Test
        @DisplayName("Owning partner assigns an unverified-mobile operator: booking OPERATOR_ASSIGNED, equipment flag untouched, operator + farmer notified")
        void owningPartnerAssigns() throws Exception {
            Booking booking = confirmedA();
            AvailabilityStatus equipmentBefore = equipmentStatus(equipmentA);
            assign(booking, operatorX, partnerA())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.operatorId").value(operatorX.getId()))
                    .andExpect(jsonPath("$.data.assignmentStatus").value("ASSIGNED"));

            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
            assertThat(bookingOperatorId(booking)).isEqualTo(operatorX.getId());
            assertThat(assignmentStatusesFor(booking)).containsExactly(OperatorAssignmentStatus.ASSIGNED);
            assertThat(equipmentStatus(equipmentA)).as("CONFIRMED -> OPERATOR_ASSIGNED keeps the reservation; no flag write")
                    .isEqualTo(equipmentBefore);
            assertThat(notificationsFor("OPERATOR", operatorX.getId(), booking)).extracting(Notification::getNotificationType)
                    .contains("OPERATOR_ASSIGNED");
            assertThat(notificationsFor("FARMER", farmer1.getId(), booking)).extracting(Notification::getNotificationType)
                    .contains("OPERATOR_ASSIGNED");
            assertThat(operatorRepository.findById(operatorX.getId()).orElseThrow().isMobileVerified())
                    .as("no workaround: assignment must not flip the verification flag").isFalse();
        }

        @Test
        @DisplayName("Assignment never overrides a partner-owned UNDER_MAINTENANCE / INACTIVE equipment flag")
        void assignmentKeepsPartnerOwnedEquipmentFlags() throws Exception {
            for (AvailabilityStatus partnerFlag : new AvailabilityStatus[]{AvailabilityStatus.UNDER_MAINTENANCE, AvailabilityStatus.INACTIVE}) {
                Equipment machine = equipmentRepository.save(equipment("Flagged Tractor " + partnerFlag, partnerA));
                Booking booking = booking(farmer1, partnerA, machine, BookingStatus.CONFIRMED);
                tx.executeWithoutResult(s -> equipmentRepository.findById(machine.getId()).orElseThrow().setAvailabilityStatus(partnerFlag));
                // Whether a flagged machine may be assigned at all is booking policy (Worker B); the invariant here is
                // that the assignment itself never rewrites the partner's flag
                int code = assign(booking, operatorY, partnerA()).andReturn().getResponse().getStatus();
                assertThat(code).isIn(201, 400, 409);
                assertThat(equipmentStatus(machine)).isEqualTo(partnerFlag);
            }
        }

        @Test
        @DisplayName("Admin may assign on any partner's booking")
        void adminAssigns() throws Exception {
            Booking booking = booking(farmer2, partnerB, equipmentB, BookingStatus.CONFIRMED);
            assign(booking, operatorY, admin()).andExpect(status().isCreated());
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        }

        @Test
        @DisplayName("Other partner, farmer, operator and anonymous cannot assign; nothing changes")
        void nonOwnersCannotAssign() throws Exception {
            Booking booking = confirmedA();
            assign(booking, operatorX, partnerB()).andExpect(status().isBadRequest());
            assign(booking, operatorX, farmer1()).andExpect(status().isForbidden());
            assign(booking, operatorX, op(operatorX)).andExpect(status().isForbidden());
            assign(booking, operatorX, null).andExpect(status().isUnauthorized());
            assertUntouched(booking);
        }

        @Test
        @DisplayName("Pending, rejected and inactive operators are never assignable")
        void ineligibleOperatorsRefused() throws Exception {
            Booking booking = confirmedA();
            assign(booking, operatorPending, partnerA()).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Operator is not approved"));
            assign(booking, operatorRejected, admin()).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Operator is not approved"));
            assign(booking, operatorInactive, partnerA()).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Operator account is inactive"));
            assertUntouched(booking);
        }

        @Test
        @DisplayName("Only CONFIRMED (or OPERATOR_ASSIGNED without an active job) bookings are assignable")
        void onlyAssignableBookingStates() throws Exception {
            for (BookingStatus status : new BookingStatus[]{BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.ON_THE_WAY,
                    BookingStatus.WORK_STARTED, BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.REJECTED}) {
                Booking booking = booking(farmer1, partnerA, equipmentA, status);
                assign(booking, operatorX, partnerA()).andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Booking with status " + status + " is not eligible for operator assignment"));
                assertUntouched(booking);
            }
        }

        @Test
        @DisplayName("No double assignment while the first assignment is active")
        void noDoubleAssignment() throws Exception {
            Booking booking = confirmedA();
            assignAsPartnerA(booking, operatorX);
            assign(booking, operatorY, admin()).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Booking is already assigned to an active operator"));
            assign(booking, operatorX, partnerA()).andExpect(status().isBadRequest());
            assertThat(assignmentStatusesFor(booking)).containsExactly(OperatorAssignmentStatus.ASSIGNED);
            assertThat(bookingOperatorId(booking)).isEqualTo(operatorX.getId());
        }

        @Test
        @DisplayName("An operator cannot take overlapping dates; a later window is fine")
        void overlapRefused() throws Exception {
            Booking first = confirmedA();
            assignAsPartnerA(first, operatorY);
            Booking overlapping = bookingRepository.save(Booking.builder().farmerId(farmer2.getId()).partner(partnerB)
                    .equipment(equipmentB).status(BookingStatus.CONFIRMED).startDate(first.getStartDate().plusDays(1))
                    .endDate(first.getEndDate().plusDays(1)).totalCost(new BigDecimal("5000.00")).deliveryAddress("Overlap").build());
            assign(overlapping, operatorY, partnerB()).andExpect(status().isBadRequest());
            assertUntouched(overlapping);

            assign(booking(farmer2, partnerB, equipmentB, BookingStatus.CONFIRMED), operatorY, partnerB())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("PATCH /assign-operator (partner only) applies the same canonical rules")
        void legacyPartnerEndpointMatchesCanonicalRules() throws Exception {
            Booking booking = confirmedA();
            String path = "/api/bookings/" + booking.getId() + "/assign-operator";
            String pendingBody = "{\"operatorId\":" + operatorPending.getId() + "}";
            String xBody = "{\"operatorId\":" + operatorX.getId() + "}";

            call(patch(path).contentType(MediaType.APPLICATION_JSON).content(pendingBody), partnerA()).andExpect(status().isForbidden());
            call(patch(path).contentType(MediaType.APPLICATION_JSON).content(xBody), partnerB()).andExpect(status().isBadRequest());
            call(patch(path).contentType(MediaType.APPLICATION_JSON).content(xBody), admin()).andExpect(status().isForbidden());
            call(patch(path).contentType(MediaType.APPLICATION_JSON).content(xBody), farmer1()).andExpect(status().isForbidden());
            assertUntouched(booking);

            call(patch(path).contentType(MediaType.APPLICATION_JSON).content(xBody), partnerA())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("OPERATOR_ASSIGNED"));
            assertThat(assignmentStatusesFor(booking)).containsExactly(OperatorAssignmentStatus.ASSIGNED);
            call(patch(path).contentType(MediaType.APPLICATION_JSON).content("{\"operatorId\":" + operatorY.getId() + "}"), partnerA())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Admin status update to OPERATOR_ASSIGNED with an operator goes through the canonical rules")
        void adminStatusAssignmentUsesCanonicalRules() throws Exception {
            Booking booking = confirmedA();
            String path = "/api/bookings/" + booking.getId() + "/status";
            call(patch(path).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"OPERATOR_ASSIGNED\",\"operatorId\":" + operatorRejected.getId() + "}"), admin())
                    .andExpect(status().isForbidden());
            assertUntouched(booking);

            call(patch(path).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"OPERATOR_ASSIGNED\",\"operatorId\":" + operatorX.getId() + "}"), admin())
                    .andExpect(status().isOk());
            assertThat(assignmentStatusesFor(booking)).containsExactly(OperatorAssignmentStatus.ASSIGNED);
            assertThat(bookingOperatorId(booking)).isEqualTo(operatorX.getId());
        }

        @Test
        @DisplayName("The owning partner keeps seeing the current assignment after the operator accepts, works and completes")
        void partnerSeesCurrentAssignmentThroughoutTheJob() throws Exception {
            Booking booking = confirmedA();
            Long assignmentId = assignAsPartnerA(booking, operatorX);
            String path = "/api/bookings/" + booking.getId() + "/operator";

            call(get(path), partnerA()).andExpect(status().isOk()).andExpect(jsonPath("$.data.assignmentStatus").value("ASSIGNED"));
            drive(assignmentId, operatorX, "accept");
            call(get(path), partnerA()).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assignmentId").value(assignmentId))
                    .andExpect(jsonPath("$.data.assignmentStatus").value("ACCEPTED"));
            drive(assignmentId, operatorX, "start-travel", "reached", "start-work");
            call(get(path), admin()).andExpect(status().isOk()).andExpect(jsonPath("$.data.assignmentStatus").value("IN_PROGRESS"));
            drive(assignmentId, operatorX, "complete");
            call(get(path), partnerA()).andExpect(status().isOk()).andExpect(jsonPath("$.data.assignmentStatus").value("COMPLETED"));
            call(get(path), partnerB()).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("After the operator declines, there is no current assignment until the booking is reassigned")
        void partnerViewAfterRejection() throws Exception {
            Booking booking = confirmedA();
            Long assignmentId = assignAsPartnerA(booking, operatorX);
            drive(assignmentId, operatorX, "reject");
            call(get("/api/bookings/" + booking.getId() + "/operator"), partnerA()).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").doesNotExist());
            Long second = assignAsPartnerA(booking, operatorY);
            call(get("/api/bookings/" + booking.getId() + "/operator"), partnerA()).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assignmentId").value(second))
                    .andExpect(jsonPath("$.data.operatorId").value(operatorY.getId()));
        }
    }

    // =============================================================================================
    // 3. Job state machine and side effects
    // =============================================================================================

    @Nested
    @DisplayName("2b. Completion recomputes the equipment flag (availability policy, Lead integration of B + C)")
    class CompletionAvailability {

        private Long runToCompletion(Booking booking) throws Exception {
            Long id = assignAsPartnerA(booking, operatorX);
            drive(id, operatorX, "accept", "start-travel", "reached", "start-work", "complete");
            return id;
        }

        @Test
        @DisplayName("Another live booking on the machine keeps it BOOKED after one job completes")
        void otherReservationKeepsBooked() throws Exception {
            Equipment machine = equipmentRepository.save(equipment("Shared Tractor", partnerA));
            Booking done = booking(farmer1, partnerA, machine, BookingStatus.CONFIRMED);
            booking(farmer2, partnerA, machine, BookingStatus.PENDING);   // other dates, still reserving
            tx.executeWithoutResult(s -> equipmentRepository.findById(machine.getId()).orElseThrow()
                    .setAvailabilityStatus(AvailabilityStatus.BOOKED));
            runToCompletion(done);
            assertThat(bookingStatus(done)).isEqualTo(BookingStatus.COMPLETED);
            assertThat(equipmentStatus(machine)).isEqualTo(AvailabilityStatus.BOOKED);
        }

        @Test
        @DisplayName("The machine's last reservation completing makes it AVAILABLE")
        void lastReservationFreesTheMachine() throws Exception {
            Equipment machine = equipmentRepository.save(equipment("Solo Tractor", partnerA));
            Booking done = booking(farmer1, partnerA, machine, BookingStatus.CONFIRMED);
            tx.executeWithoutResult(s -> equipmentRepository.findById(machine.getId()).orElseThrow()
                    .setAvailabilityStatus(AvailabilityStatus.BOOKED));
            runToCompletion(done);
            assertThat(equipmentStatus(machine)).isEqualTo(AvailabilityStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Completion never overrides a partner-owned UNDER_MAINTENANCE flag")
        void completionKeepsMaintenance() throws Exception {
            Equipment machine = equipmentRepository.save(equipment("Serviced Tractor", partnerA));
            Booking done = booking(farmer1, partnerA, machine, BookingStatus.CONFIRMED);
            Long id = assignAsPartnerA(done, operatorX);
            drive(id, operatorX, "accept", "start-travel", "reached", "start-work");
            tx.executeWithoutResult(s -> equipmentRepository.findById(machine.getId()).orElseThrow()
                    .setAvailabilityStatus(AvailabilityStatus.UNDER_MAINTENANCE));
            drive(id, operatorX, "complete");
            assertThat(equipmentStatus(machine)).isEqualTo(AvailabilityStatus.UNDER_MAINTENANCE);
        }
    }

    @Nested
    @DisplayName("3. Transitions and side effects")
    class Transitions {

        @Test
        @DisplayName("Full lifecycle with pauses: booking sync, equipment, tracking, intervals, dashboard, earnings, history, notifications")
        void fullLifecycle() throws Exception {
            Equipment ownEquipment = equipmentRepository.save(equipment("Lifecycle Tractor E", partnerA));
            Booking booking = booking(farmer1, partnerA, ownEquipment, BookingStatus.CONFIRMED);
            Long id = assignAsPartnerA(booking, operatorE);
            assertThat(equipmentStatus(ownEquipment)).as("assignment never writes the equipment flag")
                    .isEqualTo(AvailabilityStatus.AVAILABLE);

            call(get("/api/operators/dashboard/metrics"), op(operatorE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assignedJobs").value(1))
                    .andExpect(jsonPath("$.data.activeJob.assignmentId").value(id));

            act(id, "accept", operatorE).andExpect(status().isOk()).andExpect(jsonPath("$.data.assignmentStatus").value("ACCEPTED"));
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);

            act(id, "start-travel", operatorE).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assignmentStatus").value("TRAVELING"))
                    .andExpect(jsonPath("$.data.bookingStatus").value("ON_THE_WAY"));
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.ON_THE_WAY);

            // GPS tracking by the assigned operator while travelling; the farmer sees the real fix
            call(patch("/api/operators/jobs/" + id + "/location/start"), op(operatorE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.trackingActive").value(true));
            call(patch("/api/operators/jobs/" + id + "/location").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"latitude\":18.61,\"longitude\":73.91,\"accuracy\":5,\"speed\":20,\"heading\":90}"), op(operatorE))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.latitude").value(18.61));
            call(get("/api/farmers/bookings/" + booking.getId() + "/tracking"), farmer1()).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.latitude").value(18.61))
                    .andExpect(jsonPath("$.data.longitude").value(73.91))
                    .andExpect(jsonPath("$.data.status").value("ON_THE_WAY"));

            act(id, "reached", operatorE).andExpect(status().isOk()).andExpect(jsonPath("$.data.assignmentStatus").value("REACHED"));
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.ON_THE_WAY);

            act(id, "start-work", operatorE).andExpect(status().isOk()).andExpect(jsonPath("$.data.bookingStatus").value("WORK_STARTED"));
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.WORK_STARTED);

            drive(id, operatorE, "pause", "resume", "pause");
            assertThat(intervals(id)).hasSize(2).filteredOn(i -> i.getResumedAt() == null).hasSize(1);
            // Completion is only reachable from IN_PROGRESS: a paused job must be resumed first
            act(id, "complete", operatorE).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid job status transition from PAUSED to COMPLETED"));
            drive(id, operatorE, "resume");
            assertThat(intervals(id)).hasSize(2).allMatch(i -> i.getResumedAt() != null && i.getDurationMinutes() != null);

            // Make the billable time measurable: work started 180 min ago, pauses of 20 + 10 min
            LocalDateTime now = LocalDateTime.now();
            tx.executeWithoutResult(s -> {
                assignmentRepository.findById(id).orElseThrow().setWorkStartedAt(now.minusMinutes(180));
                List<OperatorJobPauseInterval> list = pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(id);
                list.get(0).setPausedAt(now.minusMinutes(120));
                list.get(0).setResumedAt(now.minusMinutes(100));
                list.get(0).setDurationMinutes(20L);
                list.get(1).setPausedAt(now.minusMinutes(60));
                list.get(1).setResumedAt(now.minusMinutes(50));
                list.get(1).setDurationMinutes(10L);
            });

            act(id, "complete", operatorE).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assignmentStatus").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.completionNotes").value("Field done"))
                    .andExpect(jsonPath("$.data.bookingStatus").value("COMPLETED"));

            // Booking / equipment / tracking side effects
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.COMPLETED);
            assertThat(equipmentStatus(ownEquipment)).isEqualTo(AvailabilityStatus.AVAILABLE);
            assertThat(locations(id)).isNotEmpty().noneMatch(OperatorLocation::isTrackingActive);

            // Earnings: 180 min elapsed - 30 min paused = 150 min at 600/h = 1500.00
            call(get("/api/operators/jobs/" + id + "/earnings"), op(operatorE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElapsedMinutes").value(180))
                    .andExpect(jsonPath("$.data.pausedMinutes").value(30))
                    .andExpect(jsonPath("$.data.netWorkMinutes").value(150))
                    .andExpect(jsonPath("$.data.grossEarnings").value(1500.00));
            call(get("/api/operators/earnings/summary"), op(operatorE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalCompletedJobs").value(1))
                    .andExpect(jsonPath("$.data.totalWorkMinutes").value(150))
                    .andExpect(jsonPath("$.data.totalPausedMinutes").value(30))
                    .andExpect(jsonPath("$.data.totalGrossEarnings").value(1500.00));
            call(get("/api/operators/earnings/history"), op(operatorE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].assignmentId").value(id))
                    .andExpect(jsonPath("$.data.content[0].grossEarnings").value(1500.00));

            // Dashboard and history reflect the completion
            call(get("/api/operators/dashboard/metrics"), op(operatorE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalJobs").value(1))
                    .andExpect(jsonPath("$.data.completedJobs").value(1))
                    .andExpect(jsonPath("$.data.assignedJobs").value(0))
                    .andExpect(jsonPath("$.data.completionRate").value(100.0))
                    .andExpect(jsonPath("$.data.acceptanceRate").value(100.0))
                    .andExpect(jsonPath("$.data.activeJob").doesNotExist());
            call(get("/api/operators/jobs/history"), op(operatorE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].assignmentId").value(id))
                    .andExpect(jsonPath("$.data.content[0].assignmentStatus").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.content[0].grossEarnings").value(1500.00))
                    .andExpect(jsonPath("$.data.content[0].hasGpsData").value(true));
            call(get("/api/operators/jobs/history/summary"), op(operatorE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.completedJobs").value(1))
                    .andExpect(jsonPath("$.data.totalPausedMinutes").value(30))
                    .andExpect(jsonPath("$.data.totalGrossEarnings").value(1500.00));

            // Notifications: farmer and partner are told about every step
            for (Long[] recipient : new Long[][]{{0L, farmer1.getId()}, {1L, partnerA.getId()}}) {
                String role = recipient[0] == 0L ? "FARMER" : "PARTNER";
                assertThat(notificationsFor(role, recipient[1], booking)).extracting(Notification::getTitle)
                        .contains("Job Accepted", "Operator En Route", "Operator Arrived", "Work In Progress",
                                "Work Paused", "Work Resumed", "Job Completed");
            }

            // Terminal: nothing moves any more
            for (String a : new String[]{"accept", "reject", "start-travel", "reached", "start-work", "pause", "resume", "complete"}) {
                act(id, a, operatorE).andExpect(status().isBadRequest());
            }
            assertThat(assignmentStatus(id)).isEqualTo(OperatorAssignmentStatus.COMPLETED);
            assertThat(operatorRepository.findById(operatorE.getId()).orElseThrow().isMobileVerified()).isFalse();
        }

        @Test
        @DisplayName("Out-of-order transitions are refused with a clear 400 and change nothing")
        void invalidTransitionsRefused() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            String[][] refusedFromAssigned = {{"start-travel", "TRAVELING"}, {"reached", "REACHED"}, {"start-work", "IN_PROGRESS"},
                    {"pause", "PAUSED"}, {"resume", "IN_PROGRESS"}, {"complete", "COMPLETED"}};
            for (String[] attempt : refusedFromAssigned) {
                act(id, attempt[0], operatorX).andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Invalid job status transition from ASSIGNED to " + attempt[1]));
            }
            assertThat(assignmentStatus(id)).isEqualTo(OperatorAssignmentStatus.ASSIGNED);
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);

            drive(id, operatorX, "accept");
            act(id, "reject", operatorX).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid job status transition from ACCEPTED to REJECTED"));
            act(id, "start-work", operatorX).andExpect(status().isBadRequest());
            act(id, "complete", operatorX).andExpect(status().isBadRequest());

            drive(id, operatorX, "start-travel");
            act(id, "start-work", operatorX).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid job status transition from TRAVELING to IN_PROGRESS"));
            drive(id, operatorX, "reached");
            act(id, "pause", operatorX).andExpect(status().isBadRequest());
            act(id, "complete", operatorX).andExpect(status().isBadRequest());
            drive(id, operatorX, "start-work");
            act(id, "resume", operatorX).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid job status transition from IN_PROGRESS to IN_PROGRESS"));
            assertThat(assignmentStatus(id)).isEqualTo(OperatorAssignmentStatus.IN_PROGRESS);
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.WORK_STARTED);
            assertThat(intervals(id)).isEmpty();
        }

        @Test
        @DisplayName("Repeated (double-submitted) requests are refused safely; no duplicate side effects")
        void duplicateRequestsRefused() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            drive(id, operatorX, "accept");
            act(id, "accept", operatorX).andExpect(status().isBadRequest());
            drive(id, operatorX, "start-travel");
            act(id, "start-travel", operatorX).andExpect(status().isBadRequest());
            drive(id, operatorX, "reached", "start-work", "pause");
            act(id, "pause", operatorX).andExpect(status().isBadRequest());
            assertThat(intervals(id)).hasSize(1);
            drive(id, operatorX, "resume");
            act(id, "resume", operatorX).andExpect(status().isBadRequest());
            drive(id, operatorX, "complete");
            act(id, "complete", operatorX).andExpect(status().isBadRequest());
            assertThat(intervals(id)).hasSize(1);
            assertThat(notificationsFor("FARMER", farmer1.getId(), booking)).filteredOn(n -> "Job Completed".equals(n.getTitle())).hasSize(1);
        }

        @Test
        @DisplayName("Reject with reason: assignment REJECTED, booking back to CONFIRMED without operator, stakeholders told, reassignable")
        void rejectWithReason() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            act(id, "reject", operatorX).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assignmentStatus").value("REJECTED"))
                    .andExpect(jsonPath("$.data.rejectionReason").value("Tractor clutch is broken"));
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(bookingOperatorId(booking)).isNull();
            assertThat(notificationsFor("FARMER", farmer1.getId(), booking)).extracting(Notification::getTitle).contains("Job Declined");
            assertThat(notificationsFor("PARTNER", partnerA.getId(), booking)).extracting(Notification::getTitle).contains("Job Declined");

            for (String a : new String[]{"accept", "reject", "start-travel"}) {
                act(id, a, operatorX).andExpect(status().isBadRequest());
            }
            Long second = assignAsPartnerA(booking, operatorY);
            drive(second, operatorY, "accept");
            assertThat(assignmentStatusesFor(booking)).containsExactly(OperatorAssignmentStatus.REJECTED, OperatorAssignmentStatus.ACCEPTED);
            assertThat(bookingOperatorId(booking)).isEqualTo(operatorY.getId());
            // The declined job stays in X's own history (and job list), never in Y's
            assertThat(JsonPath.<List<Integer>>read(body(call(get("/api/operators/jobs/history").param("status", "REJECTED")
                    .param("size", "200"), op(operatorX)).andExpect(status().isOk())), "$.data.content[*].assignmentId"))
                    .contains(id.intValue());
            assertThat(JsonPath.<List<Integer>>read(body(call(get("/api/operators/jobs/assigned").param("size", "200"), op(operatorX))),
                    "$.data.content[*].assignmentId")).contains(id.intValue()).doesNotContain(second.intValue());
            assertThat(JsonPath.<List<Integer>>read(body(call(get("/api/operators/jobs/assigned").param("size", "200"), op(operatorY))),
                    "$.data.content[*].assignmentId")).contains(second.intValue()).doesNotContain(id.intValue());
        }

        @Test
        @DisplayName("A maximum-length (500 char) reject or pause reason never breaks the step; notifications quote at most 300 chars and stay <= 500")
        void longReasonsDoNotBreakSteps() throws Exception {
            String longReason = "R".repeat(250) + " " + "S".repeat(249);
            assertThat(longReason).hasSize(500);

            Booking declinedBooking = confirmedA();
            Long declined = assignAsPartnerA(declinedBooking, operatorX);
            call(patch("/api/operators/jobs/" + declined + "/reject").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"rejectionReason\":\"" + longReason + "\"}"), op(operatorX))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.rejectionReason").value(longReason));
            assertThat(bookingStatus(declinedBooking)).isEqualTo(BookingStatus.CONFIRMED);
            for (String[] recipient : new String[][]{{"FARMER", String.valueOf(farmer1.getId())}, {"PARTNER", String.valueOf(partnerA.getId())}}) {
                assertThat(notificationsFor(recipient[0], Long.valueOf(recipient[1]), declinedBooking))
                        .filteredOn(n -> "Job Declined".equals(n.getTitle())).singleElement()
                        .satisfies(n -> assertThat(n.getMessage()).hasSizeLessThanOrEqualTo(500)
                                .contains("R".repeat(250)).doesNotContain(longReason));
            }

            Booking pausedBooking = confirmedA();
            Long paused = assignAsPartnerA(pausedBooking, operatorX);
            drive(paused, operatorX, "accept", "start-travel", "reached", "start-work");
            call(patch("/api/operators/jobs/" + paused + "/pause").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"pauseReason\":\"" + longReason + "\"}"), op(operatorX))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pauseReason").value(longReason));
            assertThat(intervals(paused)).singleElement().satisfies(i -> assertThat(i.getPauseReason()).isEqualTo(longReason));
            assertThat(notificationsFor("FARMER", farmer1.getId(), pausedBooking))
                    .filteredOn(n -> "Work Paused".equals(n.getTitle())).singleElement()
                    .satisfies(n -> assertThat(n.getMessage()).hasSizeLessThanOrEqualTo(500).contains("R".repeat(250)));
            drive(paused, operatorX, "resume", "complete");
        }

        @Test
        @DisplayName("Reject without a valid reason is a 400 and changes nothing")
        void rejectNeedsReason() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            for (String content : new String[]{"{}", "{\"rejectionReason\":\"  \"}", "{\"rejectionReason\":\"no\"}"}) {
                call(patch("/api/operators/jobs/" + id + "/reject").contentType(MediaType.APPLICATION_JSON).content(content), op(operatorX))
                        .andExpect(status().isBadRequest());
            }
            call(patch("/api/operators/jobs/" + id + "/reject").contentType(MediaType.APPLICATION_JSON), op(operatorX))
                    .andExpect(status().isBadRequest());
            assertThat(assignmentStatus(id)).isEqualTo(OperatorAssignmentStatus.ASSIGNED);
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        }

        @Test
        @DisplayName("Pending / rejected / inactive operators cannot act (filter refuses the token; service refuses too)")
        void ineligibleOperatorsCannotAct() throws Exception {
            for (Operator blocked : new Operator[]{operatorPending, operatorRejected, operatorInactive}) {
                Booking booking = confirmedA();
                // Simulate an assignment made before the operator lost eligibility
                OperatorJobAssignment assignment = tx.execute(s -> {
                    Booking b = bookingRepository.findById(booking.getId()).orElseThrow();
                    Operator o = operatorRepository.findById(blocked.getId()).orElseThrow();
                    b.setOperator(o);
                    b.setStatus(BookingStatus.OPERATOR_ASSIGNED);
                    return assignmentRepository.save(OperatorJobAssignment.builder().operator(o).booking(b)
                            .assignmentStatus(OperatorAssignmentStatus.ASSIGNED).assignedAt(LocalDateTime.now()).assignedBy("test").build());
                });
                act(assignment.getId(), "accept", blocked).andExpect(status().is4xxClientError());
                call(patch("/api/operators/jobs/" + assignment.getId() + "/location/start"), op(blocked)).andExpect(status().is4xxClientError());
                assertThatThrownBy(() -> lifecycleService.acceptJob(assignment.getId(), blocked.getId())).isInstanceOf(ForbiddenException.class);
                assertThatThrownBy(() -> lifecycleService.rejectJob(assignment.getId(), blocked.getId(),
                        OperatorJobRejectionRequest.builder().rejectionReason("cannot go").build())).isInstanceOf(ForbiddenException.class);
                assertThat(assignmentStatus(assignment.getId())).isEqualTo(OperatorAssignmentStatus.ASSIGNED);
            }
        }

        @Test
        @DisplayName("A booking closed elsewhere (completed / cancelled / rejected) can no longer be driven by the job, and is never moved backwards")
        void bookingClosedElsewhereFreezesTheJob() throws Exception {
            for (BookingStatus closed : new BookingStatus[]{BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.REJECTED}) {
                Booking booking = confirmedA();
                Long id = assignAsPartnerA(booking, operatorX);
                setBookingStatusOutOfBand(booking, closed);
                act(id, "reject", operatorX).andExpect(status().isBadRequest());
                act(id, "accept", operatorX).andExpect(status().isBadRequest());
                assertThat(bookingStatus(booking)).isEqualTo(closed);
                assertThat(assignmentStatus(id)).isEqualTo(OperatorAssignmentStatus.ASSIGNED);
            }

            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            drive(id, operatorX, "accept");
            setBookingStatusOutOfBand(booking, BookingStatus.COMPLETED);
            act(id, "start-travel", operatorX).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Booking #" + booking.getId() + " is COMPLETED; this job can no longer be updated."));
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.COMPLETED);
            assertThat(assignmentStatus(id)).isEqualTo(OperatorAssignmentStatus.ACCEPTED);
        }

        @Test
        @DisplayName("A booking re-pointed to another operator can no longer be driven by the old assignment")
        void bookingRepointedFreezesTheOldJob() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            tx.executeWithoutResult(s -> bookingRepository.findById(booking.getId()).orElseThrow()
                    .setOperator(operatorRepository.findById(operatorY.getId()).orElseThrow()));
            act(id, "accept", operatorX).andExpect(status().isBadRequest());
            act(id, "reject", operatorX).andExpect(status().isBadRequest());
            assertThat(bookingOperatorId(booking)).isEqualTo(operatorY.getId());
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        }

        @Test
        @DisplayName("A booking moved ahead by an admin is never moved backwards by a later operator step")
        void bookingNeverMovesBackwards() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            drive(id, operatorX, "accept");
            setBookingStatusOutOfBand(booking, BookingStatus.WORK_STARTED);
            drive(id, operatorX, "start-travel");
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.WORK_STARTED);
            drive(id, operatorX, "reached", "start-work", "complete");
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.COMPLETED);
        }
    }

    // =============================================================================================
    // 4. Location tracking
    // =============================================================================================

    @Nested
    @DisplayName("4. Location tracking")
    class Tracking {

        private ResultActions start(Long id, Operator operator) throws Exception {
            return call(patch("/api/operators/jobs/" + id + "/location/start"), op(operator));
        }

        private ResultActions update(Long id, Operator operator, double lat, double lng) throws Exception {
            return call(patch("/api/operators/jobs/" + id + "/location").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"latitude\":" + lat + ",\"longitude\":" + lng + "}"), op(operator));
        }

        @Test
        @DisplayName("Tracking only while travelling/working: refused before travel and after completion; stopped on completion")
        void trackingWindow() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            start(id, operatorX).andExpect(status().isBadRequest());
            update(id, operatorX, 18.7, 73.7).andExpect(status().isBadRequest());
            drive(id, operatorX, "accept");
            start(id, operatorX).andExpect(status().isBadRequest());
            update(id, operatorX, 18.7, 73.7).andExpect(status().isBadRequest());
            assertThat(locations(id)).isEmpty();

            drive(id, operatorX, "start-travel");
            start(id, operatorX).andExpect(status().isOk());
            update(id, operatorX, 18.7, 73.7).andExpect(status().isOk());
            update(id, operatorX, 91.0, 73.7).andExpect(status().isBadRequest());
            drive(id, operatorX, "reached", "start-work", "pause");
            update(id, operatorX, 18.71, 73.71).andExpect(status().isOk());
            drive(id, operatorX, "resume", "complete");
            assertThat(locations(id)).isNotEmpty().noneMatch(OperatorLocation::isTrackingActive);
            update(id, operatorX, 18.72, 73.72).andExpect(status().isBadRequest());
            start(id, operatorX).andExpect(status().isBadRequest());
            call(get("/api/operators/jobs/" + id + "/location"), op(operatorX)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.latitude").value(18.71))
                    .andExpect(jsonPath("$.data.trackingActive").value(false));
        }

        @Test
        @DisplayName("Starting tracking before any GPS fix never invents a location (no 0,0 point for the farmer)")
        void startWithoutFixInventsNothing() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            drive(id, operatorX, "accept", "start-travel");

            start(id, operatorX).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.trackingActive").value(true))
                    .andExpect(jsonPath("$.data.latitude").doesNotExist())
                    .andExpect(jsonPath("$.data.longitude").doesNotExist());
            assertThat(locations(id)).isEmpty();
            call(get("/api/operators/jobs/" + id + "/location"), op(operatorX)).andExpect(status().isNotFound());
            String farmerView = body(call(get("/api/farmers/bookings/" + booking.getId() + "/tracking"), farmer1())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.trackingAvailable").value(false))
                    .andExpect(jsonPath("$.data.locationSource").value("EQUIPMENT_BASE")));
            assertThat((Double) JsonPath.read(farmerView, "$.data.latitude")).isNotEqualTo(0.0);

            update(id, operatorX, 18.81, 73.81).andExpect(status().isOk());
            call(get("/api/farmers/bookings/" + booking.getId() + "/tracking"), farmer1()).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.latitude").value(18.81));
        }

        @Test
        @DisplayName("Restarting tracking keeps the last real fix and its real timestamp (no stale point presented as fresh)")
        void restartKeepsRealTimestamp() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            drive(id, operatorX, "accept", "start-travel");
            start(id, operatorX).andExpect(status().isOk());
            update(id, operatorX, 18.91, 73.91).andExpect(status().isOk());
            // recorded_at is DATETIME(6)/TIMESTAMP(6): the database keeps microseconds (rounding the rest), while
            // LocalDateTime.now() can carry nanoseconds on Linux. Use a value the column can hold exactly.
            LocalDateTime fixTime = LocalDateTime.now().minusMinutes(45).truncatedTo(ChronoUnit.MICROS);
            tx.executeWithoutResult(s -> locationRepository.findTopByAssignmentIdOrderByRecordedAtDesc(id).orElseThrow().setRecordedAt(fixTime));
            call(patch("/api/operators/jobs/" + id + "/location/stop"), op(operatorX)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.trackingActive").value(false));

            start(id, operatorX).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.trackingActive").value(true))
                    .andExpect(jsonPath("$.data.latitude").value(18.91));
            OperatorLocation latest = inTx(() -> locationRepository.findTopByAssignmentIdOrderByRecordedAtDesc(id).orElseThrow());
            assertThat(latest.getRecordedAt()).isEqualTo(fixTime);
            assertThat(latest.isTrackingActive()).isTrue();
            assertThat(locations(id)).hasSize(1);
        }

        @Test
        @DisplayName("Farmer tracking keeps working after the first operator declined and the booking was reassigned")
        void farmerTrackingAfterReassignment() throws Exception {
            Booking booking = confirmedA();
            Long first = assignAsPartnerA(booking, operatorX);
            drive(first, operatorX, "reject");
            Long second = assignAsPartnerA(booking, operatorY);
            drive(second, operatorY, "accept", "start-travel");
            update(second, operatorY, 18.55, 73.55).andExpect(status().isOk());

            call(get("/api/farmers/bookings/" + booking.getId() + "/tracking"), farmer1()).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.operatorId").value(operatorY.getId()))
                    .andExpect(jsonPath("$.data.latitude").value(18.55))
                    .andExpect(jsonPath("$.data.status").value("ON_THE_WAY"));
        }
    }

    // =============================================================================================
    // 4b. Farmer live tracking never fabricates data
    // =============================================================================================

    @Nested
    @DisplayName("4b. GET /api/farmers/bookings/{id}/tracking never fabricates data")
    class FarmerTrackingTruthfulness {

        private String track(Booking booking) throws Exception {
            return body(call(get("/api/farmers/bookings/" + booking.getId() + "/tracking"), farmer1()).andExpect(status().isOk()));
        }

        private Object field(String json, String path) {
            try {
                return JsonPath.read(json, path);
            } catch (com.jayway.jsonpath.PathNotFoundException missing) {
                return null;
            }
        }

        /** No position, timestamp, ETA, route or progress at all, and none of the old simulated values. */
        private void assertNothingInvented(String json) {
            for (String path : new String[]{"$.data.latitude", "$.data.longitude", "$.data.eta", "$.data.routeInformation",
                    "$.data.workProgress", "$.data.lastUpdated", "$.data.locationSource"}) {
                assertThat(field(json, path)).as(path).isNull();
            }
            assertThat((Boolean) field(json, "$.data.trackingAvailable")).isFalse();
            assertThat(json).doesNotContain("25 mins").doesNotContain("NH-48").doesNotContain("En route to registered");
        }

        @Test
        @DisplayName("Not yet in the field or closed: 200 with an explanation and no position/ETA/route/progress")
        void notInFieldNothingInvented() throws Exception {
            for (BookingStatus status : new BookingStatus[]{BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.CONFIRMED,
                    BookingStatus.CANCELLED, BookingStatus.REJECTED}) {
                String json = track(booking(farmer1, partnerA, equipmentA, status));
                assertNothingInvented(json);
                assertThat((String) field(json, "$.data.status")).isEqualTo(status.name());
                assertThat((String) field(json, "$.data.message")).isNotBlank();
            }
        }

        @Test
        @DisplayName("A cancelled job no longer exposes the operator's contact or any position")
        void cancelledJobHidesOperator() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorY);
            drive(id, operatorY, "accept", "start-travel");
            call(patch("/api/operators/jobs/" + id + "/location").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"latitude\":18.44,\"longitude\":73.44}"), op(operatorY)).andExpect(status().isOk());
            // However the booking got cancelled (farmer, admin), the tracking view follows the booking status
            setBookingStatusOutOfBand(booking, BookingStatus.CANCELLED);

            String json = track(booking);
            assertNothingInvented(json);
            assertThat(json).doesNotContain(operatorY.getMobileNumber()).doesNotContain("18.44");
            assertThat((String) field(json, "$.data.status")).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("Assigned but no position recorded yet: the equipment's registered location, labelled, never live, no timestamp")
        void inFieldWithoutPositionShowsLabelledEquipmentBase() throws Exception {
            Booking booking = confirmedA();
            assignAsPartnerA(booking, operatorX);
            String json = track(booking);
            assertThat((String) field(json, "$.data.locationSource")).isEqualTo("EQUIPMENT_BASE");
            assertThat((Boolean) field(json, "$.data.trackingAvailable")).isFalse();
            assertThat((Double) field(json, "$.data.latitude")).isEqualTo(18.52);
            assertThat((Double) field(json, "$.data.longitude")).isEqualTo(73.85);
            for (String path : new String[]{"$.data.lastUpdated", "$.data.eta", "$.data.routeInformation", "$.data.workProgress"}) {
                assertThat(field(json, path)).as(path).isNull();
            }
            assertThat((String) field(json, "$.data.operatorName")).isEqualTo(operatorX.getFullName());
            assertThat((String) field(json, "$.data.message")).contains("No live position");
        }

        @Test
        @DisplayName("A real GPS fix is live; after completion: progress 100, last real position, nothing live")
        void gpsThenCompletion() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            drive(id, operatorX, "accept", "start-travel");
            call(patch("/api/operators/jobs/" + id + "/location").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"latitude\":18.66,\"longitude\":73.66}"), op(operatorX)).andExpect(status().isOk());

            String live = track(booking);
            assertThat((String) field(live, "$.data.locationSource")).isEqualTo("OPERATOR_GPS");
            assertThat((Boolean) field(live, "$.data.trackingAvailable")).isTrue();
            assertThat((Double) field(live, "$.data.latitude")).isEqualTo(18.66);
            assertThat(field(live, "$.data.lastUpdated")).isNotNull();
            assertThat(field(live, "$.data.workProgress")).isNull();

            drive(id, operatorX, "reached", "start-work", "complete");
            String done = track(booking);
            assertThat((String) field(done, "$.data.status")).isEqualTo("COMPLETED");
            assertThat((Integer) field(done, "$.data.workProgress")).isEqualTo(100);
            assertThat((Boolean) field(done, "$.data.trackingAvailable")).isFalse();
            assertThat((Double) field(done, "$.data.latitude")).isEqualTo(18.66);
            assertThat(field(done, "$.data.eta")).isNull();
        }

        @Test
        @DisplayName("Without GPS, the stored tracking record is used as recorded (no invented values)")
        void trackingRecordUsedWhenNoGps() throws Exception {
            Booking booking = confirmedA();
            assignAsPartnerA(booking, operatorX);
            call(put("/api/farmers/bookings/" + booking.getId() + "/tracking").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"latitude\":18.77,\"longitude\":73.77,\"eta\":\"15 mins\",\"workProgress\":5}"), farmer1())
                    .andExpect(status().isOk());
            String json = track(booking);
            assertThat((String) field(json, "$.data.locationSource")).isEqualTo("TRACKING_RECORD");
            assertThat((Boolean) field(json, "$.data.trackingAvailable")).isTrue();
            assertThat((Double) field(json, "$.data.latitude")).isEqualTo(18.77);
            assertThat((String) field(json, "$.data.eta")).isEqualTo("15 mins");
            assertThat((Integer) field(json, "$.data.workProgress")).isEqualTo(5);
        }

        @Test
        @DisplayName("Ownership checks unchanged: other roles and other farmers are refused before any data is read")
        void ownershipUnchanged() throws Exception {
            Booking booking = confirmedA();
            assignAsPartnerA(booking, operatorX);
            String path = "/api/farmers/bookings/" + booking.getId() + "/tracking";
            call(get(path), null).andExpect(status().isUnauthorized());
            for (String authorization : new String[]{partnerA(), op(operatorX), admin(),
                    token(farmer2.getId(), "FARMER", AccountType.FARMER)}) {
                String json = body(call(get(path), authorization).andExpect(status().isForbidden()));
                assertThat(json).doesNotContain(operatorX.getMobileNumber()).doesNotContain("18.52");
            }
        }
    }

    // =============================================================================================
    // 5. Isolation
    // =============================================================================================

    @Nested
    @DisplayName("5. Isolation between operators and roles")
    class Isolation {

        @Test
        @DisplayName("Operator Y can neither read nor change operator X's job, location or earnings; errors leak nothing")
        void otherOperatorRefused() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            drive(id, operatorX, "accept", "start-travel");
            call(patch("/api/operators/jobs/" + id + "/location").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"latitude\":18.33,\"longitude\":73.33}"), op(operatorX)).andExpect(status().isOk());

            List<MockHttpServletRequestBuilder> attempts = new ArrayList<>();
            attempts.add(get("/api/operators/jobs/" + id));
            attempts.add(get("/api/operators/jobs/" + id + "/earnings"));
            attempts.add(get("/api/operators/jobs/" + id + "/location"));
            attempts.add(patch("/api/operators/jobs/" + id + "/location/start"));
            attempts.add(patch("/api/operators/jobs/" + id + "/location/stop"));
            attempts.add(patch("/api/operators/jobs/" + id + "/location").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"latitude\":1.0,\"longitude\":1.0}"));
            for (String a : new String[]{"accept", "reject", "start-travel", "reached", "start-work", "pause", "resume", "complete"}) {
                attempts.add(action(id, a));
            }
            for (MockHttpServletRequestBuilder attempt : attempts) {
                String json = body(call(attempt, op(operatorY)).andExpect(status().isForbidden()));
                assertThat(json).doesNotContain(DELIVERY_PREFIX).doesNotContain(operatorX.getFullName())
                        .doesNotContain("18.33").doesNotContain("\"data\":{");
            }
            assertThat(assignmentStatus(id)).isEqualTo(OperatorAssignmentStatus.TRAVELING);
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.ON_THE_WAY);
            assertThat(locations(id)).hasSize(1).allMatch(l -> l.getLatitude() == 18.33 && l.isTrackingActive());

            assertThat(JsonPath.<List<Integer>>read(body(call(get("/api/operators/jobs/assigned").param("size", "200"), op(operatorY))),
                    "$.data.content[*].assignmentId")).doesNotContain(id.intValue());
            assertThat(JsonPath.<List<Integer>>read(body(call(get("/api/operators/jobs/history").param("size", "200"), op(operatorY))),
                    "$.data.content[*].assignmentId")).doesNotContain(id.intValue());
        }

        @Test
        @DisplayName("Farmer, partner and admin cannot drive operator-only endpoints; anonymous is unauthenticated")
        void otherRolesRefused() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            for (String authorization : new String[]{farmer1(), partnerA(), admin()}) {
                for (String a : new String[]{"accept", "reject"}) {
                    call(action(id, a), authorization).andExpect(status().isForbidden());
                }
                call(get("/api/operators/jobs/" + id), authorization).andExpect(status().isForbidden());
                call(patch("/api/operators/jobs/" + id + "/location/start"), authorization).andExpect(status().isForbidden());
                call(get("/api/operators/dashboard/metrics"), authorization).andExpect(status().isForbidden());
                call(get("/api/operators/earnings/summary"), authorization).andExpect(status().isForbidden());
            }
            call(action(id, "accept"), null).andExpect(status().isUnauthorized());
            assertThat(assignmentStatus(id)).isEqualTo(OperatorAssignmentStatus.ASSIGNED);
        }

        @Test
        @DisplayName("Unknown assignment IDs are a plain 404")
        void unknownAssignment() throws Exception {
            call(action(987654321L, "accept"), op(operatorX)).andExpect(status().isNotFound());
            call(get("/api/operators/jobs/987654321"), op(operatorX)).andExpect(status().isNotFound());
        }
    }

    // =============================================================================================
    // 6. Concurrency (real threads through the full HTTP stack)
    // =============================================================================================

    @Nested
    @DisplayName("6. Concurrent conflicting requests")
    class Concurrency {

        /** Fires all calls at once and returns their HTTP statuses, in call order. */
        private List<Integer> race(List<Callable<Integer>> calls) throws Exception {
            ExecutorService pool = Executors.newFixedThreadPool(calls.size());
            try {
                CountDownLatch ready = new CountDownLatch(calls.size());
                CountDownLatch go = new CountDownLatch(1);
                List<Future<Integer>> futures = new ArrayList<>();
                for (Callable<Integer> c : calls) {
                    futures.add(pool.submit(() -> {
                        clearSecurityContext();
                        ready.countDown();
                        go.await();
                        return c.call();
                    }));
                }
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                go.countDown();
                List<Integer> statuses = new ArrayList<>();
                for (Future<Integer> f : futures) {
                    statuses.add(f.get(60, TimeUnit.SECONDS));
                }
                return statuses;
            } finally {
                pool.shutdownNow();
            }
        }

        private Callable<Integer> request(MockHttpServletRequestBuilder builder, String authorization) {
            return () -> call(builder, authorization).andReturn().getResponse().getStatus();
        }

        private void assertOneWinner(List<Integer> statuses, int winnerStatus) {
            assertThat(statuses).filteredOn(s -> s == winnerStatus).hasSize(1);
            assertThat(statuses).filteredOn(s -> s != winnerStatus).allMatch(s -> s == 400 || s == 409,
                    "losers get a clean 400/409, never a 5xx: " + statuses);
        }

        @Test
        @DisplayName("Accept vs reject on the same job: exactly one wins and booking/assignment stay consistent")
        void acceptVersusReject() throws Exception {
            for (int round = 0; round < CONCURRENCY_ROUNDS; round++) {
                Booking booking = confirmedA();
                Long id = assignAsPartnerA(booking, operatorZ);
                List<Integer> statuses = race(List.of(request(action(id, "accept"), op(operatorZ)),
                        request(action(id, "reject"), op(operatorZ))));
                assertOneWinner(statuses, 200);
                OperatorAssignmentStatus finalStatus = assignmentStatus(id);
                if (finalStatus == OperatorAssignmentStatus.ACCEPTED) {
                    assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
                    assertThat(bookingOperatorId(booking)).isEqualTo(operatorZ.getId());
                } else {
                    assertThat(finalStatus).isEqualTo(OperatorAssignmentStatus.REJECTED);
                    assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.CONFIRMED);
                    assertThat(bookingOperatorId(booking)).isNull();
                }
            }
        }

        @Test
        @DisplayName("Double-submitted pause: exactly one pause and one open interval")
        void duplicatePause() throws Exception {
            for (int round = 0; round < CONCURRENCY_ROUNDS; round++) {
                Booking booking = confirmedA();
                Long id = assignAsPartnerA(booking, operatorZ);
                drive(id, operatorZ, "accept", "start-travel", "reached", "start-work");
                List<Integer> statuses = race(List.of(request(action(id, "pause"), op(operatorZ)),
                        request(action(id, "pause"), op(operatorZ)), request(action(id, "pause"), op(operatorZ))));
                assertOneWinner(statuses, 200);
                assertThat(assignmentStatus(id)).isEqualTo(OperatorAssignmentStatus.PAUSED);
                assertThat(intervals(id)).hasSize(1).allMatch(i -> i.getResumedAt() == null);
                drive(id, operatorZ, "resume", "complete");
            }
        }

        @Test
        @DisplayName("Complete vs pause: one wins; either COMPLETED with no open pause, or PAUSED with exactly one")
        void completeVersusPause() throws Exception {
            for (int round = 0; round < CONCURRENCY_ROUNDS; round++) {
                Booking booking = confirmedA();
                Long id = assignAsPartnerA(booking, operatorZ);
                drive(id, operatorZ, "accept", "start-travel", "reached", "start-work");
                List<Integer> statuses = race(List.of(request(action(id, "complete"), op(operatorZ)),
                        request(action(id, "pause"), op(operatorZ))));
                assertOneWinner(statuses, 200);
                if (assignmentStatus(id) == OperatorAssignmentStatus.COMPLETED) {
                    assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.COMPLETED);
                    assertThat(intervals(id)).isEmpty();
                } else {
                    assertThat(assignmentStatus(id)).isEqualTo(OperatorAssignmentStatus.PAUSED);
                    assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.WORK_STARTED);
                    assertThat(intervals(id)).hasSize(1);
                    drive(id, operatorZ, "resume", "complete");
                }
            }
        }

        @Test
        @DisplayName("Farmer cancellation racing an operator step: never a 5xx (no lock-order deadlock) and a consistent outcome")
        void cancellationVersusOperatorStep() throws Exception {
            String[][] steps = {
                    {"accept"},
                    {"reject"},
                    {"start-travel", "accept"},
                    {"start-work", "accept", "start-travel", "reached"},
                    {"complete", "accept", "start-travel", "reached", "start-work"}};
            List<String> outcomes = new ArrayList<>();
            for (int round = 0; round < 3; round++) {
                for (String[] step : steps) {
                    Booking booking = confirmedA();
                    Long id = assignAsPartnerA(booking, operatorZ);
                    for (int i = 1; i < step.length; i++) {
                        drive(id, operatorZ, step[i]);
                    }
                    List<Integer> statuses = race(List.of(
                            request(patch("/api/bookings/" + booking.getId() + "/cancel"), farmer1()),
                            request(action(id, step[0]), op(operatorZ))));
                    outcomes.add(step[0] + statuses);
                    assertThat(statuses).as("no 5xx for %s: %s", step[0], statuses).allMatch(s -> s < 500);

                    BookingStatus bookingNow = bookingStatus(booking);
                    OperatorAssignmentStatus jobNow = assignmentStatus(id);
                    if (bookingNow == BookingStatus.CANCELLED) {
                        assertThat(statuses.get(0)).isEqualTo(200);
                        assertThat(jobNow).as("a cancelled booking never keeps an active job")
                                .isIn(OperatorAssignmentStatus.CANCELLED, OperatorAssignmentStatus.REJECTED);
                    } else {
                        assertThat(statuses.get(1)).as("the operator step won").isEqualTo(200);
                        assertThat(statuses.get(0)).as("the cancellation lost cleanly").isIn(400, 409);
                        BookingStatus expectedBooking = switch (step[0]) {
                            case "accept" -> BookingStatus.OPERATOR_ASSIGNED;
                            case "reject" -> BookingStatus.CONFIRMED;
                            case "start-travel" -> BookingStatus.ON_THE_WAY;
                            case "start-work" -> BookingStatus.WORK_STARTED;
                            default -> BookingStatus.COMPLETED;
                        };
                        assertThat(bookingNow).isEqualTo(expectedBooking);
                    }
                }
            }
            System.out.println("cancellationVersusOperatorStep outcomes [cancel, operator]: " + outcomes);
        }

        @Test
        @DisplayName("Partner and admin assign different operators to the same booking at once: exactly one assignment")
        void concurrentAssignmentSameBooking() throws Exception {
            for (int round = 0; round < CONCURRENCY_ROUNDS; round++) {
                Booking booking = confirmedA();
                String bodyX = "{\"operatorId\":" + operatorX.getId() + "}";
                String bodyY = "{\"operatorId\":" + operatorY.getId() + "}";
                List<Integer> statuses = race(List.of(
                        request(post("/api/bookings/" + booking.getId() + "/operator").contentType(MediaType.APPLICATION_JSON).content(bodyX), partnerA()),
                        request(post("/api/bookings/" + booking.getId() + "/operator").contentType(MediaType.APPLICATION_JSON).content(bodyY), admin())));
                assertOneWinner(statuses, 201);
                assertThat(assignmentStatusesFor(booking)).containsExactly(OperatorAssignmentStatus.ASSIGNED);
                assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
            }
        }
    }

    // =============================================================================================
    // 7. BIZ-20 interaction (product finding evidence; policy itself is frozen)
    // =============================================================================================

    @Nested
    @DisplayName("7. BIZ-20: payment only for CONFIRMED bookings")
    class PaymentInteraction {

        @Test
        @DisplayName("Once an operator is assigned, an unpaid booking can no longer be paid; it becomes payable again only if the operator declines")
        void assignmentBlocksPayment() throws Exception {
            Booking booking = confirmedA();
            Long id = assignAsPartnerA(booking, operatorX);
            String payment = "{\"bookingId\":" + booking.getId() + ",\"farmerId\":" + farmer1.getId()
                    + ",\"amount\":7500.00,\"paymentMethod\":\"UPI\"}";
            call(post("/api/farmers/payments").contentType(MediaType.APPLICATION_JSON).content(payment), farmer1())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot process payment for a booking with status: OPERATOR_ASSIGNED."
                            + " Payment is accepted only for confirmed bookings."));
            drive(id, operatorX, "reject");
            assertThat(bookingStatus(booking)).isEqualTo(BookingStatus.CONFIRMED);
        }
    }
}
