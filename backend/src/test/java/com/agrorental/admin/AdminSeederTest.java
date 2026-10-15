package com.agrorental.admin;

import com.agrorental.admin.config.AdminSeeder;
import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSeeder Security & Staging Profile Tests")
class AdminSeederTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    @InjectMocks
    private AdminSeeder adminSeeder;

    @Test
    @DisplayName("Dev Profile: Does not seed any admin when no admin password is configured (no default credentials)")
    void devProfile_WithoutConfiguredPassword_SkipsSeeding() {
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminEmail", "");
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminPassword", "");

        adminSeeder.run();

        verify(adminRepository, never()).save(any(Admin.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Dev Profile: Seeds admin with the explicitly configured password and default email")
    void devProfile_SeedsAdminWithConfiguredPassword() {
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminEmail", "");
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminPassword", "Local_Dev_Admin_Pw_1");
        when(adminRepository.existsByEmail(AdminSeeder.DEFAULT_ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("Local_Dev_Admin_Pw_1")).thenReturn("$2a$10$encodedDevPassword");

        adminSeeder.run();

        ArgumentCaptor<Admin> adminCaptor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(adminCaptor.capture());
        Admin savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getEmail()).isEqualTo(AdminSeeder.DEFAULT_ADMIN_EMAIL);
        assertThat(savedAdmin.getPassword()).isEqualTo("$2a$10$encodedDevPassword");
    }

    @Test
    @DisplayName("Dev Profile: Skips seeding when admin already exists")
    void devProfile_SkipsWhenAdminAlreadyExists() {
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminPassword", "Local_Dev_Admin_Pw_1");
        when(adminRepository.existsByEmail(AdminSeeder.DEFAULT_ADMIN_EMAIL)).thenReturn(true);

        adminSeeder.run();

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    @DisplayName("Staging Profile: Strictly skips seeding when admin password is empty or missing")
    void stagingProfile_RefusesDefaultAdminWhenPasswordMissing() {
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminEmail", "");
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminPassword", "");

        adminSeeder.run();

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    @DisplayName("Staging Profile: Strictly skips seeding when configured password matches the known committed default")
    void stagingProfile_RefusesDefaultAdminWhenConfiguredWithKnownDefaultPassword() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminEmail", "custom-admin@agrorent.in");
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminPassword", AdminSeeder.KNOWN_INSECURE_ADMIN_PASSWORD);

        adminSeeder.run();

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    @DisplayName("Implicit default profile: Refuses the known committed default password")
    void implicitDefaultProfile_RefusesKnownDefaultPassword() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        when(environment.getDefaultProfiles()).thenReturn(new String[]{"dev"});
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminPassword", AdminSeeder.KNOWN_INSECURE_ADMIN_PASSWORD);

        adminSeeder.run();

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    @DisplayName("Explicit dev profile: Known committed default password is accepted only for local development")
    void explicitDevProfile_AcceptsKnownDefaultPasswordWhenExplicitlyConfigured() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminPassword", AdminSeeder.KNOWN_INSECURE_ADMIN_PASSWORD);
        when(adminRepository.existsByEmail(AdminSeeder.DEFAULT_ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(AdminSeeder.KNOWN_INSECURE_ADMIN_PASSWORD)).thenReturn("$2a$10$encodedLocalPassword");

        adminSeeder.run();

        verify(adminRepository).save(any(Admin.class));
    }

    @Test
    @DisplayName("Staging Profile: Seeds admin when custom non-default password is provided")
    void stagingProfile_SeedsAdminWhenCustomPasswordProvided() {
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminEmail", "secure-staging-admin@agrorent.in");
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminPassword", "CustomSecret_Staging_99!");
        when(adminRepository.existsByEmail("secure-staging-admin@agrorent.in")).thenReturn(false);
        when(passwordEncoder.encode("CustomSecret_Staging_99!")).thenReturn("$2a$10$encodedCustomSecret");

        adminSeeder.run();

        ArgumentCaptor<Admin> adminCaptor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(adminCaptor.capture());
        Admin savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getEmail()).isEqualTo("secure-staging-admin@agrorent.in");
        assertThat(savedAdmin.getPassword()).isEqualTo("$2a$10$encodedCustomSecret");
    }
}
