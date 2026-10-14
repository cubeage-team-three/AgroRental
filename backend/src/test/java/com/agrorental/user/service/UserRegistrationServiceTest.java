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

        userRegistrationService.registerUser(request);

        verify(userRepository).save(any());
        verify(emailService).sendVerificationEmail(any(), any());
    }

    @Test
    @DisplayName("Should reject ADMIN registration and throw BadRequestException")
    void shouldRejectAdminRegistration() {
        request.setRole(Role.ADMIN);

        when(userRepository.existsByEmail(any())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            userRegistrationService.registerUser(request);
        });

        assertEquals("Registration as ADMIN is not permitted.", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }
}
