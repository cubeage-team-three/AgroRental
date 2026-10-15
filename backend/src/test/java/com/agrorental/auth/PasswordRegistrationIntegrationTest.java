package com.agrorental.auth;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.auth.service.PasswordPolicy;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth contract 2.2: registration with a password and no OTP step for farmers, partners, operators and the
 * legacy users-table endpoint; one password policy; approval still gates partners and operators; no public
 * admin registration.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth_password_registration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=auth-password-registration-secret-32-chars",
    "server.port=0"
})
@DisplayName("Auth contract 2.2: password registration without OTP")
class PasswordRegistrationIntegrationTest {

    private static final String PASSWORD = "Harvest2026";
    private static final String ADMIN_PASSWORD = "Admin-Pass-2026";

    @Autowired private WebApplicationContext context;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private OperatorRepository operatorRepository;
    @Autowired private AdminRepository adminRepository;

    private MockMvc mockMvc;
    private String adminToken;

    @BeforeAll
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        adminRepository.save(Admin.builder().fullName("Reg Admin").email("admin@registration.test")
                .password(passwordEncoder.encode(ADMIN_PASSWORD)).build());
        adminToken = JsonPath.read(login("admin@registration.test", ADMIN_PASSWORD, "ADMIN")
                .getResponse().getContentAsString(), "$.data.token");
    }

    private MvcResult login(String identifier, String password, String role) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileOrEmail\":\"" + identifier + "\",\"password\":\"" + password
                                + "\",\"role\":\"" + role + "\"}"))
                .andReturn();
    }

    private String message(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.message");
    }

    private static String farmerJson(String mobile, String email, String password) {
        return "{\"fullName\":\"Reg Farmer\",\"mobileNumber\":\"" + mobile + "\""
                + (email != null ? ",\"email\":\"" + email + "\"" : "")
                + (password != null ? ",\"password\":\"" + password + "\"" : "") + "}";
    }

    private static String partnerJson(String mobile, String password) {
        return "{\"fullName\":\"Reg Partner\",\"businessName\":\"Reg Rentals\",\"mobileNumber\":\"" + mobile
                + "\",\"email\":\"" + mobile + "@registration.test\",\"password\":\"" + password + "\"}";
    }

    private static String operatorJson(String mobile, String password) {
        return "{\"fullName\":\"Reg Operator\",\"mobileNumber\":\"" + mobile + "\",\"email\":\"" + mobile
                + "@registration.test\",\"address\":\"Pune\",\"aadhaarNumber\":\"123456789012\","
                + "\"drivingLicenseNumber\":\"MH12-" + mobile + "\",\"experience\":4,\"skills\":\"Tractor\","
                + "\"password\":\"" + password + "\"}";
    }

    @Test
    @DisplayName("Farmer: password is required and policy-checked; the account is ACTIVE at once and logs in by mobile or email")
    void farmerRegistration() throws Exception {
        mockMvc.perform(post("/api/farmers/register").contentType(MediaType.APPLICATION_JSON)
                        .content(farmerJson("9810000001", null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").value("Password is required"));
        for (String weak : new String[]{"short1", "onlyletters", "12345678"}) {
            mockMvc.perform(post("/api/farmers/register").contentType(MediaType.APPLICATION_JSON)
                            .content(farmerJson("9810000001", null, weak)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(PasswordPolicy.REQUIREMENTS_MESSAGE));
        }
        assertThat(farmerRepository.findByMobileNumber("9810000001")).isEmpty();

        mockMvc.perform(post("/api/farmers/register").contentType(MediaType.APPLICATION_JSON)
                        .content(farmerJson("9810000001", "reg.farmer@registration.test", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        Farmer farmer = farmerRepository.findByMobileNumber("9810000001").orElseThrow();
        assertThat(farmer.getAccountStatus()).isEqualTo("ACTIVE");
        assertThat(farmer.getPassword()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, farmer.getPassword())).isTrue();

        for (String identifier : new String[]{"9810000001", "reg.farmer@registration.test"}) {
            MvcResult login = login(identifier, PASSWORD, "FARMER");
            assertThat(login.getResponse().getStatus()).as(message(login)).isEqualTo(200);
            assertThat((Integer) JsonPath.read(login.getResponse().getContentAsString(), "$.data.farmerId"))
                    .isEqualTo(farmer.getId().intValue());
        }
    }

    @Test
    @DisplayName("Partner: policy-checked password, PENDING until an admin approves; OTP is never required")
    void partnerRegistrationNeedsApprovalOnly() throws Exception {
        mockMvc.perform(post("/api/partners/register").contentType(MediaType.APPLICATION_JSON)
                        .content(partnerJson("9610000001", "weakpass")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PasswordPolicy.REQUIREMENTS_MESSAGE));

        MvcResult registered = mockMvc.perform(post("/api/partners/register").contentType(MediaType.APPLICATION_JSON)
                        .content(partnerJson("9610000001", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.otpVerified").value(false))
                .andReturn();
        Integer partnerId = JsonPath.read(registered.getResponse().getContentAsString(), "$.data.id");

        MvcResult pending = login("9610000001", PASSWORD, "PARTNER");
        assertThat(pending.getResponse().getStatus()).isEqualTo(400);
        assertThat(message(pending)).contains("pending admin approval");

        mockMvc.perform(put("/api/partners/" + partnerId + "/kyc/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        MvcResult approved = login("9610000001", PASSWORD, "PARTNER");
        assertThat(approved.getResponse().getStatus()).as(message(approved)).isEqualTo(200);
        Partner partner = partnerRepository.findById(partnerId.longValue()).orElseThrow();
        assertThat(partner.isOtpVerified()).isFalse();
        assertThat(partner.getPassword()).startsWith("$2");
    }

    @Test
    @DisplayName("Operator: policy-checked password, PENDING until approved; approval needs no mobile/OTP verification")
    void operatorRegistrationNeedsApprovalOnly() throws Exception {
        mockMvc.perform(post("/api/operators/register").contentType(MediaType.APPLICATION_JSON)
                        .content(operatorJson("9710000001", "lettersonly")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PasswordPolicy.REQUIREMENTS_MESSAGE));

        MvcResult registered = mockMvc.perform(post("/api/operators/register").contentType(MediaType.APPLICATION_JSON)
                        .content(operatorJson("9710000001", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.mobileVerified").value(false))
                .andExpect(jsonPath("$.data.onboardingToken").isNotEmpty())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andReturn();
        Integer operatorId = JsonPath.read(registered.getResponse().getContentAsString(), "$.data.id");

        MvcResult pending = login("9710000001", PASSWORD, "OPERATOR");
        assertThat(pending.getResponse().getStatus()).isEqualTo(400);
        assertThat(message(pending)).isEqualTo("Operator account is pending admin approval.");

        mockMvc.perform(patch("/api/admin/operators/" + operatorId + "/verify").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        MvcResult approved = login("9710000001", PASSWORD, "OPERATOR");
        assertThat(approved.getResponse().getStatus()).as(message(approved)).isEqualTo(200);
        Operator operator = operatorRepository.findById(operatorId.longValue()).orElseThrow();
        assertThat(operator.getStatus()).isEqualTo(OperatorStatus.APPROVED);
        assertThat(operator.isMobileVerified()).as("approval must not pretend the mobile was OTP-verified").isFalse();
    }

    @Test
    @DisplayName("Legacy /api/auth/register: same policy, creates an ACTIVE farmer that can log in (no PENDING_OTP dead end)")
    void legacyRegistration() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Legacy\",\"email\":\"legacy@registration.test\",\"password\":\"abc12\",\"mobileNumber\":\"9820000001\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Legacy\",\"email\":\"legacy@registration.test\",\"password\":\"nodigitshere\",\"mobileNumber\":\"9820000001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PasswordPolicy.REQUIREMENTS_MESSAGE));
        assertThat(userRepository.findByEmail("legacy@registration.test")).isEmpty();

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Legacy\",\"email\":\"legacy@registration.test\",\"password\":\"" + PASSWORD
                                + "\",\"mobileNumber\":\"9820000001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("FARMER"));

        User user = userRepository.findByEmail("legacy@registration.test").orElseThrow();
        Farmer farmer = farmerRepository.findByMobileNumber("9820000001").orElseThrow();
        assertThat(farmer.getAccountStatus()).isEqualTo("ACTIVE");
        assertThat(user.getPassword()).startsWith("$2");

        MvcResult login = login("legacy@registration.test", PASSWORD, "FARMER");
        assertThat(login.getResponse().getStatus()).as(message(login)).isEqualTo(200);
    }

    @Test
    @DisplayName("There is no public admin registration")
    void noPublicAdminRegistration() throws Exception {
        long admins = adminRepository.count();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mallory\",\"email\":\"mallory@registration.test\",\"password\":\"" + PASSWORD
                                + "\",\"role\":\"ADMIN\",\"mobileNumber\":\"9820000099\"}"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(post("/api/admin/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mallory@registration.test\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(adminRepository.count()).isEqualTo(admins);
        assertThat(userRepository.findByEmail("mallory@registration.test")).isEmpty();
        MvcResult login = login("mallory@registration.test", PASSWORD, "ADMIN");
        assertThat(login.getResponse().getStatus()).isEqualTo(400);
    }
}
