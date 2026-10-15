package com.agrorental.security;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.complaint.entity.Complaint;
import com.agrorental.complaint.enums.ComplaintCategory;
import com.agrorental.complaint.enums.ComplaintStatus;
import com.agrorental.complaint.repository.ComplaintRepository;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.review.repository.ReviewRepository;
import com.agrorental.security.jwt.AccountType;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.tracking.entity.Tracking;
import com.agrorental.tracking.repository.TrackingRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BIZ-4..BIZ-10 ownership regression matrix against the real SecurityConfig, JwtAuthenticationFilter and
 * signed tokens. Farmer, partner, operator and admin #1 all share numeric ID 1 (and #2 likewise), so any
 * cross-table or path-ID trust shows up as a wrong-owner success.
 *
 * <p>For each endpoint: no authentication, wrong account type, right role but wrong owner, the owner,
 * admin (where authorized) and forged request IDs. Forbidden writes are checked for "no mutation";
 * forbidden reads are checked for "no sensitive data in the body".
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:p1_ownership_matrix_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=p1-ownership-test-secret-key-32-chars-minimum-hmac",
    "server.port=0"
})
@DisplayName("BIZ-4..BIZ-10: Ownership authorization matrix")
class OwnershipAuthorizationIntegrationTest {

    private static final String FARMER1_MOBILE = "9800000201";
    private static final String OPERATOR_X_MOBILE = "9700000201";

    @Autowired private WebApplicationContext context;
    @Autowired private JwtService jwtService;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private OperatorRepository operatorRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private TrackingRepository trackingRepository;

    private MockMvc mockMvc;

    private Partner partnerA;
    private Partner partnerB;
    private Farmer farmer1;
    private Farmer farmer2;
    private Operator operatorX;
    private Operator operatorY;
    private Admin admin;

    /** farmer1 / partnerA / operatorX, in progress, with a tracking row */
    private Booking bookingF1;
    /** farmer2 / partnerB / operatorY, in progress, with a tracking row */
    private Booking bookingF2;
    /** completed, farmer1 / partnerA (reviewable by farmer1 only) */
    private Booking completedF1;
    /** completed, farmer1 / partnerA (reviewed legitimately by its owner) */
    private Booking completedOwnerF1;
    /** confirmed, farmer1 / partnerA (admin payment target) */
    private Booking payableF1;
    /** confirmed, farmer2 / partnerB (payment target for the matrix) */
    private Booking payableF2;

    private Payment paymentF1;
    private Complaint complaintF1;
    private Tracking trackingF2;

    @BeforeAll
    void createFixtures() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        partnerA = partnerRepository.save(partner("Partner A", "9600000201"));
        partnerB = partnerRepository.save(partner("Partner B", "9600000202"));
        farmer1 = farmerRepository.save(farmer("Farmer One", FARMER1_MOBILE));
        farmer2 = farmerRepository.save(farmer("Farmer Two", "9800000202"));
        operatorX = operatorRepository.save(operator("Operator X", OPERATOR_X_MOBILE));
        operatorY = operatorRepository.save(operator("Operator Y", "9700000202"));
        admin = adminRepository.save(Admin.builder().fullName("Admin").email("admin@p1.test").password("x").build());

        Equipment equipmentA = equipmentRepository.save(equipment("Tractor A", partnerA));
        Equipment equipmentB = equipmentRepository.save(equipment("Tractor B", partnerB));
        bookingF1 = bookingRepository.save(booking(farmer1, partnerA, equipmentA, operatorX, BookingStatus.ON_THE_WAY, LocalDate.of(2026, 11, 10)));
        bookingF2 = bookingRepository.save(booking(farmer2, partnerB, equipmentB, operatorY, BookingStatus.ON_THE_WAY, LocalDate.of(2026, 11, 10)));
        completedF1 = bookingRepository.save(booking(farmer1, partnerA, equipmentA, null, BookingStatus.COMPLETED, LocalDate.of(2026, 9, 1)));
        completedOwnerF1 = bookingRepository.save(booking(farmer1, partnerA, equipmentA, null, BookingStatus.COMPLETED, LocalDate.of(2026, 8, 1)));
        payableF1 = bookingRepository.save(booking(farmer1, partnerA, equipmentA, null, BookingStatus.CONFIRMED, LocalDate.of(2027, 1, 5)));
        payableF2 = bookingRepository.save(booking(farmer2, partnerB, equipmentB, null, BookingStatus.CONFIRMED, LocalDate.of(2027, 2, 5)));

