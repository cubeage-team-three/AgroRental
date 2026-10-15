package com.agrorental.auth;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.auth.dto.ResetPasswordRequest;
import com.agrorental.auth.entity.AccountPasswordResetToken;
import com.agrorental.auth.entity.PasswordResetAccountType;
import com.agrorental.auth.repository.AccountPasswordResetTokenRepository;
import com.agrorental.auth.service.PasswordPolicy;
import com.agrorental.auth.service.PasswordResetDelivery;
import com.agrorental.auth.service.PasswordResetInstructions;
import com.agrorental.auth.service.PasswordResetRateLimitedException;
import com.agrorental.auth.service.PasswordResetRateLimiter;
import com.agrorental.auth.service.PasswordResetService;
import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Auth contract 2.4 with an email/SMS delivery provider configured (a capturing stand-in): non-enumerating
 * forgot-password, SHA-256 hashed single-use tokens, expiry, invalidation, atomic redemption under
 * concurrent double submit, account-table isolation, rate limits and secret-free logs.
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@Import(PasswordResetCapturingDeliveryConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth_password_reset_flow_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=auth-password-reset-flow-secret-32-chars-min",
    "app.frontend.url=https://app.agrorent.test/",
    "app.password-reset.dev-token-in-response=true",
    "server.port=0"
})
@DisplayName("Auth contract 2.4: forgot / reset password with delivery configured")
class PasswordResetFlowIntegrationTest {

    private static final String OLD_PASSWORD = "Old-Pass-2025";
    private static final String NEW_PASSWORD = "New-Pass-2026";

    @Autowired private WebApplicationContext context;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private PasswordResetRateLimiter rateLimiter;
    @Autowired private PasswordResetCapturingDelivery delivery;
    @Autowired private AccountPasswordResetTokenRepository tokenRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private OperatorRepository operatorRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private int clientCounter;

    private Farmer farmer;
    private Partner partner;
    private Operator operator;
    private Admin admin;

    @BeforeAll
    void createAccounts() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        String hash = passwordEncoder.encode(OLD_PASSWORD);

        // Fresh tables: farmers.id, partners.id, operators.id and admins.id all start at 1, so a reset that
        // wrote to the wrong table would hit an account with the same numeric ID.
        User farmerUser = User.builder().name("Reset Farmer").email("reset.farmer@reset.test").password(hash)
                .role(Role.FARMER).enabled(true).verified(true).build();
        Farmer farmerProfile = Farmer.builder().user(farmerUser).fullName("Reset Farmer").mobileNumber("9830000001")
                .email("reset.farmer@reset.test").password(hash).accountStatus("ACTIVE").build();
        farmerUser.setFarmer(farmerProfile);
        userRepository.save(farmerUser);
        farmer = farmerRepository.findByMobileNumber("9830000001").orElseThrow();

        partner = partnerRepository.save(Partner.builder().fullName("Reset Partner").mobileNumber("9630000001")
                .email("reset.partner@reset.test").password(hash).verificationStatus(Partner.VerificationStatus.APPROVED).build());
        operator = operatorRepository.save(Operator.builder().fullName("Reset Operator").mobileNumber("9730000001")
                .email("reset.operator@reset.test").address("Pune").aadhaarNumber("123412341234")
                .drivingLicenseNumber("MH12-RESET").experience(2).skills("Tractor").password(hash)
                .status(OperatorStatus.APPROVED).build());
        admin = adminRepository.save(Admin.builder().fullName("Reset Admin").email("reset.admin@reset.test").password(hash).build());

