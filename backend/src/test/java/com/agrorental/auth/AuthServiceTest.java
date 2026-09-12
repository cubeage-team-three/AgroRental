package com.agrorental.auth;

import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.auth.service.AuthService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmerOtpService;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.user.repository.UserRepository;
import com.agrorental.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyLong;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Partner Login & Verification Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private FarmerOtpService farmerOtpService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Partner approvedPartner;
    private Partner pendingPartner;

    @BeforeEach
    void setUp() {
        approvedPartner = Partner.builder()
                .fullName("Ramesh Patel")
                .businessName("Ramesh Agro Rentals")
                .mobileNumber("9876543210")
                .email("ramesh@agro.com")
                .password("encoded_password")
                .verificationStatus(Partner.VerificationStatus.APPROVED)
                .build();
        approvedPartner.setId(10L);
        approvedPartner.setActive(true);

        pendingPartner = Partner.builder()
                .fullName("Test Partner")
                .mobileNumber("9876543299")
                .email("testpartner@gmail.com")
                .password("encoded_password")
                .verificationStatus(Partner.VerificationStatus.PENDING)
                .build();
        pendingPartner.setId(11L);
        pendingPartner.setActive(true);
    }

    @Test
    @DisplayName("Should successfully log in approved partner by email")
    void shouldLoginApprovedPartnerByEmail() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("ramesh@agro.com")
                .password("plain_password")
                .build();

        when(adminRepository.findByEmail("ramesh@agro.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("ramesh@agro.com", "ramesh@agro.com")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("ramesh@agro.com", "ramesh@agro.com"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("plain_password", "encoded_password")).thenReturn(true);

        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any()))
                .thenReturn("mock.jwt.token");


        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getPartnerId()).isEqualTo(10L);
        assertThat(response.getRole()).isEqualTo("PARTNER");
        assertThat(response.getEmail()).isEqualTo("ramesh@agro.com");
        assertThat(response.getToken()).isNotBlank();
    }

    @Test
    @DisplayName("Should successfully log in approved partner with uppercase email")
    void shouldLoginApprovedPartnerByUppercaseEmail() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("RAMESH@AGRO.COM")
                .password("plain_password")
                .build();

        when(adminRepository.findByEmail("RAMESH@AGRO.COM")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("RAMESH@AGRO.COM", "RAMESH@AGRO.COM")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("RAMESH@AGRO.COM", "RAMESH@AGRO.COM"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("plain_password", "encoded_password")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getPartnerId()).isEqualTo(10L);
        assertThat(response.getRole()).isEqualTo("PARTNER");
    }

    @Test
    @DisplayName("Should successfully log in approved partner by mobile number")
    void shouldLoginApprovedPartnerByMobileNumber() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543210")
                .password("plain_password")
                .build();

        when(adminRepository.findByEmail("9876543210")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("9876543210", "9876543210")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("9876543210", "9876543210"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("plain_password", "encoded_password")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getPartnerId()).isEqualTo(10L);
        assertThat(response.getRole()).isEqualTo("PARTNER");
    }

    @Test
    @DisplayName("Should block login and return verification pending message for pending partner")
    void shouldBlockLoginForPendingPartner() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("testpartner@gmail.com")
                .password("plain_password")
                .build();

        when(adminRepository.findByEmail("testpartner@gmail.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("testpartner@gmail.com", "testpartner@gmail.com")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("testpartner@gmail.com", "testpartner@gmail.com"))
                .thenReturn(Optional.of(pendingPartner));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Your account verification is currently pending admin approval");
    }

    @Test
    @DisplayName("Should block login for deactivated partner")
    void shouldBlockLoginForDeactivatedPartner() {
        approvedPartner.setActive(false);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("ramesh@agro.com")
                .password("plain_password")
                .build();

        when(adminRepository.findByEmail("ramesh@agro.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("ramesh@agro.com", "ramesh@agro.com")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("ramesh@agro.com", "ramesh@agro.com"))
                .thenReturn(Optional.of(approvedPartner));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Partner account is currently deactivated");
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void shouldRejectInvalidPassword() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("ramesh@agro.com")
                .password("wrong_password")
                .build();

        when(adminRepository.findByEmail("ramesh@agro.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("ramesh@agro.com", "ramesh@agro.com")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("ramesh@agro.com", "ramesh@agro.com"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid mobile/email or password.");
    }

    @Test
    @DisplayName("Should reject login when account does not exist")
    void shouldRejectNonExistentAccount() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("unknown@agro.com")
                .password("any_password")
                .build();

        when(adminRepository.findByEmail("unknown@agro.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("unknown@agro.com", "unknown@agro.com")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("unknown@agro.com", "unknown@agro.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password.");
    }
}