        paymentF1 = paymentRepository.save(Payment.builder().bookingId(bookingF1.getId()).farmerId(farmer1.getId())
                .partnerId(partnerA.getId()).amount(new BigDecimal("7500.00")).paymentMethod(PaymentMethod.UPI)
                .transactionId("TXN-P1-F1").paymentStatus(PaymentStatus.SUCCESS).paymentDate(LocalDateTime.now())
                .invoiceReference("INV-P1-F1").build());
        complaintF1 = complaintRepository.save(Complaint.builder().farmerId(farmer1.getId()).bookingId(bookingF1.getId())
                .category(ComplaintCategory.LATE_ARRIVAL).description("Private complaint text of farmer one")
                .status(ComplaintStatus.OPEN).build());
        trackingRepository.save(Tracking.builder().bookingId(bookingF1.getId()).operatorId(operatorX.getId())
                .latitude(18.11).longitude(73.11).eta("10 mins").workProgress(10).lastUpdated(LocalDateTime.now()).build());
        trackingF2 = trackingRepository.save(Tracking.builder().bookingId(bookingF2.getId()).operatorId(operatorY.getId())
                .latitude(18.22).longitude(73.22).eta("20 mins").workProgress(20).lastUpdated(LocalDateTime.now()).build());

        // Preconditions: overlapping numeric IDs across account tables
        assertThat(farmer1.getId()).isEqualTo(partnerA.getId()).isEqualTo(operatorX.getId()).isEqualTo(admin.getId());
        assertThat(farmer2.getId()).isEqualTo(partnerB.getId()).isEqualTo(operatorY.getId());
    }

    private static Partner partner(String name, String mobile) {
        return Partner.builder().fullName(name).businessName(name + " Rentals").mobileNumber(mobile).email(mobile + "@p1.test")
                .password("x").otpVerified(true).verificationStatus(Partner.VerificationStatus.APPROVED).build();
    }

    private static Farmer farmer(String name, String mobile) {
        return Farmer.builder().fullName(name).mobileNumber(mobile).email(mobile + "@p1.test").accountStatus("ACTIVE").build();
    }

    private static Operator operator(String name, String mobile) {
        Operator operator = new Operator();
        operator.setFullName(name);
        operator.setMobileNumber(mobile);
        operator.setEmail(mobile + "@p1.test");
        operator.setAddress("Pune");
        operator.setAadhaarNumber("123412341234");
        operator.setDrivingLicenseNumber("MH-12-0000-" + mobile.substring(6));
        operator.setExperience(3);
        operator.setSkills("Tractor");
        operator.setPassword("x");
        operator.setStatus(OperatorStatus.APPROVED);
        operator.setMobileVerified(true);
        return operator;
    }

    private static Equipment equipment(String name, Partner partner) {
        return Equipment.builder().name(name).category(EquipmentCategory.TRACTOR).brand("Mahindra").model("575 DI")
                .manufacturingYear(2023).capacity("47 HP").rentalPrice(new BigDecimal("2500.00")).fuelType(FuelType.DIESEL)
                .description("Tractor").locationAddress("Pune").latitude(18.52).longitude(73.85)
                .partner(partner).availabilityStatus(AvailabilityStatus.BOOKED).build();
    }

    private static Booking booking(Farmer farmer, Partner partner, Equipment equipment, Operator operator,
                                   BookingStatus status, LocalDate start) {
        return Booking.builder().farmerId(farmer.getId()).partner(partner).equipment(equipment).operator(operator)
                .status(status).startDate(start).endDate(start.plusDays(2))
                .totalCost(new BigDecimal("7500.00")).deliveryAddress("Farm of " + farmer.getFullName()).build();
    }

    // ---- tokens & helpers ----------------------------------------------------------------------

    private String token(Long id, String role, AccountType type) {
        return "Bearer " + jwtService.generateToken(id, role, "0000000000", JwtService.accountTypeClaims(type));
    }

    private String farmer1() { return token(farmer1.getId(), "FARMER", AccountType.FARMER); }
    private String farmer2() { return token(farmer2.getId(), "FARMER", AccountType.FARMER); }
    private String partnerA() { return token(partnerA.getId(), "PARTNER", AccountType.PARTNER); }
    private String partnerB() { return token(partnerB.getId(), "PARTNER", AccountType.PARTNER); }
    private String operatorX() { return token(operatorX.getId(), "OPERATOR", AccountType.OPERATOR); }
    private String operatorY() { return token(operatorY.getId(), "OPERATOR", AccountType.OPERATOR); }
    private String admin() { return token(admin.getId(), "ADMIN", AccountType.ADMIN); }

    private ResultActions call(MockHttpServletRequestBuilder request, String authorization) throws Exception {
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return mockMvc.perform(request);
    }

    /** Forbidden read: status plus proof that no owner data leaked into the body. */
    private void assertDenied(MockHttpServletRequestBuilder request, String authorization, int expectedStatus,
                              String... secrets) throws Exception {
        ResultActions result = call(request, authorization).andExpect(status().is(expectedStatus));
        String body = result.andReturn().getResponse().getContentAsString();
        for (String secret : secrets) {
            assertThat(body).as("denied response must not contain %s", secret).doesNotContain(secret);
        }
    }

    // ---- BIZ-4: tracking ----------------------------------------------------------------------

    @Nested
    @DisplayName("BIZ-4: Farmer tracking (read and write)")
    class TrackingAccess {

        private MockHttpServletRequestBuilder read(Booking booking) {
            return get("/api/farmers/bookings/" + booking.getId() + "/tracking");
        }

        private MockHttpServletRequestBuilder write(Booking booking) {
            return put("/api/farmers/bookings/" + booking.getId() + "/tracking").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"latitude\":1.0,\"longitude\":2.0,\"eta\":\"forged\",\"workProgress\":99}");
        }

        @Test
        @DisplayName("Read: anonymous 401; partner, operator and admin 403; other farmer 403 without operator data; owner 200")
        void readMatrix() throws Exception {
            assertDenied(read(bookingF1), null, 401, OPERATOR_X_MOBILE);
            assertDenied(read(bookingF1), partnerA(), 403, OPERATOR_X_MOBILE);
            assertDenied(read(bookingF1), operatorX(), 403, OPERATOR_X_MOBILE);
            assertDenied(read(bookingF1), admin(), 403, OPERATOR_X_MOBILE);
            assertDenied(read(bookingF1), farmer2(), 403, OPERATOR_X_MOBILE, "18.11");

            call(read(bookingF1), farmer1())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.operatorMobile").value(OPERATOR_X_MOBILE));
        }

        @Test
        @DisplayName("Write: anonymous, wrong type and other farmer cannot change another booking's tracking")
        void writeMatrix() throws Exception {
            // Snapshot at start: the owner may legitimately have updated this row in another test
            Tracking before = trackingRepository.findById(trackingF2.getId()).orElseThrow();
            call(write(bookingF2), null).andExpect(status().isUnauthorized());
            call(write(bookingF2), partnerB()).andExpect(status().isForbidden());
            call(write(bookingF2), operatorY()).andExpect(status().isForbidden());
            call(write(bookingF2), farmer1()).andExpect(status().isForbidden());

            Tracking current = trackingRepository.findById(trackingF2.getId()).orElseThrow();
            assertThat(current.getLatitude()).isEqualTo(before.getLatitude());
            assertThat(current.getEta()).isEqualTo(before.getEta());
            assertThat(current.getWorkProgress()).isEqualTo(before.getWorkProgress());
            assertThat(trackingRepository.findByBookingId(bookingF1.getId()).orElseThrow().getLatitude()).isEqualTo(18.11);
        }

        @Test
        @DisplayName("Write: the owning farmer can update its own booking's tracking")
        void ownerCanWrite() throws Exception {
            call(write(bookingF2), farmer2()).andExpect(status().isOk());
            assertThat(trackingRepository.findById(trackingF2.getId()).orElseThrow().getEta()).isEqualTo("forged");
        }
    }

    // ---- BIZ-5: booking details ----------------------------------------------------------------------

    @Nested
    @DisplayName("BIZ-5: Booking details GET /api/bookings/{id}")
    class BookingDetails {

        private MockHttpServletRequestBuilder read() {
            return get("/api/bookings/" + bookingF1.getId());
        }

        @Test
        @DisplayName("Anonymous 401; other farmer, other partner and unassigned operator 403 without owner data")
        void deniedCallers() throws Exception {
            assertDenied(read(), null, 401, FARMER1_MOBILE);
            assertDenied(read(), farmer2(), 403, FARMER1_MOBILE, "Farm of Farmer One");
            assertDenied(read(), partnerB(), 403, FARMER1_MOBILE, "Farm of Farmer One");
            assertDenied(read(), operatorY(), 403, FARMER1_MOBILE, "Farm of Farmer One");
        }

        @Test
        @DisplayName("Owning farmer, owning partner, assigned operator and admin can read it")
        void authorizedCallers() throws Exception {
            for (String authorization : new String[]{farmer1(), partnerA(), operatorX(), admin()}) {
                call(read(), authorization)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.id").value(bookingF1.getId()));
            }
        }
    }

    // ---- BIZ-6: operator booking list ----------------------------------------------------------------

    @Nested
    @DisplayName("BIZ-6: Operator booking list GET /api/bookings/operator/{operatorId}")
    class OperatorBookings {

        @Test
        @DisplayName("Anonymous 401; farmer, partner and admin 403; operator with another operator's ID 403; own ID 200")
        void matrix() throws Exception {
            String other = "/api/bookings/operator/" + operatorX.getId();
            assertDenied(get(other), null, 401, "Farm of Farmer One");
            assertDenied(get(other), farmer1(), 403, "Farm of Farmer One");
            assertDenied(get(other), partnerA(), 403, "Farm of Farmer One");
            assertDenied(get(other), admin(), 403, "Farm of Farmer One");
            assertDenied(get(other), operatorY(), 403, "Farm of Farmer One");

            call(get(other), operatorX())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].id").value(org.hamcrest.Matchers.hasItem(bookingF1.getId().intValue())));
        }
    }

    // ---- BIZ-7: farmer dashboard ----------------------------------------------------------------------

    @Nested
    @DisplayName("BIZ-7: Farmer dashboard GET /api/farmers/dashboard/{farmerId}")
    class Dashboard {

        @Test
        @DisplayName("Anonymous 401; partner/operator/admin 403; another farmer's ID 403; own ID 200")
        void matrix() throws Exception {
            String path = "/api/farmers/dashboard/" + farmer1.getId();
            assertDenied(get(path), null, 401, FARMER1_MOBILE);
            assertDenied(get(path), partnerA(), 403, FARMER1_MOBILE);
            assertDenied(get(path), operatorX(), 403, FARMER1_MOBILE);
            assertDenied(get(path), admin(), 403, FARMER1_MOBILE);
            assertDenied(get(path), farmer2(), 403, FARMER1_MOBILE, "Farmer One");

            call(get(path), farmer1()).andExpect(status().isOk());
        }
    }

    // ---- BIZ-8: complaints ----------------------------------------------------------------------

    @Nested
    @DisplayName("BIZ-8: Farmer complaint GET /api/farmers/complaints/{id}")
    class Complaints {

        @Test
        @DisplayName("Anonymous 401; partner/operator 403; another farmer 403 without complaint text; owner and admin route 200")
        void matrix() throws Exception {
            String path = "/api/farmers/complaints/" + complaintF1.getId();
            assertDenied(get(path), null, 401, "Private complaint text");
            assertDenied(get(path), partnerA(), 403, "Private complaint text");
            assertDenied(get(path), operatorX(), 403, "Private complaint text");
            assertDenied(get(path), farmer2(), 403, "Private complaint text", FARMER1_MOBILE);

            call(get(path), farmer1())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.description").value("Private complaint text of farmer one"));
            call(get("/api/admin/complaints/" + complaintF1.getId()), admin()).andExpect(status().isOk());
        }
    }

    // ---- BIZ-9: reviews ----------------------------------------------------------------------

    @Nested
    @DisplayName("BIZ-9: Review creation POST /api/reviews")
    class Reviews {

        private MockHttpServletRequestBuilder review(Booking booking, Long farmerId) {
            return post("/api/reviews").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"bookingId\":" + booking.getId() + ",\"farmerId\":" + farmerId
                            + ",\"rating\":1,\"comment\":\"forged review\"}");
        }

        @Test
        @DisplayName("Anonymous 401; partner/operator 403; impersonating another farmer 403; non-owner booking 400; nothing saved")
        void deniedCallersCreateNothing() throws Exception {
            call(review(completedF1, farmer1.getId()), null).andExpect(status().isUnauthorized());
            call(review(completedF1, farmer1.getId()), partnerA()).andExpect(status().isForbidden());
            call(review(completedF1, farmer1.getId()), operatorX()).andExpect(status().isForbidden());
            // farmer2 claims to be farmer1 in the body
            call(review(completedF1, farmer1.getId()), farmer2()).andExpect(status().isForbidden());
            // farmer2 as itself, on farmer1's booking
            call(review(completedF1, farmer2.getId()), farmer2()).andExpect(status().isBadRequest());

            assertThat(reviewRepository.existsByBookingId(completedF1.getId())).isFalse();
        }

        @Test
        @DisplayName("The owning farmer can review its completed booking; the review is recorded under that farmer")
        void ownerCanReview() throws Exception {
            call(review(completedOwnerF1, farmer1.getId()), farmer1()).andExpect(status().isCreated());
            assertThat(reviewRepository.findByBookingId(completedOwnerF1.getId()).orElseThrow().getFarmerId()).isEqualTo(farmer1.getId());
        }
    }

    // ---- BIZ-10: payments ----------------------------------------------------------------------

    @Nested
    @DisplayName("BIZ-10: Payments")
    class Payments {

        private MockHttpServletRequestBuilder create(Booking booking, Long claimedFarmerId) {
            return post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"bookingId\":" + booking.getId() + ",\"farmerId\":" + claimedFarmerId
                            + ",\"amount\":7500.00,\"paymentMethod\":\"UPI\"}");
        }

        @Test
        @DisplayName("Create: anonymous 401; partner/operator 403 even with the right farmerId; other farmer 403; nothing saved")
        void createDenied() throws Exception {
            call(create(bookingF2, farmer2.getId()), null).andExpect(status().isUnauthorized());
            call(create(bookingF2, farmer2.getId()), partnerB()).andExpect(status().isForbidden());
            call(create(bookingF2, farmer2.getId()), operatorY()).andExpect(status().isForbidden());
            call(create(bookingF2, farmer2.getId()), farmer1()).andExpect(status().isForbidden());
            call(post("/api/farmers/payments").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"bookingId\":" + bookingF2.getId() + ",\"farmerId\":" + farmer2.getId()
                            + ",\"amount\":7500.00,\"paymentMethod\":\"UPI\"}"), farmer1())
                    .andExpect(status().isForbidden());

            assertThat(paymentRepository.findByBookingId(bookingF2.getId())).isEmpty();
        }

        @Test
        @DisplayName("Create: the owning farmer can pay; an admin's payment is always recorded for the booking's own farmer")
        void createAuthorized() throws Exception {
            call(create(payableF2, farmer2.getId()), farmer2()).andExpect(status().isCreated());
            assertThat(paymentRepository.findByBookingId(payableF2.getId()).orElseThrow().getFarmerId()).isEqualTo(farmer2.getId());

            // the admin's body names farmer2, but payableF1 belongs to farmer1
            call(create(payableF1, farmer2.getId()), admin()).andExpect(status().isCreated());
            assertThat(paymentRepository.findByBookingId(payableF1.getId()).orElseThrow().getFarmerId()).isEqualTo(farmer1.getId());
        }

        @Test
        @DisplayName("Read by ID and by booking: anonymous 401; operator, other partner, other farmer 403; owners and admin 200")
        void readMatrix() throws Exception {
            // A fresh request per call: MockMvc builders accumulate headers when reused
            for (String path : new String[]{"/api/payments/" + paymentF1.getId(), "/api/payments/booking/" + bookingF1.getId()}) {
                assertDenied(get(path), null, 401, "TXN-P1-F1");
                assertDenied(get(path), operatorX(), 403, "TXN-P1-F1");
                assertDenied(get(path), partnerB(), 403, "TXN-P1-F1");
                assertDenied(get(path), farmer2(), 403, "TXN-P1-F1");
                for (String authorization : new String[]{farmer1(), partnerA(), admin()}) {
                    call(get(path), authorization)
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.data.transactionId").value("TXN-P1-F1"));
                }
            }
        }

        @Test
        @DisplayName("Farmer payment history: another farmer's ID or a partner is refused; own ID works")
        void farmerHistoryRequiresOwnId() throws Exception {
            assertDenied(get("/api/payments/farmer/" + farmer1.getId()), farmer2(), 403, "TXN-P1-F1");
            assertDenied(get("/api/payments/farmer/" + farmer1.getId()), partnerA(), 403, "TXN-P1-F1");
            call(get("/api/payments/farmer/" + farmer1.getId()), farmer1())
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("TXN-P1-F1")));
        }
    }
}
