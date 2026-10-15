package com.agrorental.auth;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.auth.service.AuthService;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.controller.OperatorAuthController;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.otp.OtpDeliveryService;
import com.agrorental.operator.otp.OtpPurpose;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.controller.PartnerController;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.jwt.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Password login for every role against the real SecurityConfig, JwtAuthenticationFilter, H2 database and
 * signed tokens: credentials first, account state second, generic errors for unknown accounts, OTP login
 * disabled, the retired duplicate endpoints, and sessions that work after login.
 */
@SpringBootTest
// Imported top-level config (not a nested @TestConfiguration) so every @Nested class shares one identical context
@Import(PasswordLoginOtpCaptureConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth_password_login_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=auth-password-login-test-secret-32-chars-min",
    "app.otp.include-dev-mock=false",
    "server.port=0"
})
@DisplayName("Auth contract 2.1: password login for every role")
class PasswordLoginIntegrationTest {

    static final String PASSWORD = "Correct-Horse-1";
    static final String WRONG_PASSWORD = "Wrong-Horse-9";

    @Autowired private WebApplicationContext context;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private OperatorRepository operatorRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private PasswordLoginCapturingOtpDelivery otpDelivery;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private Farmer activeFarmer;
    private Farmer pendingOtpFarmer;
    private Partner approvedPartner;
    private Operator approvedUnverifiedOperator;
    private Admin admin;

    @BeforeAll
    void createAccounts() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        String hash = passwordEncoder.encode(PASSWORD);

        activeFarmer = farmerRepository.save(farmer("Active Farmer", "9800000101", "farmer.active@login.test", hash, "ACTIVE", true));
        pendingOtpFarmer = farmerRepository.save(farmer("Legacy Pending Farmer", "9800000102", "farmer.pending@login.test", hash, "PENDING_OTP", true));
        farmerRepository.save(farmer("Otp Only Farmer", "9800000103", "farmer.otponly@login.test", null, "ACTIVE", true));
        farmerRepository.save(farmer("Inactive Farmer", "9800000104", "farmer.inactive@login.test", hash, "INACTIVE", true));
        farmerRepository.save(farmer("Deactivated Farmer", "9800000105", "farmer.off@login.test", hash, "ACTIVE", false));

        approvedPartner = partnerRepository.save(partner("Approved Partner", "9600000101", Partner.VerificationStatus.APPROVED, true, hash));
        partnerRepository.save(partner("Pending Partner", "9600000102", Partner.VerificationStatus.PENDING, true, hash));
        partnerRepository.save(partner("Rejected Partner", "9600000103", Partner.VerificationStatus.REJECTED, false, hash));

        approvedUnverifiedOperator = operatorRepository.save(operator("Approved Operator", "9700000101", OperatorStatus.APPROVED, true, false, hash));
        operatorRepository.save(operator("Pending Operator", "9700000102", OperatorStatus.PENDING, true, false, hash));
        Operator rejected = operator("Rejected Operator", "9700000103", OperatorStatus.REJECTED, false, false, hash);
        rejected.setRejectionReason("Licence expired");
        operatorRepository.save(rejected);
        operatorRepository.save(operator("Inactive Operator", "9700000104", OperatorStatus.APPROVED, false, true, hash));

