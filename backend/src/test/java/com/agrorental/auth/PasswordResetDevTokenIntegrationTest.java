package com.agrorental.auth;

import com.agrorental.auth.repository.AccountPasswordResetTokenRepository;
import com.agrorental.auth.service.PasswordResetRateLimiter;
import com.agrorental.auth.service.PasswordResetService;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * DEVELOPMENT ONLY token disclosure: with an explicitly active 'dev' profile AND
 * app.password-reset.dev-token-in-response=true the forgot-password body carries the reset token for a
 * matching account (clearly labelled), so the reset flow can be exercised without a delivery provider.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth_password_reset_dev_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=auth-password-reset-dev-token-secret-32chars",
    "app.password-reset.dev-token-in-response=true",
    "server.port=0"
})
@DisplayName("Auth contract 2.4: dev-only reset token disclosure (explicit dev profile + opt-in)")
class PasswordResetDevTokenIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private PasswordResetRateLimiter rateLimiter;
    @Autowired private AccountPasswordResetTokenRepository tokenRepository;
    @Autowired private OperatorRepository operatorRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private Operator operator;

    @BeforeAll
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        operator = operatorRepository.save(Operator.builder().fullName("Dev Operator").mobileNumber("9760000001")
                .email("dev.operator@dev.test").address("Pune").aadhaarNumber("123412341234")
                .drivingLicenseNumber("MH12-DEV").experience(2).skills("Tractor")
                .password(passwordEncoder.encode("Dev-Old-Pass-1")).status(OperatorStatus.PENDING).build());
    }

    @BeforeEach
    void clearLimits() {
        rateLimiter.clear();
    }

    @AfterEach
    void restoreFlag() {
        passwordResetService.setDevTokenInResponse(true);
    }

    private JsonNode forgot(String identifier, String client) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/forgot-password")
                        .with(request -> {
                            request.setRemoteAddr(client);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"OPERATOR\",\"identifier\":\"" + identifier + "\"}"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Existing account: labelled dev-only token is returned, hashed at rest, and resets the password")
    void devTokenForExistingAccount() throws Exception {
        assertThat(passwordResetService.isDevTokenDisclosureAllowed()).isTrue();
        JsonNode body = forgot("9760000001", "10.40.0.1");

        JsonNode data = body.path("data");
        String token = data.path("devResetToken").asText();
        assertThat(token).matches("^[A-Za-z0-9_-]{43}$");
        assertThat(data.path("devNotice").asText()).isEqualTo(PasswordResetService.DEV_TOKEN_NOTICE).contains("DEVELOPMENT ONLY");
        assertThat(data.path("deliveryConfigured").asBoolean(true)).isFalse();
        assertThat(body.path("message").asText()).isEqualTo(PasswordResetService.DEV_TOKEN_MESSAGE);
        assertThat(tokenRepository.findByTokenHash(PasswordResetService.sha256Hex(token))).isPresent();

        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"Dev-New-Pass-2\",\"confirmPassword\":\"Dev-New-Pass-2\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));
        assertThat(passwordEncoder.matches("Dev-New-Pass-2",
                operatorRepository.findById(operator.getId()).orElseThrow().getPassword())).isTrue();
        // a reset never approves anyone
        assertThat(operatorRepository.findById(operator.getId()).orElseThrow().getStatus()).isEqualTo(OperatorStatus.PENDING);
    }

    @Test
    @DisplayName("Unknown account: same notice and message, but no token (dev-only behaviour, documented)")
    void noTokenForUnknownAccount() throws Exception {
        JsonNode body = forgot("9769999999", "10.40.0.2");
        assertThat(body.path("data").has("devResetToken")).isFalse();
        assertThat(body.path("data").path("devNotice").asText()).isEqualTo(PasswordResetService.DEV_TOKEN_NOTICE);
        assertThat(body.path("message").asText()).isEqualTo(PasswordResetService.DEV_TOKEN_MESSAGE);
    }

    @Test
    @DisplayName("Explicit dev profile without the opt-in flag: no token, honest unavailable message")
    void devProfileWithoutFlag() throws Exception {
        passwordResetService.setDevTokenInResponse(false);
        long tokensBefore = tokenRepository.count();
        JsonNode body = forgot("9760000001", "10.40.0.3");
        assertThat(body.path("data").has("devResetToken")).isFalse();
        assertThat(body.path("data").has("devNotice")).isFalse();
        assertThat(body.path("message").asText()).isEqualTo(PasswordResetService.DELIVERY_UNAVAILABLE_MESSAGE);
        assertThat(tokenRepository.count()).isEqualTo(tokensBefore);
    }
}
