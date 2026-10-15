package com.agrorental.security;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.common.enums.Role;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.jwt.AccountType;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
import com.agrorental.auth.service.PasswordResetDelivery;
import com.agrorental.auth.service.PasswordResetInstructions;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-3 regression tests against the real SecurityConfig, JwtAuthenticationFilter and signed tokens,
 * with deliberately overlapping IDs across the users, farmers, operators, partners and admins tables.
 *
 * <p>Fixture layout (fresh in-memory database, IDs asserted as preconditions):
 * <ul>
 *   <li>farmers.id 1 = farmer A (mobile login, no core user)</li>
 *   <li>users.id 1 = farmer B core user, linked to farmers.id 2</li>
 *   <li>users.id 2 = legacy users-table OPERATOR; operators.id 2 = real approved operator</li>
 *   <li>users.id 3 = legacy users-table PARTNER; partners.id 3 = real approved partner</li>
 *   <li>users.id 4 = core ADMIN user; admins.id 1 = admins-table admin</li>
 * </ul>
 */
@SpringBootTest
// Imported (not a nested @TestConfiguration) so every @Nested class shares one identical context
@Import(P03EmailCaptureConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:p03_identity_isolation_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=" + AccountIdentityIsolationIntegrationTest.SECRET,
    "server.port=0"
})
@DisplayName("P0-3: Account identity isolation (registration, login, JWT account type)")
class AccountIdentityIsolationIntegrationTest {

    static final String SECRET = "p03-test-secret-key-32-chars-minimum-length-hmac";
    private static final String PASSWORD = "Correct-Horse-1";

    @Autowired private CapturingPasswordResetDelivery emailService;

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private OperatorRepository operatorRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;

    private Farmer farmerA;
    private Farmer farmerB;
    private User farmerBUser;
    private Operator victimOperator;
    private User legacyOperatorUser;
    private Partner victimPartner;
    private User legacyPartnerUser;
    private Admin tableAdmin;
    private User coreAdmin;

    @BeforeAll
    void createOverlappingAccounts() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        String hash = passwordEncoder.encode(PASSWORD);

        farmerA = farmerRepository.save(Farmer.builder()
                .fullName("Farmer A").mobileNumber("9800000001").email("farmer.a@p03.test")
                .password(hash).accountStatus("ACTIVE").build());

        farmerBUser = User.builder().name("Farmer B").email("farmer.b@p03.test").password(hash)
                .role(Role.FARMER).enabled(true).verified(true).build();
        Farmer farmerBProfile = Farmer.builder().user(farmerBUser)
                .fullName("Farmer B").mobileNumber("9800000002").email("farmer.b@p03.test")
                .password(hash).accountStatus("ACTIVE").build();
        farmerBUser.setFarmer(farmerBProfile);
        farmerBUser = userRepository.save(farmerBUser);
        farmerB = farmerRepository.findByMobileNumber("9800000002").orElseThrow();

        operatorRepository.save(operator("Filler Operator", "9700000001", "filler.operator@p03.test", hash, OperatorStatus.PENDING));
        victimOperator = operatorRepository.save(
                operator("Victim Operator", "9700000002", "operator.victim@p03.test", hash, OperatorStatus.APPROVED));
        legacyOperatorUser = userRepository.save(User.builder().name("Legacy Operator").email("legacy.operator@p03.test")
                .password(hash).role(Role.OPERATOR).enabled(true).verified(true).build());

        partnerRepository.save(partner("Filler Partner 1", "9600000001", "filler1.partner@p03.test", hash, Partner.VerificationStatus.PENDING));
        partnerRepository.save(partner("Filler Partner 2", "9600000002", "filler2.partner@p03.test", hash, Partner.VerificationStatus.PENDING));
        victimPartner = partnerRepository.save(
                partner("Victim Partner", "9600000003", "partner.victim@p03.test", hash, Partner.VerificationStatus.APPROVED));
        legacyPartnerUser = userRepository.save(User.builder().name("Legacy Partner").email("legacy.partner@p03.test")
                .password(hash).role(Role.PARTNER).enabled(true).verified(true).build());

        tableAdmin = adminRepository.save(Admin.builder().fullName("Table Admin").email("admin.table@p03.test").password(hash).build());
        coreAdmin = userRepository.save(User.builder().name("Core Admin").email("core.admin@p03.test")
                .password(hash).role(Role.ADMIN).enabled(true).verified(true).build());