        admin = adminRepository.save(Admin.builder().fullName("Login Admin").email("admin@login.test").password(hash).build());
        Admin inactiveAdmin = Admin.builder().fullName("Off Admin").email("admin.off@login.test").password(hash).build();
        inactiveAdmin.setActive(false);
        adminRepository.save(inactiveAdmin);
    }

    private static Farmer farmer(String name, String mobile, String email, String hash, String status, boolean active) {
        Farmer farmer = Farmer.builder().fullName(name).mobileNumber(mobile).email(email).password(hash).accountStatus(status).build();
        farmer.setActive(active);
        return farmer;
    }

    private static Partner partner(String name, String mobile, Partner.VerificationStatus status, boolean active, String hash) {
        Partner partner = Partner.builder().fullName(name).businessName(name + " Rentals").mobileNumber(mobile)
                .email(mobile + "@login.test").password(hash).otpVerified(false).verificationStatus(status).build();
        partner.setActive(active);
        return partner;
    }

    private static Operator operator(String name, String mobile, OperatorStatus status, boolean active, boolean mobileVerified, String hash) {
        Operator operator = Operator.builder().fullName(name).mobileNumber(mobile).email(mobile + "@login.test")
                .address("Pune").aadhaarNumber("123412341234").drivingLicenseNumber("MH12-" + mobile).experience(3)
                .skills("Tractor").password(hash).status(status).mobileVerified(mobileVerified).build();
        operator.setActive(active);
        return operator;
    }

    // ---- helpers ------------------------------------------------------------------------------

    private MvcResult login(String identifier, String password, String role) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(identifier, password, role, "PASSWORD")))
                .andReturn();
    }

    private String loginJson(String identifier, String password, String role, String loginType) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("mobileOrEmail", identifier);
        if (password != null) {
            body.put("password", password);
        }
        if (role != null) {
            body.put("role", role);
        }
        if (loginType != null) {
            body.put("loginType", loginType);
        }
        return objectMapper.writeValueAsString(body);
    }

    private String token(String identifier, String role) throws Exception {
        MvcResult result = login(identifier, PASSWORD, role);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    /** Response body without the per-request timestamp, for byte-for-byte comparisons. */
    private JsonNode bodyWithoutTimestamp(MvcResult result) throws Exception {
        ObjectNode node = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        node.remove("timestamp");
        return node;
    }

    private void assertGenericFailure(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(result.getResponse().getContentAsString(), "$.message"))
                .isEqualTo(AuthService.INVALID_CREDENTIALS_MESSAGE);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("\"token\"");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---- tests --------------------------------------------------------------------------------

    @Test
    @DisplayName("No username/password user store exists: Spring Boot's generated in-memory user is not created")
    void noGeneratedInMemoryUser() {
        java.util.Map<String, org.springframework.security.core.userdetails.UserDetailsService> services =
                context.getBeansOfType(org.springframework.security.core.userdetails.UserDetailsService.class);
        assertThat(services).hasSize(1);
        assertThat(services.values()).noneMatch(service ->
                service instanceof org.springframework.security.provisioning.InMemoryUserDetailsManager);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> services.values().iterator().next().loadUserByUsername("user"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Nested
    @DisplayName("Successful logins issue role- and table-bound sessions")
    class SuccessfulLogins {

        @Test
        @DisplayName("Farmer by mobile, +91 mobile and email; the session works on the farmer profile")
        void farmerLogins() throws Exception {
            for (String identifier : new String[]{"9800000101", "+91 98000 00101", "FARMER.ACTIVE@login.test"}) {
                String token = token(identifier, "FARMER");
                assertThat(jwtService.extractAccountType(token)).isEqualTo("FARMER");
                assertThat(jwtService.extractUserId(token)).isEqualTo(activeFarmer.getId());
                mockMvc.perform(get("/api/farmers/profile").header("Authorization", bearer(token)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.mobileNumber").value("9800000101"));
            }
        }

        @Test
        @DisplayName("Legacy PENDING_OTP farmer logs in with its password and the session is accepted by the JWT filter")
        void pendingOtpFarmerCanUseItsSession() throws Exception {
            String token = token("9800000102", "FARMER");
            assertThat(jwtService.extractUserId(token)).isEqualTo(pendingOtpFarmer.getId());
            mockMvc.perform(get("/api/farmers/profile").header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.mobileNumber").value("9800000102"));
        }

        @Test
        @DisplayName("Approved partner logs in and reaches its own partner profile")
        void partnerLogin() throws Exception {
            String token = token("9600000101", "PARTNER");
            assertThat(jwtService.extractAccountType(token)).isEqualTo("PARTNER");
            mockMvc.perform(get("/api/partners/" + approvedPartner.getId()).header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.otpVerified").value(false));
        }

        @Test
        @DisplayName("Approved operator whose mobile was never OTP-verified logs in and reaches /api/operators/me")
        void operatorWithoutMobileVerificationLogsIn() throws Exception {
            String token = token("9700000101@login.test", "OPERATOR");
            assertThat(jwtService.extractAccountType(token)).isEqualTo("OPERATOR");
            assertThat(jwtService.extractScope(token)).isNull();
            mockMvc.perform(get("/api/operators/me").header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(approvedUnverifiedOperator.getId()))
                    .andExpect(jsonPath("$.data.mobileVerified").value(false));
        }

        @Test
        @DisplayName("Admin login is unchanged and reaches admin endpoints")
        void adminLogin() throws Exception {
            String token = token("admin@login.test", "ADMIN");
            assertThat(jwtService.extractAccountType(token)).isEqualTo("ADMIN");
            assertThat(jwtService.extractUserId(token)).isEqualTo(admin.getId());
            mockMvc.perform(get("/api/admin/dashboard/stats").header("Authorization", bearer(token)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Account state is only revealed after the password was verified")
    class GateOrder {

        private void assertBlockedOnlyWithCorrectPassword(String identifier, String role, String expectedMessage) throws Exception {
            assertGenericFailure(login(identifier, WRONG_PASSWORD, role));
            assertGenericFailure(login(identifier, null, role));

            MvcResult correct = login(identifier, PASSWORD, role);
            assertThat(correct.getResponse().getStatus()).isEqualTo(400);
            assertThat((String) JsonPath.read(correct.getResponse().getContentAsString(), "$.message")).isEqualTo(expectedMessage);
            assertThat(correct.getResponse().getContentAsString()).doesNotContain("\"token\"");
        }

        @Test
        @DisplayName("Farmers: INACTIVE status and a deactivated profile still block, after the password check")
        void farmerStates() throws Exception {
            assertBlockedOnlyWithCorrectPassword("9800000104", "FARMER", "Account has been deactivated. Please contact customer support.");
            assertBlockedOnlyWithCorrectPassword("9800000105", "FARMER", "Account has been deactivated. Please contact customer support.");
        }

        @Test
        @DisplayName("Partners: PENDING and REJECTED/deactivated still block, after the password check")
        void partnerStates() throws Exception {
            assertBlockedOnlyWithCorrectPassword("9600000102", "PARTNER",
                    "Your account verification is currently pending admin approval. Please check back later.");
            assertBlockedOnlyWithCorrectPassword("9600000103", "PARTNER",
                    "Partner account is currently deactivated. Please contact support.");
        }

        @Test
        @DisplayName("Operators: PENDING, REJECTED and inactive still block, after the password check")
        void operatorStates() throws Exception {
            assertBlockedOnlyWithCorrectPassword("9700000102", "OPERATOR", "Operator account is pending admin approval.");
            assertBlockedOnlyWithCorrectPassword("9700000103", "OPERATOR", "Operator account is inactive. Please contact customer support.");
            assertBlockedOnlyWithCorrectPassword("9700000104", "OPERATOR", "Operator account is inactive. Please contact customer support.");
        }

        @Test
        @DisplayName("Admins: a deactivated admin is blocked, after the password check")
        void adminStates() throws Exception {
            assertBlockedOnlyWithCorrectPassword("admin.off@login.test", "ADMIN", "Admin account is currently deactivated.");
        }
    }

    @Nested
    @DisplayName("Unknown accounts, wrong passwords and password-less accounts are indistinguishable")
    class NonEnumeration {

        @Test
        @DisplayName("Same status and body for unknown account vs wrong password, for every role")
        void identicalFailures() throws Exception {
            String[][] cases = {
                    {"FARMER", "9800000101", "9899999999"},
                    {"PARTNER", "9600000101", "9699999999"},
                    {"OPERATOR", "9700000101", "9799999999"},
                    {"ADMIN", "admin@login.test", "nobody@login.test"},
            };
            for (String[] c : cases) {
                MvcResult wrong = login(c[1], WRONG_PASSWORD, c[0]);
                MvcResult unknown = login(c[2], WRONG_PASSWORD, c[0]);
                assertGenericFailure(wrong);
                assertGenericFailure(unknown);
                assertThat(bodyWithoutTimestamp(unknown)).as(c[0]).isEqualTo(bodyWithoutTimestamp(wrong));
            }
        }

        @Test
        @DisplayName("A farmer without a password (former OTP-only account) fails exactly like a wrong password")
        void farmerWithoutPassword() throws Exception {
            MvcResult noPassword = login("9800000103", PASSWORD, "FARMER");
            MvcResult wrong = login("9800000101", WRONG_PASSWORD, "FARMER");
            assertGenericFailure(noPassword);
            assertThat(bodyWithoutTimestamp(noPassword)).isEqualTo(bodyWithoutTimestamp(wrong));
        }

        @Test
        @DisplayName("Credentials of one role never authenticate another role")
        void roleIsolation() throws Exception {
            assertGenericFailure(login("9800000101", PASSWORD, "PARTNER"));
            assertGenericFailure(login("9600000101", PASSWORD, "OPERATOR"));
            assertGenericFailure(login("9700000101", PASSWORD, "FARMER"));
            assertGenericFailure(login("admin@login.test", PASSWORD, "FARMER"));
            assertGenericFailure(login("farmer.active@login.test", PASSWORD, "ADMIN"));
        }
    }

    @Nested
    @DisplayName("OTP login and OTP sessions are gone")
    class OtpDisabled {

        @Test
        @DisplayName("POST /api/auth/login-otp is refused (with or without a body) and never returns a token")
        void loginOtpEndpointRefused() throws Exception {
            mockMvc.perform(post("/api/auth/login-otp").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mobileOrEmail\":\"9800000101\",\"otp\":\"123456\",\"role\":\"FARMER\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(AuthService.OTP_LOGIN_DISABLED_MESSAGE))
                    .andExpect(jsonPath("$.data").doesNotExist());
            mockMvc.perform(post("/api/auth/login-otp").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(AuthService.OTP_LOGIN_DISABLED_MESSAGE));
        }

        @Test
        @DisplayName("loginType=OTP on /api/auth/login is refused for every role")
        void loginTypeOtpRefused() throws Exception {
            for (String role : new String[]{"FARMER", "PARTNER", "OPERATOR", "ADMIN"}) {
                mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson("9800000101", null, role, "OTP")))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value(AuthService.OTP_LOGIN_DISABLED_MESSAGE))
                        .andExpect(jsonPath("$.data").doesNotExist());
            }
        }

        @Test
        @DisplayName("Farmer verify-otp confirms the code but issues no session token")
        void farmerVerifyOtpIssuesNoSession() throws Exception {
            mockMvc.perform(post("/api/farmers/send-otp").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mobileNumber\":\"9800000101\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.devMockOtp").doesNotExist());
            String otp = otpDelivery.lastOtp;
            assertThat(otp).matches("\\d{6}");

            MvcResult verified = mockMvc.perform(post("/api/farmers/verify-otp").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mobileNumber\":\"9800000101\",\"otp\":\"" + otp + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.verified").value(true))
                    .andExpect(jsonPath("$.data.token").doesNotExist())
                    .andExpect(jsonPath("$.data.farmerId").doesNotExist())
                    .andReturn();
            assertThat(verified.getResponse().getContentAsString()).doesNotContain("eyJ");
        }

        @Test
        @DisplayName("Retired duplicate /api/operators/login answers 410 even with valid credentials")
        void retiredOperatorLogin() throws Exception {
            mockMvc.perform(post("/api/operators/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mobileNumber\":\"9700000101\",\"password\":\"" + PASSWORD + "\"}"))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.message").value(OperatorAuthController.LOGIN_RETIRED_MESSAGE))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("Retired partner OTP endpoints answer 410 for the owning partner and stay protected for others")
        void retiredPartnerOtp() throws Exception {
            String token = token("9600000101", "PARTNER");
            mockMvc.perform(post("/api/partners/" + approvedPartner.getId() + "/otp/send").header("Authorization", bearer(token)))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.message").value(PartnerController.PARTNER_OTP_RETIRED_MESSAGE));
            mockMvc.perform(post("/api/partners/" + approvedPartner.getId() + "/otp/send"))
                    .andExpect(status().isUnauthorized());
        }
    }
}

/** Stands in for the farmer's phone so the OTP endpoints can be exercised end to end. */
class PasswordLoginCapturingOtpDelivery implements OtpDeliveryService {
    volatile String lastOtp;

    @Override
    public void deliverOtp(String mobileNumber, String rawOtp, OtpPurpose purpose) {
        lastOtp = rawOtp;
    }
}

@TestConfiguration
class PasswordLoginOtpCaptureConfig {
    @Bean
    @Primary
    PasswordLoginCapturingOtpDelivery passwordLoginCapturingOtpDelivery() {
        return new PasswordLoginCapturingOtpDelivery();
    }
}
