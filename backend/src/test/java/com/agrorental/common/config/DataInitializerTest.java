package com.agrorental.common.config;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.notification.repository.NotificationRepository;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorJobPauseIntervalRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.repository.OperatorReviewRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.review.repository.ReviewRepository;
import com.agrorental.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataInitializer Gating & Safety Unit Tests")
class DataInitializerTest {

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorJobAssignmentRepository operatorJobAssignmentRepository;

    @Mock
    private OperatorJobPauseIntervalRepository pauseIntervalRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OperatorReviewRepository operatorReviewRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(
                partnerRepository,
                farmerRepository,
                userRepository,
                farmRepository,
                equipmentRepository,
                operatorRepository,
                operatorJobAssignmentRepository,
                pauseIntervalRepository,
                bookingRepository,
                paymentRepository,
                notificationRepository,
                reviewRepository,
                operatorReviewRepository,
                passwordEncoder,
                environment
        );
    }

    @Test
    @DisplayName("Should seed mock data when property is enabled in development profile and database is empty")
    void shouldSeed_WhenPropertyIsTrueAndDevProfileAndDatabaseEmpty_ReturnsTrueAndExecutesSeeding() {
        dataInitializer.setSeedMockDataEnabled(true);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(partnerRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("hashed-test-password");

        when(partnerRepository.save(any(Partner.class))).thenAnswer(inv -> {
            Partner p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(farmerRepository.save(any(Farmer.class))).thenAnswer(inv -> {
            Farmer f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> {
            Farm f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(operatorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(100L);
            return b;
        });
        when(operatorJobAssignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(inv -> {
            OperatorJobAssignment a = inv.getArgument(0);
            a.setId(200L);
            return a;
        });

        assertTrue(dataInitializer.shouldSeed());
        dataInitializer.run();

        verify(partnerRepository, times(1)).save(any(Partner.class));
        verify(farmerRepository, times(1)).save(any(Farmer.class));
        verify(bookingRepository, atLeastOnce()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should skip seeding when property is explicitly false")
    void shouldSeed_WhenPropertyIsFalse_ReturnsFalseAndSkipsSeeding() {
        dataInitializer.setSeedMockDataEnabled(false);

        assertFalse(dataInitializer.shouldSeed());
        dataInitializer.run();

        verify(partnerRepository, never()).save(any());
        verify(partnerRepository, never()).count();
    }

    @Test
    @DisplayName("Should skip seeding by default when property is missing / defaults to false")
    void shouldSeed_WhenPropertyDefaultsToFalse_ReturnsFalseAndSkipsSeeding() {
        // default value is false
        assertFalse(dataInitializer.isSeedMockDataEnabled());
        assertFalse(dataInitializer.shouldSeed());
        dataInitializer.run();

        verify(partnerRepository, never()).save(any());
        verify(partnerRepository, never()).count();
    }

    @Test
    @DisplayName("Should strictly block mock-data seeding in production profile even if property is true")
    void shouldSeed_WhenInProductionProfileEvenIfPropertyIsTrue_ReturnsFalseAndBlocksSeeding() {
        dataInitializer.setSeedMockDataEnabled(true);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        assertFalse(dataInitializer.shouldSeed());
        dataInitializer.run();

        verify(partnerRepository, never()).save(any());
        verify(partnerRepository, never()).count();
    }

    @Test
    @DisplayName("Should skip mock-data seeding in staging profile by default when property is false")
    void shouldSeed_WhenInStagingProfileAndPropertyIsFalse_ReturnsFalseAndSkipsSeeding() {
        dataInitializer.setSeedMockDataEnabled(false);

        assertFalse(dataInitializer.shouldSeed());
        dataInitializer.run();

        verify(partnerRepository, never()).save(any());
        verify(partnerRepository, never()).count();
    }

    @Test
    @DisplayName("Should strictly block mock-data seeding in staging profile even if property is true")
    void shouldSeed_WhenInStagingProfileEvenIfPropertyIsTrue_ReturnsFalseAndBlocksSeeding() {
        dataInitializer.setSeedMockDataEnabled(true);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});

        assertFalse(dataInitializer.shouldSeed());
        dataInitializer.run();

        verify(partnerRepository, never()).save(any());
        verify(partnerRepository, never()).count();
    }

    @Test
    @DisplayName("Should verify that enabling development seeding does not enable production")
    void testEnablingDevelopmentSeedingDoesNotInadvertentlyEnableProduction() {
        // Dev profile with property true enables seeding
        dataInitializer.setSeedMockDataEnabled(true);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(partnerRepository.count()).thenReturn(0L);
        assertTrue(dataInitializer.shouldSeed());

        // In prod profile, even with property true, seeding is strictly rejected
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        assertFalse(dataInitializer.shouldSeed());
        dataInitializer.run();
        verify(partnerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip seeding when database already contains existing partner records")
    void shouldSeed_WhenDatabaseAlreadyHasData_ReturnsFalseAndSkips() {
        dataInitializer.setSeedMockDataEnabled(true);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(partnerRepository.count()).thenReturn(1L);

        assertFalse(dataInitializer.shouldSeed());
        dataInitializer.run();

        verify(partnerRepository, never()).save(any());
    }
}