        // Preconditions: the collisions this suite relies on really exist
        assertThat(farmerA.getId()).isEqualTo(farmerBUser.getId());
        assertThat(farmerB.getId()).isNotEqualTo(farmerBUser.getId());
        assertThat(legacyOperatorUser.getId()).isEqualTo(victimOperator.getId());
        assertThat(legacyPartnerUser.getId()).isEqualTo(victimPartner.getId());
    }

    private static Operator operator(String name, String mobile, String email, String hash, OperatorStatus status) {
        Operator operator = new Operator();
        operator.setFullName(name);
        operator.setMobileNumber(mobile);
        operator.setEmail(email);
        operator.setAddress("Pune");
        operator.setAadhaarNumber("123412341234");
        operator.setDrivingLicenseNumber("MH-12-0000-" + mobile.substring(6));
        operator.setExperience(3);
        operator.setSkills("Tractor");
        operator.setPassword(hash);
        operator.setStatus(status);
        operator.setMobileVerified(true);
        return operator;
    }

    private static Partner partner(String name, String mobile, String email, String hash, Partner.VerificationStatus status) {
        return Partner.builder().fullName(name).businessName(name + " Rentals").mobileNumber(mobile).email(email)
                .password(hash).otpVerified(true).verificationStatus(status).build();
    }

    // ---- helpers -------------------------------------------------------------------------

    private String loginJson(String identifier, String role) {
        return "{\"mobileOrEmail\":\"" + identifier + "\",\"password\":\"" + PASSWORD + "\""
                + (role != null ? ",\"role\":\"" + role + "\"" : "") + ",\"loginType\":\"PASSWORD\"}";
    }

    private String login(String identifier, String role) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(identifier, role)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /** Signs arbitrary claims with the test secret, exactly like JwtService (HS256). */
    private static String signedToken(String payloadJson) throws Exception {
        String data = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}") + "." + b64(payloadJson);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return data + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static String claims(String sub, String role, String acct) {
        long now = Instant.now().getEpochSecond();
        return "{\"sub\":\"" + sub + "\",\"role\":\"" + role + "\""
                + (acct != null ? ",\"acct\":\"" + acct + "\"" : "")
                + ",\"iat\":" + now + ",\"exp\":" + (now + 600) + "}";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---- 1. Registration -----------------------------------------------------------------

    @Nested
    @DisplayName("1. Public registration cannot create privileged accounts")
    class Registration {

        @ParameterizedTest(name = "role \"{0}\" is rejected and nothing is created")
        @ValueSource(strings = {"PARTNER", "OPERATOR", "ADMIN", "partner", "Operator", "admin", "SUPERUSER", "PARTNER "})
        void privilegedOrUnexpectedRolesAreRejected(String role) throws Exception {
            long users = userRepository.count();
            long partners = partnerRepository.count();
            long operators = operatorRepository.count();
            long admins = adminRepository.count();
            String email = "reg." + Math.abs(role.hashCode()) + "@p03.test";

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Attacker\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                    + "\",\"role\":\"" + role + "\"}"))
                    .andExpect(status().is4xxClientError());

            assertThat(userRepository.count()).isEqualTo(users);
            assertThat(userRepository.findByEmail(email)).isEmpty();
            assertThat(partnerRepository.count()).isEqualTo(partners);
            assertThat(operatorRepository.count()).isEqualTo(operators);
            assertThat(adminRepository.count()).isEqualTo(admins);
        }

        @Test
        @DisplayName("FARMER (explicit or default) self-registration still works")
        void farmerRegistrationStillWorks() throws Exception {
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Farmer\",\"email\":\"new.farmer@p03.test\",\"password\":\"" + PASSWORD
                                    + "\",\"role\":\"FARMER\",\"mobileNumber\":\"9800000091\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.role").value("FARMER"));
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Default Farmer\",\"email\":\"default.farmer@p03.test\",\"password\":\"" + PASSWORD
                                    + "\",\"mobileNumber\":\"9800000092\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.role").value("FARMER"));
        }
    }

    // ---- 2. Legacy users-table partner/operator accounts -----------------------------------

    @Nested
    @DisplayName("2. users-table PARTNER/OPERATOR accounts cannot authenticate")
    class LegacyCoreUserAccounts {

        @Test
        @DisplayName("Legacy users-table OPERATOR (same ID as a real operator) cannot log in as OPERATOR")
        void legacyOperatorUserCannotLogin() throws Exception {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("legacy.operator@p03.test", "OPERATOR")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.token").doesNotExist());
        }

        @Test
        @DisplayName("Legacy users-table PARTNER (same ID as a real partner) cannot log in as PARTNER")
        void legacyPartnerUserCannotLogin() throws Exception {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("legacy.partner@p03.test", "PARTNER")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.token").doesNotExist());
        }

        @Test
        @DisplayName("Legacy users-table accounts cannot log in through the role-less legacy path either")
        void legacyAccountsCannotUseRolelessLogin() throws Exception {
            for (String email : new String[]{"legacy.operator@p03.test", "legacy.partner@p03.test"}) {
                mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(email, null)))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    // ---- 3. Account-type claim enforcement ----------------------------------------------------

    @Nested
    @DisplayName("3. JWT account type selects exactly one table and cannot be substituted")
    class AccountTypeClaim {

        @Test
        @DisplayName("Control: a correctly typed OPERATOR token for the real operator is accepted")
        void correctlyTypedOperatorTokenAccepted() throws Exception {
            String token = signedToken(claims(String.valueOf(victimOperator.getId()), "OPERATOR", "OPERATOR"));
            mockMvc.perform(get("/api/operators/me").header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.mobileNumber").value("9700000002"));
        }

        @ParameterizedTest(name = "OPERATOR token with acct={0} is rejected")
        @ValueSource(strings = {"<missing>", "USER", "PARTNER", "FARMER", "ADMIN", "operator", "OPERATOR ", "", "SUPERUSER"})
        @DisplayName("Missing, unsupported or mismatched account types never authenticate")
        void missingOrMismatchedAccountTypeRejected(String acct) throws Exception {
            String token = signedToken(claims(String.valueOf(victimOperator.getId()), "OPERATOR",
                    "<missing>".equals(acct) ? null : acct));
            mockMvc.perform(get("/api/operators/me").header("Authorization", bearer(token)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("A users-table ADMIN-typed token for a users row that is not ADMIN is rejected")
        void userTypedTokenForNonAdminUserRejected() throws Exception {
            // users.id of the legacy OPERATOR user equals operators.id of the real operator
            String token = signedToken(claims(String.valueOf(legacyOperatorUser.getId()), "ADMIN", "USER"));
            mockMvc.perform(get("/api/admin/dashboard/stats").header("Authorization", bearer(token)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Tampering with acct/role/sub of a valid token without re-signing is rejected")
        void tamperedClaimsRejected() throws Exception {
            String farmerToken = login("9800000001", "FARMER");
            String[] parts = farmerToken.split("\\.");
            String forgedPayload = b64(claims(String.valueOf(victimOperator.getId()), "OPERATOR", "OPERATOR"));
            String forged = parts[0] + "." + forgedPayload + "." + parts[2];

            mockMvc.perform(get("/api/operators/me").header("Authorization", bearer(forged)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token issuance refuses missing or role-inconsistent account types")
        void issuanceFailsClosed() {
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    jwtService.generateToken(1L, "OPERATOR", "9700000002", new java.util.HashMap<>()))
                    .isInstanceOf(IllegalArgumentException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    jwtService.generateToken(1L, "OPERATOR", "9700000002", JwtService.accountTypeClaims(AccountType.USER)))
                    .isInstanceOf(IllegalArgumentException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    jwtService.generateToken(1L, "PARTNER", "9600000003", JwtService.accountTypeClaims(AccountType.ADMIN)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Malformed, expired and non-numeric-subject tokens fail closed")
        void malformedTokensFailClosed() throws Exception {
            long past = Instant.now().getEpochSecond() - 3600;
            String expired = signedToken("{\"sub\":\"" + victimOperator.getId()
                    + "\",\"role\":\"OPERATOR\",\"acct\":\"OPERATOR\",\"iat\":" + (past - 60) + ",\"exp\":" + past + "}");
            String nonNumeric = signedToken(claims("abc", "OPERATOR", "OPERATOR"));
            for (String token : new String[]{"garbage", "a.b.c", "a.b", expired, nonNumeric}) {
                mockMvc.perform(get("/api/operators/me").header("Authorization", bearer(token)))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    // ---- 5. Password recovery cannot cross account tables ----------------------------------------

    @Nested
    @DisplayName("5. Operator password recovery is bound to the operators table")
    class PasswordRecovery {

        @Test
        @DisplayName("Legacy users-table OPERATOR gets no reset token, and the real operator with the same ID is untouched")
        void legacyOperatorUserCannotObtainOperatorResetToken() throws Exception {
            String victimHashBefore = operatorRepository.findById(victimOperator.getId()).orElseThrow().getPassword();
            emailService.resetRecipients.clear();

            mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"OPERATOR\",\"email\":\"legacy.operator@p03.test\"}"))
                    .andExpect(status().isOk());

            assertThat(emailService.resetRecipients).isEmpty();
            assertThat(operatorRepository.findById(victimOperator.getId()).orElseThrow().getPassword())
                    .isEqualTo(victimHashBefore);
        }

        @Test
        @DisplayName("Control: the real operator still receives a reset token")
        void realOperatorStillReceivesResetToken() throws Exception {
            emailService.resetRecipients.clear();

            mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"OPERATOR\",\"email\":\"operator.victim@p03.test\"}"))
                    .andExpect(status().isOk());

            assertThat(emailService.resetRecipients).containsExactly("OPERATOR:operator.victim@p03.test");
        }
    }

    // ---- 4. Overlapping IDs resolve to the right account ----------------------------------------

    @Nested
    @DisplayName("4. Legitimate logins for all four roles resolve to the correct account")
    class LegitimateFlows {

        @Test
        @DisplayName("Farmer A (mobile login, farmers.id = users.id of farmer B) is resolved as farmer A")
        void farmerMobileLoginResolvesFarmersTable() throws Exception {
            String token = login("9800000001", "FARMER");
            assertThat(jwtService.extractAccountType(token)).isEqualTo("FARMER");
            assertThat(jwtService.extractUserId(token)).isEqualTo(farmerA.getId());

            mockMvc.perform(get("/api/farmers/profile").header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.mobileNumber").value("9800000001"));
        }

        @Test
        @DisplayName("Farmer B (email login via users table) gets a token bound to farmers.id, not users.id")
        void farmerEmailLoginBindsFarmerId() throws Exception {
            String token = login("farmer.b@p03.test", "FARMER");
            assertThat(jwtService.extractAccountType(token)).isEqualTo("FARMER");
            assertThat(jwtService.extractUserId(token)).isEqualTo(farmerB.getId());

            mockMvc.perform(get("/api/farmers/profile").header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.mobileNumber").value("9800000002"));
        }

        @Test
        @DisplayName("Real operator logs in and is resolved from the operators table")
        void operatorLogin() throws Exception {
            String token = login("operator.victim@p03.test", "OPERATOR");
            assertThat(jwtService.extractAccountType(token)).isEqualTo("OPERATOR");

            mockMvc.perform(get("/api/operators/me").header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.mobileNumber").value("9700000002"));
        }

        @Test
        @DisplayName("Real partner logs in and is resolved from the partners table")
        void partnerLogin() throws Exception {
            String token = login("partner.victim@p03.test", "PARTNER");
            assertThat(jwtService.extractAccountType(token)).isEqualTo("PARTNER");

            mockMvc.perform(get("/api/partners/" + victimPartner.getId()).header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.mobileNumber").value("9600000003"));
        }

        @Test
        @DisplayName("Admins-table admin and core-user admin both reach admin endpoints with distinct account types")
        void adminLogins() throws Exception {
            String tableAdminToken = login("admin.table@p03.test", "ADMIN");
            assertThat(jwtService.extractAccountType(tableAdminToken)).isEqualTo("ADMIN");
            assertThat(jwtService.extractUserId(tableAdminToken)).isEqualTo(tableAdmin.getId());
            mockMvc.perform(get("/api/admin/dashboard/stats").header("Authorization", bearer(tableAdminToken)))
                    .andExpect(status().isOk());

            String coreAdminToken = login("core.admin@p03.test", "ADMIN");
            assertThat(jwtService.extractAccountType(coreAdminToken)).isEqualTo("USER");
            assertThat(jwtService.extractUserId(coreAdminToken)).isEqualTo(coreAdmin.getId());
            mockMvc.perform(get("/api/admin/dashboard/stats").header("Authorization", bearer(coreAdminToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Role boundaries still hold: farmer cannot reach admin, operator cannot read a partner")
        void roleBoundariesHold() throws Exception {
            String farmerToken = login("9800000001", "FARMER");
            mockMvc.perform(get("/api/admin/dashboard/stats").header("Authorization", bearer(farmerToken)))
                    .andExpect(status().isForbidden());

            String operatorToken = login("operator.victim@p03.test", "OPERATOR");
            mockMvc.perform(get("/api/partners/" + victimPartner.getId()).header("Authorization", bearer(operatorToken)))
                    .andExpect(status().isForbidden());
        }
    }
}

/** A configured reset delivery that records recipients instead of sending anything. */
class CapturingPasswordResetDelivery implements PasswordResetDelivery {
    final java.util.List<String> resetRecipients = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public void sendPasswordReset(PasswordResetInstructions instructions) {
        resetRecipients.add(instructions.accountType() + ":" + instructions.email());
    }
}

@TestConfiguration
class P03EmailCaptureConfig {
    @Bean
    @Primary
    CapturingPasswordResetDelivery capturingPasswordResetDelivery() {
        return new CapturingPasswordResetDelivery();
    }
}
