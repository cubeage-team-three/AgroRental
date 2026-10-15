package com.agrorental.user.service;

import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.user.dto.UserRegistrationRequest;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.EmailVerificationTokenRepository;
import com.agrorental.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationService Unit Tests")
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserRegistrationService userRegistrationService;

    private UserRegistrationRequest request;

    @BeforeEach
    void setUp() {
        request = new UserRegistrationRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setMobileNumber("1234567890");
        request.setRole(Role.FARMER);
    }

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() {
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName(request.getName());
        savedUser.setEmail(request.getEmail());
        savedUser.setRole(request.getRole());
        
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed-password");
        when(userRepository.save(any())).thenReturn(savedUser);

        com.agrorental.user.dto.UserRegistrationResponse response = userRegistrationService.registerUser(request);

        verify(userRepository).save(any());
        verify(emailService).sendVerificationEmail(any(), any());
        // No dead end: the linked farmer is ACTIVE and can log in with its password right away
        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("ACTIVE", userCaptor.getValue().getFarmer().getAccountStatus());
        assertEquals("hashed-password", userCaptor.getValue().getFarmer().getPassword());
        assertEquals("User registered successfully. You can now log in with your email address and password.", response.getMessage());
    }

    @Test
    @DisplayName("Email verification token is stored only as a SHA-256 hash of the emailed token")
    void verificationTokenIsStoredHashed() {
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail(request.getEmail());
        savedUser.setRole(Role.FARMER);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed-password");
        when(userRepository.save(any())).thenReturn(savedUser);

        userRegistrationService.registerUser(request);

        org.mockito.ArgumentCaptor<String> sentToken = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(any(), sentToken.capture());
        org.mockito.ArgumentCaptor<com.agrorental.user.entity.EmailVerificationToken> stored =
                org.mockito.ArgumentCaptor.forClass(com.agrorental.user.entity.EmailVerificationToken.class);
        verify(tokenRepository).save(stored.capture());
        assertEquals(com.agrorental.auth.service.PasswordResetService.sha256Hex(sentToken.getValue()), stored.getValue().getToken());
        org.junit.jupiter.api.Assertions.assertNotEquals(sentToken.getValue(), stored.getValue().getToken());
    }

    @Test
    @DisplayName("Weak passwords are rejected by the shared policy before anything is saved")
    void shouldRejectWeakPassword() {
        request.setPassword("password");
        when(userRepository.existsByEmail(any())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> userRegistrationService.registerUser(request));

        assertEquals(com.agrorental.auth.service.PasswordPolicy.REQUIREMENTS_MESSAGE, exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "rejects self-registration as {0}")
    @org.junit.jupiter.params.provider.EnumSource(value = Role.class, names = {"ADMIN", "PARTNER", "OPERATOR"})
    @DisplayName("Should reject ADMIN, PARTNER and OPERATOR self-registration and throw BadRequestException")
    void shouldRejectNonFarmerRegistration(Role role) {
        request.setRole(role);

        when(userRepository.existsByEmail(any())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            userRegistrationService.registerUser(request);
        });

        assertEquals("Only farmer accounts can be registered here. Partners and operators must use "
                + "their dedicated registration and approval process.", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }
}
