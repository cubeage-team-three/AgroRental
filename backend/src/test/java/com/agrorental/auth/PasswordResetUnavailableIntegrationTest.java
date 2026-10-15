package com.agrorental.auth;

import com.agrorental.auth.repository.AccountPasswordResetTokenRepository;
import com.agrorental.auth.service.PasswordResetRateLimitedException;
import com.agrorental.auth.service.PasswordResetRateLimiter;
import com.agrorental.auth.service.PasswordResetService;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Auth contract 2.4 in today's configuration: no email/SMS provider and no development token disclosure.
 * Forgot-password honestly reports that reset is unavailable, identically for every identifier, and issues
 * no token at all.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth_password_reset_unavailable_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=auth-password-reset-unavailable-secret-32ch",
    "server.port=0"
})
@DisplayName("Auth contract 2.4: no delivery configured")
class PasswordResetUnavailableIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private PasswordResetRateLimiter rateLimiter;
    @Autowired private AccountPasswordResetTokenRepository tokenRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private PartnerRepository partnerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeAll
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        String hash = passwordEncoder.encode("Existing-Pass-1");
        farmerRepository.save(Farmer.builder().fullName("Existing Farmer").mobileNumber("9850000001")
                .email("existing.farmer@unavailable.test").password(hash).accountStatus("ACTIVE").build());
        partnerRepository.save(Partner.builder().fullName("Existing Partner").mobileNumber("9650000001")
                .email("existing.partner@unavailable.test").password(hash)
                .verificationStatus(Partner.VerificationStatus.APPROVED).build());
    }

    @BeforeEach
    void clearLimits() {
        rateLimiter.clear();
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

    private JsonNode bodyWithoutTimestamp(MvcResult result) throws Exception {
        ObjectNode node = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        node.remove("timestamp");
        return node;
    }

    @Test
    @DisplayName("Honest unavailable message, identical for existing and unknown accounts, and no token is created")
    void unavailableAndNonEnumerating() throws Exception {
        assertThat(passwordResetService.isDeliveryConfigured()).isFalse();
        assertThat(passwordResetService.isDevTokenDisclosureAllowed()).isFalse();

        JsonNode reference = null;
        int client = 0;
        for (String[] c : new String[][]{
                {"FARMER", "9850000001"}, {"FARMER", "existing.farmer@unavailable.test"}, {"FARMER", "9859999999"},
                {"PARTNER", "9650000001"}, {"PARTNER", "nobody@unavailable.test"}, {"OPERATOR", "9759999999"}}) {
            MvcResult result = forgot(c[0], c[1], "10.30.0." + (++client));
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat((String) JsonPath.read(result.getResponse().getContentAsString(), "$.message"))
                    .isEqualTo(PasswordResetService.DELIVERY_UNAVAILABLE_MESSAGE);
            JsonNode body = bodyWithoutTimestamp(result);
            assertThat(body.path("data").path("deliveryConfigured").asBoolean(true)).isFalse();
            assertThat(body.path("data").has("devResetToken")).isFalse();
            if (reference == null) {
                reference = body;
            }
            assertThat(body).as("%s %s", c[0], c[1]).isEqualTo(reference);
        }
        assertThat(tokenRepository.count()).as("no reset token is issued when nothing could deliver it").isZero();
    }

    @Test
    @DisplayName("Reset with any token fails with the generic invalid-token message")
    void resetFailsGenerically() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + "B".repeat(43) + "\",\"newPassword\":\"Brand-New-1\",\"confirmPassword\":\"Brand-New-1\"}"))
                .andExpect(result -> {
                    assertThat(result.getResponse().getStatus()).isEqualTo(400);
                    assertThat((String) JsonPath.read(result.getResponse().getContentAsString(), "$.message"))
                            .isEqualTo(PasswordResetService.INVALID_TOKEN_MESSAGE);
                });
    }

    @Test
    @DisplayName("Rate limits apply even when reset is unavailable, with the same 429 for existing and unknown accounts")
    void rateLimitedIdentically() throws Exception {
        MvcResult existing = null;
        MvcResult unknown = null;
        for (int i = 0; i < 6; i++) {
            existing = forgot("FARMER", "9850000001", "10.31.0." + i);
            unknown = forgot("FARMER", "9859999998", "10.32.0." + i);
        }
        assertThat(existing.getResponse().getStatus()).isEqualTo(429);
        assertThat(unknown.getResponse().getStatus()).isEqualTo(429);
        assertThat(bodyWithoutTimestamp(existing)).isEqualTo(bodyWithoutTimestamp(unknown));
        assertThat((String) JsonPath.read(existing.getResponse().getContentAsString(), "$.message"))
                .isEqualTo(PasswordResetRateLimitedException.MESSAGE);
    }
}
