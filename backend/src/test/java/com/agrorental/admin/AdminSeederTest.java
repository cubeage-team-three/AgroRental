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
    @DisplayName("Dev Profile: Seeds default admin when none exists")
    void devProfile_SeedsDefaultAdminWhenNoneExists() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(adminRepository.existsByEmail(AdminSeeder.DEFAULT_ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(AdminSeeder.DEFAULT_ADMIN_PASSWORD)).thenReturn("$2a$10$encodedDevPassword");

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
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(adminRepository.existsByEmail(AdminSeeder.DEFAULT_ADMIN_EMAIL)).thenReturn(true);

        adminSeeder.run();

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    @DisplayName("Staging Profile: Strictly skips seeding when admin password is empty or missing")
    void stagingProfile_RefusesDefaultAdminWhenPasswordMissing() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminEmail", "");
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminPassword", "");
        when(adminRepository.existsByEmail(AdminSeeder.DEFAULT_ADMIN_EMAIL)).thenReturn(false);

        adminSeeder.run();

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    @DisplayName("Staging Profile: Strictly skips seeding when configured password matches the hardcoded default")
    void stagingProfile_RefusesDefaultAdminWhenConfiguredWithKnownDefaultPassword() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminEmail", "custom-admin@agrorent.in");
        ReflectionTestUtils.setField(adminSeeder, "configuredAdminPassword", AdminSeeder.DEFAULT_ADMIN_PASSWORD);
        when(adminRepository.existsByEmail("custom-admin@agrorent.in")).thenReturn(false);

        adminSeeder.run();

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    @DisplayName("Staging Profile: Seeds admin when custom non-default password is provided")
    void stagingProfile_SeedsAdminWhenCustomPasswordProvided() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
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
