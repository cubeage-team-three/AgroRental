package com.agrorental.payment;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
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
import com.agrorental.payment.entity.PaymentRefund;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.entity.RefundStatus;
import com.agrorental.payment.repository.PaymentRefundEventRepository;
import com.agrorental.payment.repository.PaymentRefundRepository;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.security.jwt.AccountType;
import com.agrorental.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BIZ-20 refund endpoints against the real SecurityConfig, JWT filter and signed tokens. Farmer, partner, operator
 * and admin share numeric ID 1, so trusting an ID from the wrong account table would show up as a success.
 * A farmer cancels a paid booking over HTTP; only an admin can then see or move the refund.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:biz20_admin_refund_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=biz20-refund-test-secret-key-32-chars-minimum-hmac",
    "server.port=0"
})
@DisplayName("BIZ-20: Admin refund endpoints (real security)")
class AdminRefundSecurityIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JwtService jwtService;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private OperatorRepository operatorRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentRefundRepository paymentRefundRepository;
    @Autowired private PaymentRefundEventRepository paymentRefundEventRepository;

    private MockMvc mockMvc;
    private Partner partner;
    private Farmer farmer;
    private Operator operator;
    private Admin admin;
    private Booking booking;
    private Payment payment;

    @BeforeAll
    void createFixtures() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();

        partner = partnerRepository.save(Partner.builder().fullName("Refund Partner").businessName("Refund Rentals")
                .mobileNumber("9600000901").email("partner@biz20.test").password("x").otpVerified(true)
                .verificationStatus(Partner.VerificationStatus.APPROVED).build());
        farmer = farmerRepository.save(Farmer.builder().fullName("Refund Farmer").mobileNumber("9800000901")
                .email("farmer@biz20.test").accountStatus("ACTIVE").build());
        Operator op = new Operator();
        op.setFullName("Refund Operator");
        op.setMobileNumber("9700000901");
        op.setEmail("operator@biz20.test");
        op.setAddress("Pune");
        op.setAadhaarNumber("123412341234");
        op.setDrivingLicenseNumber("MH-12-0000-0901");
        op.setExperience(3);
        op.setSkills("Tractor");
        op.setPassword("x");
        op.setStatus(OperatorStatus.APPROVED);
        op.setMobileVerified(true);
        operator = operatorRepository.save(op);
        admin = adminRepository.save(Admin.builder().fullName("Admin").email("admin@biz20.test").password("x").build());

        Equipment equipment = equipmentRepository.save(Equipment.builder().name("Tractor").category(EquipmentCategory.TRACTOR)
                .brand("Mahindra").model("575 DI").manufacturingYear(2023).capacity("47 HP").rentalPrice(new BigDecimal("2500.00"))
                .fuelType(FuelType.DIESEL).description("Tractor").locationAddress("Pune").latitude(18.52).longitude(73.85)
                .partner(partner).availabilityStatus(AvailabilityStatus.BOOKED).build());
        booking = bookingRepository.save(Booking.builder().farmerId(farmer.getId()).partner(partner).equipment(equipment)
                .status(BookingStatus.CONFIRMED).startDate(LocalDate.of(2031, 3, 1)).endDate(LocalDate.of(2031, 3, 3))
                .totalCost(new BigDecimal("7500.00")).deliveryAddress("Field").build());
        payment = paymentRepository.save(Payment.builder().bookingId(booking.getId()).farmerId(farmer.getId())
                .partnerId(partner.getId()).amount(new BigDecimal("7500.00")).paymentMethod(PaymentMethod.UPI)
                .transactionId("TXN-BIZ20-1").invoiceReference("INV-BIZ20-1").paymentStatus(PaymentStatus.SUCCESS)
                .paymentDate(LocalDateTime.now()).build());

        assertThat(farmer.getId()).isEqualTo(partner.getId()).isEqualTo(operator.getId()).isEqualTo(admin.getId());
    }

    private String token(Long id, String role, AccountType type) {
        return "Bearer " + jwtService.generateToken(id, role, "0000000000", JwtService.accountTypeClaims(type));
    }

    private String farmerToken() { return token(farmer.getId(), "FARMER", AccountType.FARMER); }
    private String partnerToken() { return token(partner.getId(), "PARTNER", AccountType.PARTNER); }
    private String operatorToken() { return token(operator.getId(), "OPERATOR", AccountType.OPERATOR); }
    private String adminToken() { return token(admin.getId(), "ADMIN", AccountType.ADMIN); }

    /** A fresh request per call: MockMvc builders accumulate headers when reused. */
    private ResultActions call(MockHttpServletRequestBuilder request, String authorization) throws Exception {
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return mockMvc.perform(request);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private PaymentRefund refund() {
        return paymentRefundRepository.findByPaymentId(payment.getId()).orElseThrow();
    }

    @Test
    @Order(1)
    void farmerCancellingThePaidBookingOverHttpInitiatesTheFullRefund() throws Exception {
        call(patch("/api/farmers/bookings/" + booking.getId() + "/cancel"), farmerToken()).andExpect(status().isOk());

        PaymentRefund refund = refund();
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
        assertThat(refund.getAmount()).isEqualByComparingTo("7500.00");
        assertThat(refund.getInitiatedBy()).isEqualTo("FARMER_" + farmer.getId());
    }

    @Test
    @Order(2)
    void nonAdminsCannotListReadOrMoveRefunds() throws Exception {
        Long id = refund().getId();
        String[][] requests = {
                {"GET", "/api/admin/refunds", null},
                {"GET", "/api/admin/refunds/" + id, null},
                {"POST", "/api/admin/refunds/" + id + "/complete", "{\"transferReference\":\"UTR998877665544\"}"},
                {"POST", "/api/admin/refunds/" + id + "/fail", "{\"reason\":\"bounced\"}"},
                {"POST", "/api/admin/refunds/" + id + "/retry", "{}"},
        };
        for (String[] r : requests) {
            call(build(r), null).andExpect(status().isUnauthorized());
            for (String authorization : new String[]{farmerToken(), partnerToken(), operatorToken()}) {
                call(build(r), authorization).andExpect(status().isForbidden());
            }
        }
        PaymentRefund unchanged = refund();
        assertThat(unchanged.getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
        assertThat(unchanged.getTransferReference()).isNull();
        assertThat(paymentRefundEventRepository.findByRefundIdOrderByOccurredAtAscIdAsc(id)).hasSize(1);
    }

    private static MockHttpServletRequestBuilder build(String[] r) {
        MockHttpServletRequestBuilder request = "GET".equals(r[0]) ? get(r[1]) : post(r[1]);
        return r[2] != null ? json(request, r[2]) : request;
    }

    @Test
    @Order(3)
    void adminCompletionRequiresAReferenceThenRecordsTheVerifiedTransfer() throws Exception {
        Long id = refund().getId();
        call(get("/api/admin/refunds?status=REFUND_PENDING"), adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")].amount").value(7500.0));

        call(json(post("/api/admin/refunds/" + id + "/complete"), "{}"), adminToken()).andExpect(status().isBadRequest());
        call(json(post("/api/admin/refunds/" + id + "/complete"), "{\"transferReference\":\"bad ref\"}"), adminToken())
                .andExpect(status().isBadRequest());
        assertThat(refund().getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);

        call(json(post("/api/admin/refunds/" + id + "/fail"), "{\"reason\":\"UPI handle inactive\"}"), adminToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("REFUND_FAILED"));
        call(json(post("/api/admin/refunds/" + id + "/retry"), "{\"note\":\"new UPI handle\"}"), adminToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.attemptCount").value(2));
        call(json(post("/api/admin/refunds/" + id + "/complete"), "{\"transferReference\":\"UTR998877665544\"}"), adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.completedBy").value("ADMIN_" + admin.getId()))
                .andExpect(jsonPath("$.data.history.length()").value(4));
        call(json(post("/api/admin/refunds/" + id + "/complete"), "{\"transferReference\":\"UTR112233445566\"}"), adminToken())
                .andExpect(status().isBadRequest());

        PaymentRefund done = refund();
        assertThat(done.getStatus()).isEqualTo(RefundStatus.REFUNDED);
        assertThat(done.getTransferReference()).isEqualTo("UTR998877665544");
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paymentRefundRepository.count()).isEqualTo(1);
    }
}
