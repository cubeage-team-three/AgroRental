package com.agrorental.booking.workflow;

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
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.notification.entity.Notification;
import com.agrorental.notification.repository.NotificationRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRefundRepository;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.security.jwt.AccountType;
import com.agrorental.security.jwt.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Shared fixtures for the farmer/partner booking workflow suite. Everything runs through the real
 * SecurityConfig, JwtAuthenticationFilter (real signed tokens), controllers, services and JPA (H2 here; the
 * concurrency scenarios can also run on a disposable MySQL, see {@link BookingWorkflowConcurrencyMySQLTest}).
 *
 * <p>Every test creates its own partner, farmers and equipment, so tests are independent of each other and of
 * rows left by other classes sharing the same Spring context. Dates are relative to today so the
 * {@code @FutureOrPresent} request rules apply exactly as in production.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:wb_booking_workflow_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=wb-booking-workflow-test-secret-key-32-chars-minimum",
    "server.port=0"
})
abstract class AbstractBookingWorkflowTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(7000);
    protected static final BigDecimal DAILY_RATE = new BigDecimal("2500.00");

    @Autowired protected WebApplicationContext context;
    @Autowired protected JwtService jwtService;
    @Autowired protected PartnerRepository partnerRepository;
    @Autowired protected FarmerRepository farmerRepository;
    @Autowired protected FarmRepository farmRepository;
    @Autowired protected OperatorRepository operatorRepository;
    @Autowired protected AdminRepository adminRepository;
    @Autowired protected EquipmentRepository equipmentRepository;
    @Autowired protected BookingRepository bookingRepository;
    @Autowired protected NotificationRepository notificationRepository;
    @Autowired protected PaymentRepository paymentRepository;
    @Autowired protected PaymentRefundRepository paymentRefundRepository;

    protected MockMvc mockMvc;

    @BeforeEach
    void buildMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---- fixtures ---------------------------------------------------------------------------------------

    protected static int next() {
        return SEQUENCE.incrementAndGet();
    }

    private static String mobile(String prefix, int n) {
        return prefix + String.format("%08d", n);
    }

    protected Partner newPartner() {
        int n = next();
        return partnerRepository.save(Partner.builder().fullName("Workflow Partner " + n).businessName("Rentals " + n)
                .mobileNumber(mobile("96", n)).email("wb.partner" + n + "@workflow.test").password("x").otpVerified(true)
                .verificationStatus(Partner.VerificationStatus.APPROVED).build());
    }

    protected Farmer newFarmer() {
        int n = next();
        return farmerRepository.save(Farmer.builder().fullName("Workflow Farmer " + n).mobileNumber(mobile("98", n))
                .email("wb.farmer" + n + "@workflow.test").accountStatus("ACTIVE").build());
    }

    protected Operator newOperator() {
        int n = next();
        Operator operator = new Operator();
        operator.setFullName("Workflow Operator " + n);
        operator.setMobileNumber(mobile("97", n));
        operator.setEmail("wb.operator" + n + "@workflow.test");
        operator.setAddress("Pune");
        operator.setAadhaarNumber(String.format("%012d", n));
        operator.setDrivingLicenseNumber("MH-12-" + n);
        operator.setExperience(3);
        operator.setSkills("Tractor");
        operator.setPassword("x");
        operator.setStatus(OperatorStatus.APPROVED);
        operator.setMobileVerified(true);
        return operatorRepository.save(operator);
    }

    protected Admin newAdmin() {
        int n = next();
        return adminRepository.save(Admin.builder().fullName("Workflow Admin " + n)
                .email("wb.admin" + n + "@workflow.test").password("x").build());
    }

    protected Equipment newEquipment(Partner owner) {
        return newEquipment(owner, b -> { });
    }

    protected Equipment newEquipment(Partner owner, Consumer<Equipment.EquipmentBuilder> customizer) {
        int n = next();
        Equipment.EquipmentBuilder builder = Equipment.builder().name("Workflow Tractor " + n)
                .category(EquipmentCategory.TRACTOR).brand("Mahindra").model("575 DI").manufacturingYear(2023)
                .capacity("47 HP").rentalPrice(DAILY_RATE).fuelType(FuelType.DIESEL).description("Tractor")
                .locationAddress("Workflow Village " + n + ", Pune").latitude(18.52).longitude(73.85)
                .partner(owner).availabilityStatus(AvailabilityStatus.AVAILABLE).isDisabled(false);
        customizer.accept(builder);
        return equipmentRepository.save(builder.build());
    }

    protected Farm newFarm(Farmer owner) {
        int n = next();
        return farmRepository.save(Farm.builder().farmerId(owner.getId()).farmName("Farm " + n).village("Khed")
                .taluka("Pune").district("Pune").state("Maharashtra").farmArea(new BigDecimal("4.50")).build());
    }

    /** Inserts a booking directly (bypassing the workflow), e.g. to put a booking in a later lifecycle state. */
    protected Booking insertBooking(Farmer farmer, Equipment equipment, BookingStatus status, LocalDate start, LocalDate end) {
        return bookingRepository.save(Booking.builder().farmerId(farmer.getId()).partner(equipment.getPartner())
                .equipment(equipment).status(status).startDate(start).endDate(end)
                .totalCost(equipment.getRentalPrice().multiply(BigDecimal.valueOf(end.toEpochDay() - start.toEpochDay() + 1)))
                .deliveryAddress("Field").build());
    }

    protected Payment insertSuccessfulPayment(Booking booking) {
        return paymentRepository.save(Payment.builder().bookingId(booking.getId()).farmerId(booking.getFarmerId())
                .partnerId(booking.getPartner().getId()).amount(booking.getTotalCost()).paymentMethod(PaymentMethod.UPI)
                .transactionId("TXN-WB-" + booking.getId()).paymentStatus(PaymentStatus.SUCCESS)
                .paymentDate(LocalDateTime.now()).invoiceReference("INV-WB-" + booking.getId()).build());
    }

    protected static LocalDate day(int offsetFromToday) {
        return LocalDate.now().plusDays(offsetFromToday);
    }

    // ---- tokens ---------------------------------------------------------------------------------------

    private String token(Long id, String role, AccountType type) {
        return "Bearer " + jwtService.generateToken(id, role, "0000000000", JwtService.accountTypeClaims(type));
    }

    protected String tokenFor(Farmer farmer) { return token(farmer.getId(), "FARMER", AccountType.FARMER); }
    protected String tokenFor(Partner partner) { return token(partner.getId(), "PARTNER", AccountType.PARTNER); }
    protected String tokenFor(Operator operator) { return token(operator.getId(), "OPERATOR", AccountType.OPERATOR); }
    protected String tokenFor(Admin admin) { return token(admin.getId(), "ADMIN", AccountType.ADMIN); }

    // ---- HTTP helpers ---------------------------------------------------------------------------------

    protected MvcResult call(MockHttpServletRequestBuilder request, String authorization) throws Exception {
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return mockMvc.perform(request).andReturn();
    }

    protected MvcResult callJson(MockHttpServletRequestBuilder request, String authorization, String json) throws Exception {
        return call(request.contentType(MediaType.APPLICATION_JSON).content(json), authorization);
    }

    protected static int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    protected static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    @SuppressWarnings("unchecked")
    protected static <T> T json(MvcResult result, String path) throws Exception {
        return (T) JsonPath.read(body(result), path);
    }

    protected static String bookingJson(Long equipmentId, LocalDate start, LocalDate end) {
        return """
                {"equipmentId": %d, "startDate": "%s", "endDate": "%s", "deliveryAddress": "Plot 4, Khed"}
                """.formatted(equipmentId, start, end);
    }

    protected MvcResult postBooking(Farmer farmer, Long equipmentId, LocalDate start, LocalDate end) throws Exception {
        return callJson(post("/api/bookings"), tokenFor(farmer), bookingJson(equipmentId, start, end));
    }

    /** Creates a booking through the real API and returns its id (asserts 201). */
    protected Long createBooking(Farmer farmer, Equipment equipment, LocalDate start, LocalDate end) throws Exception {
        MvcResult result = postBooking(farmer, equipment.getId(), start, end);
        assertThat(status(result)).as("create booking: %s", body(result)).isEqualTo(201);
        return ((Number) json(result, "$.data.id")).longValue();
    }

    protected MvcResult accept(Partner partner, Long bookingId) throws Exception {
        return call(patch("/api/bookings/" + bookingId + "/accept"), tokenFor(partner));
    }

    protected MvcResult reject(Partner partner, Long bookingId, String reason) throws Exception {
        String payload = reason == null ? "{}" : "{\"rejectionReason\": \"" + reason + "\"}";
        return callJson(patch("/api/bookings/" + bookingId + "/reject"), tokenFor(partner), payload);
    }

    protected MvcResult farmerCancel(Farmer farmer, Long bookingId) throws Exception {
        return call(patch("/api/bookings/" + bookingId + "/cancel"), tokenFor(farmer));
    }

    protected MvcResult adminStatus(Admin admin, Long bookingId, BookingStatus status) throws Exception {
        return callJson(patch("/api/admin/bookings/" + bookingId + "/status"), tokenFor(admin),
                "{\"status\": \"" + status.name() + "\"}");
    }

    protected MvcResult adminCancel(Admin admin, Long bookingId) throws Exception {
        return call(patch("/api/admin/bookings/" + bookingId + "/cancel"), tokenFor(admin));
    }

    protected MvcResult anonymousGet(String url) throws Exception {
        return call(get(url), null);
    }

    // ---- state readers --------------------------------------------------------------------------------

    protected AvailabilityStatus availabilityOf(Equipment equipment) {
        return equipmentRepository.findById(equipment.getId()).orElseThrow().getAvailabilityStatus();
    }

    protected BookingStatus statusOf(Long bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow().getStatus();
    }

    protected List<Notification> notificationsFor(String role, Long recipientId, Long bookingId) {
        return notificationRepository.findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc(role, recipientId).stream()
                .filter(n -> bookingId.equals(n.getBookingId()))
                .toList();
    }

    protected List<String> notificationTypesFor(String role, Long recipientId, Long bookingId) {
        return notificationsFor(role, recipientId, bookingId).stream().map(Notification::getNotificationType).toList();
    }

    // ---- concurrency ----------------------------------------------------------------------------------

    /** Runs all tasks at the same moment (real threads); each result is the returned value or the thrown exception. */
    protected static List<Object> runConcurrently(List<Callable<Object>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        try {
            for (Callable<Object> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        return task.call();
                    } catch (Exception e) {
                        return e;
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get(90, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    /** HTTP status codes of concurrently executed MockMvc calls (exceptions are reported as -1). */
    protected static List<Integer> statuses(List<Object> results) {
        return results.stream().map(r -> r instanceof MvcResult m ? m.getResponse().getStatus() : -1).toList();
    }
}