        assertThat(farmer.getId()).isEqualTo(partner.getId()).isEqualTo(operator.getId()).isEqualTo(admin.getId());
    }

    @BeforeEach
    void resetState() {
        rateLimiter.clear();
        delivery.sent.clear();
        passwordResetService.setDevTokenInResponse(true);
    }

    // ---- helpers ------------------------------------------------------------------------------

    private String nextClient() {
        return "10.20.0." + (++clientCounter);
    }

    private MvcResult forgot(String role, String identifier, String client) throws Exception {
        return mockMvc.perform(post("/api/auth/forgot-password")
                        .with(request -> {
                            request.setRemoteAddr(client);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"" + role + "\",\"identifier\":\"" + identifier + "\"}"))
                .andReturn();
    }

    private MvcResult reset(String token, String password, String role) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("token", token);
        body.put("newPassword", password);
        body.put("confirmPassword", password);
        if (role != null) {
            body.put("role", role);
        }
        return mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    private MvcResult login(String identifier, String password, String role) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileOrEmail\":\"" + identifier + "\",\"password\":\"" + password + "\",\"role\":\"" + role + "\"}"))
                .andReturn();
    }

    private JsonNode bodyWithoutTimestamp(MvcResult result) throws Exception {
        ObjectNode node = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        node.remove("timestamp");
        return node;
    }

    /** Requests a reset for an existing account and returns the token from the delivered link. */
    private String requestToken(String role, String identifier) throws Exception {
        delivery.sent.clear();
        MvcResult result = forgot(role, identifier, nextClient());
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(delivery.sent).hasSize(1);
        String url = delivery.sent.get(0).resetUrl();
        assertThat(url).startsWith("https://app.agrorent.test/reset-password?token=");
        String token = url.substring(url.indexOf("token=") + "token=".length());
        // The opt-in flag is set in this context, but the 'dev' profile is only the implicit default: never disclosed
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.has("devResetToken")).as("token must not be disclosed without an explicit dev profile").isFalse();
        assertThat(data.has("devNotice")).isFalse();
        return token;
    }

    private String message(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.message");
    }

    // ---- forgot password --------------------------------------------------------------------------

    @Test
    @DisplayName("Same status and body for existing and non-existing accounts (FARMER/PARTNER/OPERATOR, mobile and email)")
    void forgotPasswordDoesNotEnumerate() throws Exception {
        String[][] cases = {
                {"FARMER", "9830000001", "9839999999"},
                {"FARMER", "reset.farmer@reset.test", "nobody.farmer@reset.test"},
                {"PARTNER", "9630000001", "9639999999"},
                {"PARTNER", "reset.partner@reset.test", "nobody.partner@reset.test"},
                {"OPERATOR", "9730000001", "9739999999"},
                {"OPERATOR", "reset.operator@reset.test", "nobody.operator@reset.test"},
                // an account of another role never matches
                {"FARMER", "9630000001", "9639999998"},
        };
        for (String[] c : cases) {
            MvcResult existing = forgot(c[0], c[1], nextClient());
            MvcResult missing = forgot(c[0], c[2], nextClient());
            assertThat(existing.getResponse().getStatus()).isEqualTo(200);
            assertThat(missing.getResponse().getStatus()).isEqualTo(200);
            assertThat(bodyWithoutTimestamp(existing)).as("%s %s", c[0], c[1]).isEqualTo(bodyWithoutTimestamp(missing));
            assertThat(message(existing)).isEqualTo(PasswordResetService.DELIVERY_CONFIGURED_MESSAGE);
            assertThat(existing.getResponse().getContentAsString()).doesNotContain("devResetToken");
        }
        // Deliveries only happened for real accounts of the requested role
        assertThat(delivery.sent).extracting(PasswordResetInstructions::accountType)
                .containsExactly(PasswordResetAccountType.FARMER, PasswordResetAccountType.FARMER,
                        PasswordResetAccountType.PARTNER, PasswordResetAccountType.PARTNER,
                        PasswordResetAccountType.OPERATOR, PasswordResetAccountType.OPERATOR);
    }

    @Test
    @DisplayName("Admins cannot use self-service reset; invalid roles and missing identifiers are rejected")
    void adminExcluded() throws Exception {
        long tokensBefore = tokenRepository.count();
        MvcResult adminRequest = forgot("ADMIN", "reset.admin@reset.test", nextClient());
        assertThat(adminRequest.getResponse().getStatus()).isEqualTo(400);
        assertThat(message(adminRequest)).isEqualTo(PasswordResetService.INVALID_ROLE_MESSAGE);
        assertThat(forgot("SUPERUSER", "x@reset.test", nextClient()).getResponse().getStatus()).isEqualTo(400);
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"FARMER\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(400));
        assertThat(delivery.sent).isEmpty();
        assertThat(tokenRepository.count()).isEqualTo(tokensBefore);
        // and the admin account cannot be logged into with anything but its unchanged password
        assertThat(login("reset.admin@reset.test", OLD_PASSWORD, "ADMIN").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Only the SHA-256 hash of the token is stored; the raw token appears in no column")
    void tokenIsHashedAtRest() throws Exception {
        String token = requestToken("PARTNER", "reset.partner@reset.test");
        assertThat(token).matches("^[A-Za-z0-9_-]{43}$");

        AccountPasswordResetToken stored = tokenRepository.findByTokenHash(PasswordResetService.sha256Hex(token)).orElseThrow();
        assertThat(stored.getAccountType()).isEqualTo(PasswordResetAccountType.PARTNER);
        assertThat(stored.getAccountId()).isEqualTo(partner.getId());
        assertThat(stored.getTokenHash()).matches("^[0-9a-f]{64}$").isNotEqualTo(token);
        assertThat(stored.getUsedAt()).isNull();
        assertThat(stored.getExpiresAt()).isBetween(LocalDateTime.now().plusMinutes(29), LocalDateTime.now().plusMinutes(31));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM account_password_reset_tokens");
        for (Map<String, Object> row : rows) {
            for (Object value : row.values()) {
                assertThat(String.valueOf(value)).doesNotContain(token);
            }
        }
    }

    // ---- reset password ---------------------------------------------------------------------------

    @Test
    @DisplayName("Farmer reset: old password stops working, new works, token is single use, only farmers.password changes")
    void farmerResetEndToEnd() throws Exception {
        String partnerHash = partnerRepository.findById(partner.getId()).orElseThrow().getPassword();
        String operatorHash = operatorRepository.findById(operator.getId()).orElseThrow().getPassword();
        String adminHash = adminRepository.findById(admin.getId()).orElseThrow().getPassword();
        String userHash = userRepository.findByEmail("reset.farmer@reset.test").orElseThrow().getPassword();

        String token = requestToken("FARMER", "9830000001");
        MvcResult reset = reset(token, NEW_PASSWORD, null);
        assertThat(reset.getResponse().getStatus()).as(message(reset)).isEqualTo(200);
        assertThat(message(reset)).isEqualTo(PasswordResetService.RESET_SUCCESS_MESSAGE);

        assertThat(login("9830000001", OLD_PASSWORD, "FARMER").getResponse().getStatus()).isEqualTo(400);
        assertThat(login("9830000001", NEW_PASSWORD, "FARMER").getResponse().getStatus()).isEqualTo(200);
        assertThat(login("reset.farmer@reset.test", NEW_PASSWORD, "FARMER").getResponse().getStatus()).isEqualTo(200);

        MvcResult reuse = reset(token, "Another-Pass-1", null);
        assertThat(reuse.getResponse().getStatus()).isEqualTo(400);
        assertThat(message(reuse)).isEqualTo(PasswordResetService.INVALID_TOKEN_MESSAGE);
        assertThat(login("9830000001", NEW_PASSWORD, "FARMER").getResponse().getStatus()).isEqualTo(200);

        // Same numeric ID in every account table: only the farmer row was written
        assertThat(partnerRepository.findById(partner.getId()).orElseThrow().getPassword()).isEqualTo(partnerHash);
        assertThat(operatorRepository.findById(operator.getId()).orElseThrow().getPassword()).isEqualTo(operatorHash);
        assertThat(adminRepository.findById(admin.getId()).orElseThrow().getPassword()).isEqualTo(adminHash);
        assertThat(userRepository.findByEmail("reset.farmer@reset.test").orElseThrow().getPassword()).isEqualTo(userHash);

        // restore for other tests
        restorePassword(PasswordResetAccountType.FARMER);
    }

    @Test
    @DisplayName("Partner and operator resets write only their own table and keep approval state")
    void partnerAndOperatorResets() throws Exception {
        String farmerHash = farmerRepository.findById(farmer.getId()).orElseThrow().getPassword();

        String partnerToken = requestToken("PARTNER", "9630000001");
        assertThat(reset(partnerToken, NEW_PASSWORD, "PARTNER").getResponse().getStatus()).isEqualTo(200);
        Partner updatedPartner = partnerRepository.findById(partner.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, updatedPartner.getPassword())).isTrue();
        assertThat(updatedPartner.getVerificationStatus()).isEqualTo(Partner.VerificationStatus.APPROVED);
        assertThat(login("9630000001", NEW_PASSWORD, "PARTNER").getResponse().getStatus()).isEqualTo(200);

        String operatorToken = requestToken("OPERATOR", "reset.operator@reset.test");
        assertThat(reset(operatorToken, NEW_PASSWORD, null).getResponse().getStatus()).isEqualTo(200);
        Operator updatedOperator = operatorRepository.findById(operator.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, updatedOperator.getPassword())).isTrue();
        assertThat(updatedOperator.getStatus()).isEqualTo(OperatorStatus.APPROVED);
        assertThat(login("9730000001", NEW_PASSWORD, "OPERATOR").getResponse().getStatus()).isEqualTo(200);

        assertThat(farmerRepository.findById(farmer.getId()).orElseThrow().getPassword()).isEqualTo(farmerHash);
        restorePassword(PasswordResetAccountType.PARTNER);
        restorePassword(PasswordResetAccountType.OPERATOR);
    }

    @Test
    @DisplayName("A role that does not match the token fails like an invalid token and does not burn it")
    void crossRoleRedemptionRejected() throws Exception {
        String token = requestToken("PARTNER", "9630000001");
        for (String role : new String[]{"FARMER", "OPERATOR", "ADMIN"}) {
            MvcResult wrongRole = reset(token, NEW_PASSWORD, role);
            assertThat(wrongRole.getResponse().getStatus()).isEqualTo(400);
            assertThat(message(wrongRole)).isEqualTo(PasswordResetService.INVALID_TOKEN_MESSAGE);
        }
        assertThat(farmerRepository.findById(farmer.getId()).orElseThrow().getPassword()).satisfies(
                hash -> assertThat(passwordEncoder.matches(NEW_PASSWORD, hash)).isFalse());
        assertThat(reset(token, NEW_PASSWORD, "PARTNER").getResponse().getStatus()).isEqualTo(200);
        restorePassword(PasswordResetAccountType.PARTNER);
    }

    @Test
    @DisplayName("Unknown, malformed, expired and invalidated tokens all fail with the same message")
    void invalidTokensFailIdentically() throws Exception {
        String expiredToken = requestToken("OPERATOR", "9730000001");
        jdbcTemplate.update("UPDATE account_password_reset_tokens SET expires_at = ? WHERE token_hash = ?",
                LocalDateTime.now().minusSeconds(1), PasswordResetService.sha256Hex(expiredToken));

        String supersededToken = requestToken("FARMER", "9830000001");
        String latestToken = requestToken("FARMER", "9830000001");

        List<String> invalid = List.of(expiredToken, supersededToken, "A".repeat(43), "not a token!", "short",
                "x".repeat(500), latestToken + "x");
        List<JsonNode> bodies = new ArrayList<>();
        for (String token : invalid) {
            MvcResult result = reset(token, NEW_PASSWORD, null);
            assertThat(result.getResponse().getStatus()).as(token).isEqualTo(400);
            assertThat(message(result)).isEqualTo(PasswordResetService.INVALID_TOKEN_MESSAGE);
            bodies.add(bodyWithoutTimestamp(result));
        }
        assertThat(bodies).allMatch(body -> body.equals(bodies.get(0)));
        assertThat(login("9730000001", OLD_PASSWORD, "OPERATOR").getResponse().getStatus()).isEqualTo(200);

        // The newest token still works: a new request invalidated only the earlier ones
        assertThat(reset(latestToken, NEW_PASSWORD, null).getResponse().getStatus()).isEqualTo(200);
        restorePassword(PasswordResetAccountType.FARMER);
    }

    @Test
    @DisplayName("A completed reset invalidates the account's other outstanding tokens")
    void resetInvalidatesOtherOutstandingTokens() throws Exception {
        String token = requestToken("OPERATOR", "9730000001");
        AccountPasswordResetToken extra = tokenRepository.save(AccountPasswordResetToken.builder()
                .accountType(PasswordResetAccountType.OPERATOR).accountId(operator.getId())
                .tokenHash(PasswordResetService.sha256Hex("extra-outstanding-token-value-0123456789"))
                .expiresAt(LocalDateTime.now().plusMinutes(30)).build());

        assertThat(reset(token, NEW_PASSWORD, null).getResponse().getStatus()).isEqualTo(200);

        assertThat(tokenRepository.findById(extra.getId()).orElseThrow().getUsedAt()).isNotNull();
        assertThat(reset("extra-outstanding-token-value-0123456789", "Third-Pass-3", null).getResponse().getStatus()).isEqualTo(400);
        restorePassword(PasswordResetAccountType.OPERATOR);
    }

    @Test
    @DisplayName("New password must satisfy the policy and match the confirmation; a failed attempt does not burn the token")
    void passwordRulesOnReset() throws Exception {
        String token = requestToken("PARTNER", "reset.partner@reset.test");

        MvcResult weak = reset(token, "weakpass", null);
        assertThat(weak.getResponse().getStatus()).isEqualTo(400);
        assertThat(message(weak)).isEqualTo(PasswordPolicy.REQUIREMENTS_MESSAGE);

        MvcResult mismatch = mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + NEW_PASSWORD + "\",\"confirmPassword\":\"Different-1\"}"))
                .andReturn();
        assertThat(mismatch.getResponse().getStatus()).isEqualTo(400);
        assertThat(message(mismatch)).isEqualTo("New password and confirm password do not match.");

        assertThat(reset(token, NEW_PASSWORD, null).getResponse().getStatus()).isEqualTo(200);
        restorePassword(PasswordResetAccountType.PARTNER);
    }

    @Test
    @DisplayName("Concurrent double submit of one token: exactly one success, real threads, consistent final password")
    void concurrentRedemptionHasExactlyOneWinner() throws Exception {
        for (int round = 0; round < 3; round++) {
            String token = requestToken("FARMER", "9830000001");
            int threads = 6;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger successes = new AtomicInteger();
            List<String> winningPasswords = new CopyOnWriteArrayList<>();
            List<Future<?>> futures = new ArrayList<>();
            try {
                for (int i = 0; i < threads; i++) {
                    String candidate = "Racer-" + round + "-" + i + "-Pass1";
                    Callable<Void> attempt = () -> {
                        start.await();
                        try {
                            passwordResetService.resetPassword(ResetPasswordRequest.builder()
                                    .token(token).newPassword(candidate).confirmPassword(candidate).build());
                            successes.incrementAndGet();
                            winningPasswords.add(candidate);
                        } catch (BadRequestException e) {
                            assertThat(e.getMessage()).isEqualTo(PasswordResetService.INVALID_TOKEN_MESSAGE);
                        }
                        return null;
                    };
                    futures.add(executor.submit(attempt));
                }
                start.countDown();
                for (Future<?> future : futures) {
                    future.get(60, TimeUnit.SECONDS);
                }
            } finally {
                executor.shutdownNow();
            }
            assertThat(successes.get()).as("round %d", round).isEqualTo(1);
            String stored = farmerRepository.findById(farmer.getId()).orElseThrow().getPassword();
            assertThat(passwordEncoder.matches(winningPasswords.get(0), stored)).isTrue();
            assertThat(tokenRepository.findByTokenHash(PasswordResetService.sha256Hex(token)).orElseThrow().getUsedAt()).isNotNull();
        }
        restorePassword(PasswordResetAccountType.FARMER);
    }

    // ---- rate limits --------------------------------------------------------------------------------

    @Test
    @DisplayName("Per-identifier limit: the 6th request in the window gets 429 with an identical body for existing and unknown accounts")
    void perIdentifierRateLimit() throws Exception {
        List<MvcResult> limited = new ArrayList<>();
        for (String identifier : new String[]{"9630000001", "9639999990"}) {
            for (int i = 0; i < 5; i++) {
                assertThat(forgot("PARTNER", identifier, nextClient()).getResponse().getStatus()).isEqualTo(200);
            }
            // formatting variations of the same identifier share the bucket
            MvcResult sixth = forgot("PARTNER", "+91 " + identifier, nextClient());
            assertThat(sixth.getResponse().getStatus()).isEqualTo(429);
            assertThat(sixth.getResponse().getHeader("Retry-After")).isEqualTo("900");
            limited.add(sixth);
        }
        assertThat(message(limited.get(0))).isEqualTo(PasswordResetRateLimitedException.MESSAGE);
        assertThat(bodyWithoutTimestamp(limited.get(0))).isEqualTo(bodyWithoutTimestamp(limited.get(1)));
        // The limit is per role + identifier: the same mobile as another role is unaffected
        assertThat(forgot("FARMER", "9630000001", nextClient()).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Per-client limit: one address cannot spray many identifiers (429 after 20 in the window)")
    void perClientRateLimit() throws Exception {
        String client = nextClient();
        for (int i = 0; i < 20; i++) {
            assertThat(forgot("FARMER", "98400000" + String.format("%02d", i), client).getResponse().getStatus()).isEqualTo(200);
        }
        MvcResult blocked = forgot("FARMER", "9830000001", client);
        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
        assertThat(delivery.sent).isEmpty();
        assertThat(forgot("FARMER", "9830000001", nextClient()).getResponse().getStatus()).isEqualTo(200);
    }

    // ---- logs ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("Tokens, reset links and passwords never reach the logs")
    void noSecretsInLogs(CapturedOutput output) throws Exception {
        int start = output.getAll().length();
        String token = requestToken("OPERATOR", "9730000001");
        String resetUrl = delivery.sent.get(0).resetUrl();
        assertThat(reset(token, "Log-Check-Pass-7", null).getResponse().getStatus()).isEqualTo(200);
        assertThat(reset(token, "Log-Check-Pass-8", null).getResponse().getStatus()).isEqualTo(400);
        assertThat(reset("bogus-token-value-0123456789abcdef", "Log-Check-Pass-9", null).getResponse().getStatus()).isEqualTo(400);
        assertThat(login("9730000001", "Log-Check-Pass-7", "OPERATOR").getResponse().getStatus()).isEqualTo(200);
        assertThat(delivery.sent.get(0).toString()).doesNotContain(token);

        String logs = output.getAll().substring(start);
        assertThat(logs).contains("Password reset completed for OPERATOR account");
        assertThat(logs).doesNotContain(token)
                .doesNotContain(resetUrl)
                .doesNotContain(PasswordResetService.sha256Hex(token))
                .doesNotContain("Log-Check-Pass-7")
                .doesNotContain("Log-Check-Pass-8")
                .doesNotContain("bogus-token-value-0123456789abcdef");
        restorePassword(PasswordResetAccountType.OPERATOR);
    }

    private void restorePassword(PasswordResetAccountType type) {
        String hash = passwordEncoder.encode(OLD_PASSWORD);
        switch (type) {
            case FARMER -> {
                Farmer f = farmerRepository.findById(farmer.getId()).orElseThrow();
                f.setPassword(hash);
                farmerRepository.save(f);
            }
            case PARTNER -> {
                Partner p = partnerRepository.findById(partner.getId()).orElseThrow();
                p.setPassword(hash);
                partnerRepository.save(p);
            }
            case OPERATOR -> {
                Operator o = operatorRepository.findById(operator.getId()).orElseThrow();
                o.setPassword(hash);
                operatorRepository.save(o);
            }
        }
    }

    @Test
    @DisplayName("Service-level guard: rate limiting happens before any account lookup")
    void rateLimitCheckedBeforeLookup() {
        String client = nextClient();
        for (int i = 0; i < 5; i++) {
            passwordResetService.requestReset(forgotRequest("OPERATOR", "9739999911"), client);
        }
        assertThatThrownBy(() -> passwordResetService.requestReset(forgotRequest("OPERATOR", "9739999911"), client))
                .isInstanceOf(PasswordResetRateLimitedException.class);
    }

    private static com.agrorental.auth.dto.ForgotPasswordRequest forgotRequest(String role, String identifier) {
        return com.agrorental.auth.dto.ForgotPasswordRequest.builder().role(role).identifier(identifier).build();
    }
}

/** A configured provider that records what it would send instead of sending anything. */
class PasswordResetCapturingDelivery implements PasswordResetDelivery {
    final List<PasswordResetInstructions> sent = new CopyOnWriteArrayList<>();

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public void sendPasswordReset(PasswordResetInstructions instructions) {
        sent.add(instructions);
    }
}

@TestConfiguration
class PasswordResetCapturingDeliveryConfig {
    @Bean
    PasswordResetCapturingDelivery passwordResetCapturingDelivery() {
        return new PasswordResetCapturingDelivery();
    }
}
