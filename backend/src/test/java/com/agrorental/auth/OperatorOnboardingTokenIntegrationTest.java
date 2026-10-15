package com.agrorental.auth;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorDocumentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.jwt.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth contract 2.3: the onboarding token returned by operator registration lets the still-PENDING operator
 * upload its own KYC documents and nothing else; normal login still requires admin approval.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth_operator_onboarding_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=" + OperatorOnboardingTokenIntegrationTest.SECRET,
    "server.port=0"
})
@DisplayName("Auth contract 2.3: operator onboarding token is KYC-upload-only")
class OperatorOnboardingTokenIntegrationTest {

    static final String SECRET = "auth-operator-onboarding-secret-32-chars-min";
    private static final String PASSWORD = "Tractor2026";
    private static final String ADMIN_PASSWORD = "Admin-Pass-2026";

    @Autowired private WebApplicationContext context;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private OperatorRepository operatorRepository;
    @Autowired private OperatorDocumentRepository documentRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private AdminRepository adminRepository;

    private MockMvc mockMvc;
    private String adminToken;
    private Partner partner;
    private Farmer farmer;
    private Operator otherOperator;

    @BeforeAll
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        String hash = passwordEncoder.encode(PASSWORD);
        adminRepository.save(Admin.builder().fullName("Onboarding Admin").email("admin@onboarding.test")
                .password(passwordEncoder.encode(ADMIN_PASSWORD)).build());
        adminToken = JsonPath.read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileOrEmail\":\"admin@onboarding.test\",\"password\":\"" + ADMIN_PASSWORD + "\",\"role\":\"ADMIN\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.token");

        partner = partnerRepository.save(Partner.builder().fullName("Onb Partner").mobileNumber("9620000001")
                .email("partner@onboarding.test").password(hash).verificationStatus(Partner.VerificationStatus.APPROVED).build());
        farmer = farmerRepository.save(Farmer.builder().fullName("Onb Farmer").mobileNumber("9820000001")
                .email("farmer@onboarding.test").password(hash).accountStatus("ACTIVE").build());
        Operator other = Operator.builder().fullName("Other Operator").mobileNumber("9720000099").email("other@onboarding.test")
                .address("Pune").aadhaarNumber("123412341234").drivingLicenseNumber("MH12-OTHER").experience(2)
                .skills("Harvester").password(hash).status(OperatorStatus.PENDING).build();
        otherOperator = operatorRepository.save(other);
    }

    private record Registration(Long operatorId, String onboardingToken) {
    }

    private Registration register(String mobile) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/operators/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Onboarding Operator\",\"mobileNumber\":\"" + mobile + "\",\"email\":\"" + mobile
                                + "@onboarding.test\",\"address\":\"Pune\",\"aadhaarNumber\":\"123456789012\","
                                + "\"drivingLicenseNumber\":\"MH12-" + mobile + "\",\"experience\":4,\"skills\":\"Tractor\","
                                + "\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.data.id");
        return new Registration(id.longValue(), JsonPath.read(body, "$.data.onboardingToken"));
    }

    private static String document(String type) {
        return "{\"documentType\":\"" + type + "\",\"documentNumber\":\"123456789012\",\"fileName\":\"" + type.toLowerCase()
                + ".pdf\",\"fileUrl\":\"https://files.test/" + type.toLowerCase() + ".pdf\",\"fileSize\":1024,\"mimeType\":\"application/pdf\"}";
    }

    private MockHttpServletRequestBuilder upload(Long operatorId, String token, String type) {
        return post("/api/operators/" + operatorId + "/documents").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(document(type));
    }

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String payloadJson) throws Exception {
        String data = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}") + "." + b64(payloadJson);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return data + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Registration returns a short-lived onboarding token bound to operators.id that is not an operator session")
    void tokenShape() throws Exception {
        Registration registration = register("9720000001");
        String token = registration.onboardingToken();

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(registration.operatorId());
        assertThat(jwtService.extractAccountType(token)).isEqualTo("OPERATOR");
        assertThat(jwtService.extractRole(token)).isEqualTo("OPERATOR_ONBOARDING");
        assertThat(jwtService.extractScope(token)).isEqualTo("OPERATOR_ONBOARDING");
        long exp = jwtService.extractClaims(token).get("exp").asLong();
        assertThat(exp - Instant.now().getEpochSecond()).isPositive().isLessThanOrEqualTo(2 * 60 * 60);
    }

    @Test
    @DisplayName("The onboarding token uploads the operator's OWN KYC documents while it is PENDING")
    void uploadsOwnDocuments() throws Exception {
        Registration registration = register("9720000002");

        mockMvc.perform(upload(registration.operatorId(), registration.onboardingToken(), "AADHAAR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.documentType").value("AADHAAR"))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"));
        mockMvc.perform(upload(registration.operatorId(), registration.onboardingToken(), "DRIVING_LICENSE"))
                .andExpect(status().isCreated());

        assertThat(documentRepository.findByOperatorId(registration.operatorId())).hasSize(2);
        assertThat(operatorRepository.findById(registration.operatorId()).orElseThrow().getStatus()).isEqualTo(OperatorStatus.PENDING);
    }

    @Test
    @DisplayName("Everything else rejects it: other operators' documents, document reads, operator/partner/farmer/admin endpoints")
    void rejectedEverywhereElse() throws Exception {
        Registration registration = register("9720000003");
        String token = registration.onboardingToken();
        Long ownId = registration.operatorId();

        // another operator's documents
        mockMvc.perform(upload(otherOperator.getId(), token, "AADHAAR")).andExpect(status().isUnauthorized());
        assertThat(documentRepository.findByOperatorId(otherOperator.getId())).isEmpty();

        List<MockHttpServletRequestBuilder> forbidden = List.of(
                get("/api/operators/" + ownId + "/documents"),
                get("/api/operators/me"),
                get("/api/operators/profile"),
                patch("/api/operators/profile/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"Another2026\",\"confirmPassword\":\"Another2026\"}"),
                get("/api/operators/jobs/assigned"),
                get("/api/operators/jobs/history"),
                get("/api/operators/dashboard/metrics"),
                get("/api/operators/earnings/summary"),
                get("/api/operators/earnings/history"),
                get("/api/operators/me/ratings/summary"),
                get("/api/operators"),
                get("/api/operators/eligible"),
                get("/api/bookings/operator/" + ownId),
                get("/api/notifications"),
                get("/api/farmers/profile"),
                get("/api/partners/" + partner.getId()),
                get("/api/admin/dashboard/stats"),
                get("/api/admin/operators"),
                patch("/api/admin/operators/" + ownId + "/verify").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"),
                put("/api/partners/" + partner.getId() + "/kyc/approve"),
                post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{}"));
        for (MockHttpServletRequestBuilder request : forbidden) {
            MvcResult result = mockMvc.perform(request.header("Authorization", "Bearer " + token)).andReturn();
            assertThat(result.getResponse().getStatus())
                    .as("%s %s", result.getRequest().getMethod(), result.getRequest().getRequestURI())
                    .isIn(401, 403);
        }
        // nothing changed for the operator: still PENDING, password unchanged
        Operator stillPending = operatorRepository.findById(ownId).orElseThrow();
        assertThat(stillPending.getStatus()).isEqualTo(OperatorStatus.PENDING);
        assertThat(passwordEncoder.matches(PASSWORD, stillPending.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Approval policy unchanged: login needs APPROVED; after approval the onboarding token stops working and the session works")
    void approvalStillRequired() throws Exception {
        Registration registration = register("9720000004");

        MvcResult pendingLogin = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileOrEmail\":\"9720000004\",\"password\":\"" + PASSWORD + "\",\"role\":\"OPERATOR\"}"))
                .andReturn();
        assertThat(pendingLogin.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(pendingLogin.getResponse().getContentAsString(), "$.message"))
                .isEqualTo("Operator account is pending admin approval.");

        mockMvc.perform(patch("/api/admin/operators/" + registration.operatorId() + "/verify")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());

        // The onboarding token never becomes a session, even for the now-approved operator
        mockMvc.perform(upload(registration.operatorId(), registration.onboardingToken(), "AADHAAR"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/operators/me").header("Authorization", "Bearer " + registration.onboardingToken()))
                .andExpect(status().isUnauthorized());

        String session = JsonPath.read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileOrEmail\":\"9720000004\",\"password\":\"" + PASSWORD + "\",\"role\":\"OPERATOR\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.token");
        mockMvc.perform(get("/api/operators/me").header("Authorization", "Bearer " + session)).andExpect(status().isOk());
        mockMvc.perform(upload(registration.operatorId(), session, "AADHAAR")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A rejected operator's onboarding token is refused")
    void rejectedOperatorTokenRefused() throws Exception {
        Registration registration = register("9720000005");
        mockMvc.perform(patch("/api/admin/operators/" + registration.operatorId() + "/verify")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\",\"rejectionReason\":\"Blurred licence\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(upload(registration.operatorId(), registration.onboardingToken(), "AADHAAR"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Forged, mis-scoped and expired onboarding-style tokens are refused; scoped claims never make a session")
    void forgedTokensRefused() throws Exception {
        Registration registration = register("9720000006");
        long now = Instant.now().getEpochSecond();
        String sub = String.valueOf(registration.operatorId());

        String expired = sign("{\"sub\":\"" + sub + "\",\"role\":\"OPERATOR_ONBOARDING\",\"scope\":\"OPERATOR_ONBOARDING\","
                + "\"acct\":\"OPERATOR\",\"iat\":" + (now - 7300) + ",\"exp\":" + (now - 100) + "}");
        String wrongAccountType = sign("{\"sub\":\"" + sub + "\",\"role\":\"OPERATOR_ONBOARDING\",\"scope\":\"OPERATOR_ONBOARDING\","
                + "\"acct\":\"PARTNER\",\"iat\":" + now + ",\"exp\":" + (now + 600) + "}");
        String operatorRoleWithScope = sign("{\"sub\":\"" + sub + "\",\"role\":\"OPERATOR\",\"scope\":\"OPERATOR_ONBOARDING\","
                + "\"acct\":\"OPERATOR\",\"iat\":" + now + ",\"exp\":" + (now + 600) + "}");
        String unknownScope = sign("{\"sub\":\"" + sub + "\",\"role\":\"OPERATOR\",\"scope\":\"ADMIN\","
                + "\"acct\":\"OPERATOR\",\"iat\":" + now + ",\"exp\":" + (now + 600) + "}");
        String onboardingRoleWithoutScope = sign("{\"sub\":\"" + sub + "\",\"role\":\"OPERATOR_ONBOARDING\","
                + "\"acct\":\"OPERATOR\",\"iat\":" + now + ",\"exp\":" + (now + 600) + "}");
        String[] parts = registration.onboardingToken().split("\\.");
        String tampered = parts[0] + "." + b64("{\"sub\":\"" + otherOperator.getId() + "\",\"role\":\"OPERATOR_ONBOARDING\","
                + "\"scope\":\"OPERATOR_ONBOARDING\",\"acct\":\"OPERATOR\",\"iat\":" + now + ",\"exp\":" + (now + 600) + "}") + "." + parts[2];

        for (String token : new String[]{expired, wrongAccountType, operatorRoleWithScope, unknownScope, onboardingRoleWithoutScope}) {
            mockMvc.perform(upload(registration.operatorId(), token, "AADHAAR")).andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/operators/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(upload(otherOperator.getId(), tampered, "AADHAAR")).andExpect(status().isUnauthorized());
        assertThat(documentRepository.findByOperatorId(registration.operatorId())).isEmpty();
        assertThat(documentRepository.findByOperatorId(otherOperator.getId())).isEmpty();
    }

    @Test
    @DisplayName("Other roles' sessions cannot upload an operator's documents; admins still can")
    void otherRoles() throws Exception {
        Registration registration = register("9720000007");
        String farmerToken = jwtService.generateToken(farmer.getId(), "FARMER", farmer.getMobileNumber(),
                JwtService.accountTypeClaims(com.agrorental.security.jwt.AccountType.FARMER));
        String partnerToken = jwtService.generateToken(partner.getId(), "PARTNER", partner.getMobileNumber(),
                JwtService.accountTypeClaims(com.agrorental.security.jwt.AccountType.PARTNER));

        mockMvc.perform(upload(registration.operatorId(), farmerToken, "AADHAAR")).andExpect(status().isForbidden());
        mockMvc.perform(upload(registration.operatorId(), partnerToken, "AADHAAR")).andExpect(status().isForbidden());
        mockMvc.perform(upload(registration.operatorId(), adminToken, "AADHAAR")).andExpect(status().isCreated());
    }
}
