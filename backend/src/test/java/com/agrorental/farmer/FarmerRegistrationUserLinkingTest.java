package com.agrorental.farmer;

import com.agrorental.common.enums.Role;
import com.agrorental.farmer.dto.FarmerRegisterRequest;
import com.agrorental.farmer.dto.FarmerResponse;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmerService;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Farmer Registration User Entity Linking Unit Tests")
class FarmerRegistrationUserLinkingTest {

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private FarmerService farmerService;

    private FarmerRegisterRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRequest = FarmerRegisterRequest.builder()
                .fullName("Shivaji Patil")
                .mobileNumber("9876543210")
                .email("shivaji@agrorental.com")
                .password("SecurePass123!")
                .preferredLanguage("Marathi")
                .build();
    }

    @Test
    @DisplayName("Farmer registration instantiates and links core User entity with Role.FARMER")
    void testRegisterFarmerCreatesAndLinksUserEntity() {
        when(farmerRepository.existsByMobileNumber("9876543210")).thenReturn(false);
        when(farmerRepository.existsByEmail("shivaji@agrorental.com")).thenReturn(false);
        when(userRepository.existsByEmail("shivaji@agrorental.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encoded_pass_123");

        User mockSavedUser = User.builder()
                .name("Shivaji Patil")
                .email("shivaji@agrorental.com")
                .password("encoded_pass_123")
                .role(Role.FARMER)
                .enabled(true)
                .verified(false)
                .build();
        mockSavedUser.setId(50L);

        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

        Farmer mockSavedFarmer = Farmer.builder()
                .user(mockSavedUser)
                .fullName("Shivaji Patil")
                .mobileNumber("9876543210")
                .email("shivaji@agrorental.com")
                .password("encoded_pass_123")
                .preferredLanguage("Marathi")
                .accountStatus("ACTIVE")
                .build();
        mockSavedFarmer.setId(10L);

        when(farmerRepository.save(any(Farmer.class))).thenReturn(mockSavedFarmer);

        FarmerResponse response = farmerService.registerFarmer(sampleRequest);

        assertNotNull(response);
        assertEquals(10L, response.getFarmerId());
        assertEquals("Shivaji Patil", response.getFullName());
        assertEquals("ACTIVE", response.getAccountStatus());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("Shivaji Patil", capturedUser.getName());
        assertEquals("shivaji@agrorental.com", capturedUser.getEmail());
        assertEquals(Role.FARMER, capturedUser.getRole());

        ArgumentCaptor<Farmer> farmerCaptor = ArgumentCaptor.forClass(Farmer.class);
        verify(farmerRepository, times(1)).save(farmerCaptor.capture());
        Farmer capturedFarmer = farmerCaptor.getValue();
        assertNotNull(capturedFarmer.getUser());
        assertEquals(50L, capturedFarmer.getUser().getId());
        // No OTP step: the farmer is active immediately and owns the password hash (never the raw password)
        assertEquals("ACTIVE", capturedFarmer.getAccountStatus());
        assertEquals("encoded_pass_123", capturedFarmer.getPassword());
        assertEquals("encoded_pass_123", capturedUser.getPassword());
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "rejects password \"{0}\" before touching the database")
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"short1", "lettersonly", "1234567890", "          "})
    @DisplayName("Farmer registration requires a password that satisfies the shared policy")
    void testRegisterFarmerRequiresPolicyCompliantPassword(String password) {
        sampleRequest.setPassword(password);

        com.agrorental.common.exception.BadRequestException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.agrorental.common.exception.BadRequestException.class,
                () -> farmerService.registerFarmer(sampleRequest));

        assertEquals(com.agrorental.auth.service.PasswordPolicy.REQUIREMENTS_MESSAGE, ex.getMessage());
        org.mockito.Mockito.verifyNoInteractions(farmerRepository, userRepository, passwordEncoder);
    }
}
