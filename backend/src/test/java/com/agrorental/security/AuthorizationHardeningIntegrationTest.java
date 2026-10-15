package com.agrorental.security;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.enums.Role;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.notification.entity.Notification;
import com.agrorental.notification.repository.NotificationRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorDocument;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.DocumentStatus;
import com.agrorental.operator.enums.DocumentType;
import com.agrorental.operator.repository.OperatorDocumentRepository;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.jwt.AccountType;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-6/P0-7/P0-8 regression tests against the real SecurityConfig, JwtAuthenticationFilter and signed
 * tokens. Account IDs deliberately overlap across tables (farmer, partner, operator and admin #1), and
 * notifications for FARMER #1, PARTNER #1 and OPERATOR #1 belong to different owners.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:p068_authorization_hardening_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=" + AuthorizationHardeningIntegrationTest.SECRET,
    "server.port=0"
})
@DisplayName("P0-6/P0-7/P0-8: Assignment, KYC document and notification authorization")
class AuthorizationHardeningIntegrationTest {

    static final String SECRET = "p068-test-secret-key-32-chars-minimum-length-hmac";

    @Autowired private WebApplicationContext context;
    @Autowired private JwtService jwtService;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private OperatorRepository operatorRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private OperatorJobAssignmentRepository assignmentRepository;
    @Autowired private OperatorDocumentRepository documentRepository;
    @Autowired private NotificationRepository notificationRepository;

    private MockMvc mockMvc;

    private Partner partnerA;
    private Partner partnerB;
    private Farmer farmer1;
    private Farmer farmer2;
    private Farmer farmer3;
    private Operator operatorX;
    private Operator operatorY;
    private Operator pendingOperator;
    private Admin admin;
    private User unaffiliatedUser;

    private Booking bookingA1;
    private Booking bookingA2;
    private Booking bookingA3;
    private Booking bookingB1;

    private OperatorDocument xAadhaarApproved;
    private OperatorDocument xLicenseRejected;
    private OperatorDocument yAadhaarPending;
    private OperatorDocument yPhotoApproved;

    private Notification farmer1Note;
    private Notification farmer1DeletableNote;
    private Notification partner1Note;
    private Notification operator1Note;
    private Notification farmer2Note;
    private Notification farmer2OtherNote;
    private Notification farmer3Note;

    @BeforeAll
    void createFixtures() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        partnerA = partnerRepository.save(partner("Partner A", "9600000101", "partner.a@p068.test"));
        partnerB = partnerRepository.save(partner("Partner B", "9600000102", "partner.b@p068.test"));
        farmer1 = farmerRepository.save(farmer("Farmer One", "9800000101"));
        farmer2 = farmerRepository.save(farmer("Farmer Two", "9800000102"));
        farmer3 = farmerRepository.save(farmer("Farmer Three", "9800000103"));
        operatorX = operatorRepository.save(operator("Operator X", "9700000101", OperatorStatus.APPROVED));
        operatorY = operatorRepository.save(operator("Operator Y", "9700000102", OperatorStatus.APPROVED));
        pendingOperator = operatorRepository.save(operator("Pending Operator", "9700000103", OperatorStatus.PENDING));
        admin = adminRepository.save(Admin.builder().fullName("Admin").email("admin@p068.test").password("x").build());
        unaffiliatedUser = userRepository.save(User.builder().name("Plain User").email("plain.user@p068.test")
                .password("x").role(Role.FARMER).enabled(true).verified(true).build());

        Equipment equipmentA = equipmentRepository.save(equipment("Tractor A", partnerA));
        Equipment equipmentB = equipmentRepository.save(equipment("Tractor B", partnerB));
        bookingA1 = bookingRepository.save(booking(partnerA, equipmentA, farmer1, LocalDate.of(2026, 11, 10)));
        bookingA2 = bookingRepository.save(booking(partnerA, equipmentA, farmer1, LocalDate.of(2026, 12, 1)));
        bookingA3 = bookingRepository.save(booking(partnerA, equipmentA, farmer2, LocalDate.of(2027, 1, 5)));
        bookingB1 = bookingRepository.save(booking(partnerB, equipmentB, farmer2, LocalDate.of(2027, 2, 5)));

        xAadhaarApproved = documentRepository.save(document(operatorX, DocumentType.AADHAAR, DocumentStatus.APPROVED));
        xLicenseRejected = documentRepository.save(document(operatorX, DocumentType.DRIVING_LICENSE, DocumentStatus.REJECTED));
        yAadhaarPending = documentRepository.save(document(operatorY, DocumentType.AADHAAR, DocumentStatus.PENDING));
        yPhotoApproved = documentRepository.save(document(operatorY, DocumentType.PROFILE_PHOTO, DocumentStatus.APPROVED));

        farmer1Note = notificationRepository.save(note("FARMER", farmer1.getId(), "farmer1"));
        farmer1DeletableNote = notificationRepository.save(note("FARMER", farmer1.getId(), "farmer1-deletable"));
        partner1Note = notificationRepository.save(note("PARTNER", partnerA.getId(), "partner1"));
        operator1Note = notificationRepository.save(note("OPERATOR", operatorX.getId(), "operator1"));
        farmer2Note = notificationRepository.save(note("FARMER", farmer2.getId(), "farmer2"));
        farmer2OtherNote = notificationRepository.save(note("FARMER", farmer2.getId(), "farmer2-other"));
        farmer3Note = notificationRepository.save(note("FARMER", farmer3.getId(), "farmer3"));

        // Preconditions: the overlapping IDs this suite relies on
        assertThat(farmer1.getId()).isEqualTo(partnerA.getId()).isEqualTo(operatorX.getId()).isEqualTo(admin.getId());
        assertThat(unaffiliatedUser.getId()).isEqualTo(admin.getId());
    }

    private static Partner partner(String name, String mobile, String email) {
        return Partner.builder().fullName(name).businessName(name + " Rentals").mobileNumber(mobile).email(email)
                .password("x").otpVerified(true).verificationStatus(Partner.VerificationStatus.APPROVED).build();
    }

    private static Farmer farmer(String name, String mobile) {
        return Farmer.builder().fullName(name).mobileNumber(mobile).email(mobile + "@p068.test").accountStatus("ACTIVE").build();
    }

    private static Operator operator(String name, String mobile, OperatorStatus status) {
        Operator operator = new Operator();
        operator.setFullName(name);
        operator.setMobileNumber(mobile);
        operator.setEmail(mobile + "@p068.test");
        operator.setAddress("Pune");
        operator.setAadhaarNumber("123412341234");
        operator.setDrivingLicenseNumber("MH-12-0000-" + mobile.substring(6));
        operator.setExperience(3);
        operator.setSkills("Tractor");
        operator.setPassword("x");
        operator.setStatus(status);
        operator.setMobileVerified(true);
        return operator;
    }

    private static Equipment equipment(String name, Partner partner) {
        return Equipment.builder().name(name).category(EquipmentCategory.TRACTOR).brand("Mahindra").model("575 DI")
                .manufacturingYear(2023).capacity("47 HP").rentalPrice(new BigDecimal("2500.00")).fuelType(FuelType.DIESEL)
                .description("Tractor").locationAddress("Pune").latitude(18.52).longitude(73.85)
                .partner(partner).availabilityStatus(AvailabilityStatus.BOOKED).build();
    }

    private static Booking booking(Partner partner, Equipment equipment, Farmer farmer, LocalDate start) {
        return Booking.builder().farmerId(farmer.getId()).partner(partner).equipment(equipment)
                .status(BookingStatus.CONFIRMED).startDate(start).endDate(start.plusDays(2))
                .totalCost(new BigDecimal("7500.00")).deliveryAddress("Farm").build();
    }

    private static OperatorDocument document(Operator operator, DocumentType type, DocumentStatus status) {
        return OperatorDocument.builder().operator(operator).documentType(type).documentNumber("DOC-" + type)
                .fileName(type + ".pdf").fileUrl("https://files.test/original/" + operator.getMobileNumber() + "/" + type)
                .verificationStatus(status).build();
    }

    private static Notification note(String role, Long id, String title) {
        return Notification.builder().recipientRole(role).recipientId(id).title(title).message(title)
                .notificationType("TEST").isRead(false).build();
    }

    // ---- tokens -------------------------------------------------------------------------------

    private String token(Long id, String role, AccountType type) {
        return "Bearer " + jwtService.generateToken(id, role, "0000000000", JwtService.accountTypeClaims(type));
    }

    private String farmer1Token() { return token(farmer1.getId(), "FARMER", AccountType.FARMER); }
    private String farmer2Token() { return token(farmer2.getId(), "FARMER", AccountType.FARMER); }
    private String farmer3Token() { return token(farmer3.getId(), "FARMER", AccountType.FARMER); }
    private String partnerAToken() { return token(partnerA.getId(), "PARTNER", AccountType.PARTNER); }
    private String partnerBToken() { return token(partnerB.getId(), "PARTNER", AccountType.PARTNER); }
    private String operatorXToken() { return token(operatorX.getId(), "OPERATOR", AccountType.OPERATOR); }
    private String operatorYToken() { return token(operatorY.getId(), "OPERATOR", AccountType.OPERATOR); }
    private String adminToken() { return token(admin.getId(), "ADMIN", AccountType.ADMIN); }

    /** Validly signed USER-typed admin token for a users row that is NOT an admin. */
    private String unaffiliatedUserToken() throws Exception {
        long now = Instant.now().getEpochSecond();
        String payload = "{\"sub\":\"" + unaffiliatedUser.getId() + "\",\"role\":\"ADMIN\",\"acct\":\"USER\",\"iat\":"
                + now + ",\"exp\":" + (now + 600) + "}";
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String data = b64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
                + "." + b64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "Bearer " + data + "." + b64.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    // ---- P0-6 -------------------------------------------------------------------------------

    @Nested
    @DisplayName("P0-6: Operator assignment")
    class Assignment {

        private ResultActions assign(Booking booking, Operator operator, String authorization) throws Exception {
            var request = post("/api/bookings/" + booking.getId() + "/operator")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"operatorId\":" + operator.getId() + "}");
            if (authorization != null) {
                request.header("Authorization", authorization);
            }
            return mockMvc.perform(request);
        }

        private void assertNotAssigned(Booking booking) {
            assertThat(assignmentRepository.findByBookingId(booking.getId())).isEmpty();
            assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Farmer, operator, unaffiliated user and anonymous callers cannot assign")
        void nonPartnerNonAdminCallersCannotAssign() throws Exception {
            assign(bookingA3, operatorX, farmer1Token()).andExpect(status().isForbidden());
            assign(bookingA3, operatorX, operatorXToken()).andExpect(status().isForbidden());
            assign(bookingA3, operatorX, unaffiliatedUserToken()).andExpect(status().isUnauthorized());
            assign(bookingA3, operatorX, null).andExpect(status().isUnauthorized());
            assertNotAssigned(bookingA3);
        }

        @Test
        @DisplayName("A partner cannot assign an operator to another partner's booking (either endpoint)")
        void partnerCannotAssignToOtherPartnersBooking() throws Exception {
            assign(bookingA3, operatorX, partnerBToken()).andExpect(status().isBadRequest());
            assertNotAssigned(bookingA3);

            mockMvc.perform(patch("/api/bookings/" + bookingB1.getId() + "/assign-operator")
                            .header("Authorization", partnerAToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"operatorId\":" + operatorX.getId() + "}"))
                    .andExpect(status().is4xxClientError());
            assertNotAssigned(bookingB1);
        }

        @Test
        @DisplayName("The owning partner can assign an operator to its own booking")
        void owningPartnerCanAssign() throws Exception {
            assign(bookingA1, operatorX, partnerAToken())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.operatorId").value(operatorX.getId()));
            assertThat(assignmentRepository.findByBookingId(bookingA1.getId()).orElseThrow().getOperator().getId())
                    .isEqualTo(operatorX.getId());
        }

        @Test
        @DisplayName("An admin can assign an operator to any partner's booking")
        void adminCanAssign() throws Exception {
            assign(bookingA2, operatorY, adminToken())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.operatorId").value(operatorY.getId()));
            assertThat(assignmentRepository.findByBookingId(bookingA2.getId())).isPresent();
        }
    }

    // ---- P0-7 -------------------------------------------------------------------------------

    @Nested
    @DisplayName("P0-7: Operator KYC documents")
    class KycDocuments {

        private ResultActions upload(Operator owner, DocumentType type, String url, String authorization) throws Exception {
            var request = post("/api/operators/" + owner.getId() + "/documents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"documentType\":\"" + type + "\",\"documentNumber\":\"NEW-123\",\"fileName\":\"new.pdf\",\"fileUrl\":\""
                            + url + "\"}");
            if (authorization != null) {
                request.header("Authorization", authorization);
            }
            return mockMvc.perform(request);
        }

        private void assertUnchanged(OperatorDocument original) {
            OperatorDocument current = documentRepository.findById(original.getId()).orElseThrow();
            assertThat(current.getFileUrl()).isEqualTo(original.getFileUrl());
            assertThat(current.getDocumentNumber()).isEqualTo(original.getDocumentNumber());
            assertThat(current.getVerificationStatus()).isEqualTo(original.getVerificationStatus());
        }

        @Test
        @DisplayName("Another operator, a partner, a farmer, a pending operator and anonymous callers cannot replace documents")
        void nonOwnersCannotReplaceDocuments() throws Exception {
            // Snapshot current state (another test may legitimately have replaced this document as its owner)
            OperatorDocument licenseBefore = documentRepository.findById(xLicenseRejected.getId()).orElseThrow();
            long before = documentRepository.count();
            upload(operatorX, DocumentType.DRIVING_LICENSE, "https://evil.test/a", operatorYToken()).andExpect(status().isForbidden());
            upload(operatorX, DocumentType.DRIVING_LICENSE, "https://evil.test/b", partnerAToken()).andExpect(status().isForbidden());
            upload(operatorX, DocumentType.DRIVING_LICENSE, "https://evil.test/c", farmer1Token()).andExpect(status().isForbidden());
            upload(operatorX, DocumentType.DRIVING_LICENSE, "https://evil.test/d",
                    token(pendingOperator.getId(), "OPERATOR", AccountType.OPERATOR)).andExpect(status().isUnauthorized());
            upload(operatorX, DocumentType.DRIVING_LICENSE, "https://evil.test/e", null).andExpect(status().isUnauthorized());
            upload(operatorY, DocumentType.EXPERIENCE_CERTIFICATE, "https://evil.test/f", null).andExpect(status().isUnauthorized());

            assertUnchanged(licenseBefore);
            assertThat(documentRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("An operator cannot replace its own APPROVED document")
        void ownerCannotReplaceApprovedDocument() throws Exception {
            upload(operatorX, DocumentType.AADHAAR, "https://files.test/swap", operatorXToken()).andExpect(status().isForbidden());
            assertUnchanged(xAadhaarApproved);
        }

        @Test
        @DisplayName("An operator can replace its own REJECTED document, which returns to PENDING review")
        void ownerCanReplaceRejectedDocument() throws Exception {
            upload(operatorX, DocumentType.DRIVING_LICENSE, "https://files.test/new-license", operatorXToken())
                    .andExpect(status().isCreated());
            OperatorDocument current = documentRepository.findById(xLicenseRejected.getId()).orElseThrow();
            assertThat(current.getFileUrl()).isEqualTo("https://files.test/new-license");
            assertThat(current.getVerificationStatus()).isEqualTo(DocumentStatus.PENDING);
            // Operator approval state is never changed by a document upload
            assertThat(operatorRepository.findById(operatorX.getId()).orElseThrow().getStatus()).isEqualTo(OperatorStatus.APPROVED);
        }

        @Test
        @DisplayName("An admin can replace an approved document, which returns to PENDING review")
        void adminCanReplaceDocument() throws Exception {
            upload(operatorY, DocumentType.PROFILE_PHOTO, "https://files.test/admin-photo", adminToken())
                    .andExpect(status().isCreated());
            OperatorDocument current = documentRepository.findById(yPhotoApproved.getId()).orElseThrow();
            assertThat(current.getFileUrl()).isEqualTo("https://files.test/admin-photo");
            assertThat(current.getVerificationStatus()).isEqualTo(DocumentStatus.PENDING);
        }

        @Test
        @DisplayName("Only admins can read KYC documents (not anonymous, the owner, other operators or partners)")
        void onlyAdminsCanReadDocuments() throws Exception {
            String path = "/api/operators/" + operatorY.getId() + "/documents";
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(path).header("Authorization", operatorYToken())).andExpect(status().isForbidden());
            mockMvc.perform(get(path).header("Authorization", operatorXToken())).andExpect(status().isForbidden());
            mockMvc.perform(get(path).header("Authorization", partnerAToken())).andExpect(status().isForbidden());
            mockMvc.perform(get(path).header("Authorization", farmer1Token())).andExpect(status().isForbidden());

            mockMvc.perform(get(path).header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
            assertUnchanged(yAadhaarPending);
        }
    }

    // ---- P0-8 -------------------------------------------------------------------------------

    @Nested
    @DisplayName("P0-8: Notifications")
    class Notifications {

        private boolean isRead(Notification notification) {
            return notificationRepository.findById(notification.getId()).orElseThrow().getIsRead();
        }

        @Test
        @DisplayName("Listing with another user's role/id returns only the caller's own notifications")
        void listIgnoresForgedRoleAndId() throws Exception {
            mockMvc.perform(get("/api/notifications").param("role", "PARTNER").param("id", String.valueOf(partnerA.getId()))
                            .header("Authorization", farmer1Token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].recipientRole").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("FARMER"))))
                    .andExpect(jsonPath("$.data[*].recipientId").value(org.hamcrest.Matchers.everyItem(
                            org.hamcrest.Matchers.is(farmer1.getId().intValue()))))
                    .andExpect(jsonPath("$.data[?(@.title == 'partner1')]").isEmpty());

            mockMvc.perform(get("/api/notifications").param("role", "FARMER").param("id", String.valueOf(farmer1.getId()))
                            .header("Authorization", operatorXToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].recipientRole").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("OPERATOR"))))
                    .andExpect(jsonPath("$.data[*].recipientId").value(org.hamcrest.Matchers.everyItem(
                            org.hamcrest.Matchers.is(operatorX.getId().intValue()))))
                    .andExpect(jsonPath("$.data[*].title").value(org.hamcrest.Matchers.hasItem("operator1")))
                    .andExpect(jsonPath("$.data[?(@.title == 'farmer1')]").isEmpty());
        }

        @Test
        @DisplayName("Unread count ignores forged role/id and counts only the caller's notifications")
        void unreadCountIgnoresForgedRoleAndId() throws Exception {
            long own = notificationRepository.countByRecipientRoleAndRecipientIdAndIsReadFalse("FARMER", farmer2.getId());
            mockMvc.perform(get("/api/notifications/unread-count").param("role", "OPERATOR").param("id", String.valueOf(operatorX.getId()))
                            .header("Authorization", farmer2Token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.unreadCount").value((int) own));
        }

        @Test
        @DisplayName("Marking, deleting or clearing another user's notifications never mutates them")
        void forgedMutationsDoNotTouchOtherUsersNotifications() throws Exception {
            mockMvc.perform(patch("/api/notifications/" + operator1Note.getId() + "/read")
                            .param("role", "OPERATOR").param("id", String.valueOf(operatorX.getId()))
                            .header("Authorization", farmer1Token()))
                    .andExpect(status().is4xxClientError());
            assertThat(isRead(operator1Note)).isFalse();

            mockMvc.perform(delete("/api/notifications/" + farmer2Note.getId())
                            .param("role", "FARMER").param("id", String.valueOf(farmer2.getId()))
                            .header("Authorization", farmer1Token()))
                    .andExpect(status().is4xxClientError());
            assertThat(notificationRepository.findById(farmer2Note.getId())).isPresent();

            mockMvc.perform(patch("/api/notifications/read-all").param("role", "PARTNER").param("id", String.valueOf(partnerA.getId()))
                            .header("Authorization", farmer3Token()))
                    .andExpect(status().isOk());
            assertThat(isRead(partner1Note)).isFalse();
            assertThat(isRead(farmer3Note)).isTrue();

            mockMvc.perform(delete("/api/notifications/clear-all").param("role", "FARMER").param("id", String.valueOf(farmer2.getId()))
                            .header("Authorization", partnerBToken()))
                    .andExpect(status().isOk());
            assertThat(notificationRepository.findById(farmer2OtherNote.getId())).isPresent();
            assertThat(notificationRepository.findById(partner1Note.getId())).isPresent();
        }

        @Test
        @DisplayName("Owners can still read and delete their own notifications, with or without legacy parameters")
        void ownersCanManageOwnNotifications() throws Exception {
            mockMvc.perform(patch("/api/notifications/" + farmer1Note.getId() + "/read")
                            .param("role", "FARMER").param("id", String.valueOf(farmer1.getId()))
                            .header("Authorization", farmer1Token()))
                    .andExpect(status().isOk());
            assertThat(isRead(farmer1Note)).isTrue();

            // The frontend's DELETE drops query parameters; it must work without them
            mockMvc.perform(delete("/api/notifications/" + farmer1DeletableNote.getId()).header("Authorization", farmer1Token()))
                    .andExpect(status().isOk());
            assertThat(notificationRepository.findById(farmer1DeletableNote.getId())).isEmpty();
        }

        @Test
        @DisplayName("Admins get their own (empty) notification namespace; anonymous callers are rejected")
        void adminAndAnonymous() throws Exception {
            mockMvc.perform(get("/api/notifications").param("role", "FARMER").param("id", String.valueOf(farmer1.getId()))
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
            mockMvc.perform(get("/api/notifications").param("role", "FARMER").param("id", String.valueOf(farmer1.getId())))
                    .andExpect(status().isUnauthorized());
        }
    }
}
