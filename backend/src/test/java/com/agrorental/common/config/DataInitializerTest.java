package com.agrorental.common.config;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.notification.entity.Notification;
import com.agrorental.notification.repository.NotificationRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorJobPauseInterval;
import com.agrorental.operator.entity.OperatorReview;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private static final String TEST_OPERATOR_SEED_PASSWORD = "test-only-operator-seed-password";

    /** Enables mock-data seeding in a dev profile and stubs the repositories used by every seeding run. */
    private void stubCoreSeeding() {
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
        AtomicLong bookingIds = new AtomicLong(100L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(bookingIds.getAndIncrement());
            return b;
        });
    }

    private void configureOperatorSeed(boolean enabled, String password) {
        ReflectionTestUtils.setField(dataInitializer, "operatorSeedEnabled", enabled);
        ReflectionTestUtils.setField(dataInitializer, "operatorSeedPassword", password);
    }

    private List<Booking> captureSavedBookings() {
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, times(5)).save(bookingCaptor.capture());
        return bookingCaptor.getAllValues();
    }

    private void assertOperatorDependentDataSkipped() {
        verify(operatorRepository, never()).save(any());
        verify(operatorJobAssignmentRepository, never()).save(any());
        verify(pauseIntervalRepository, never()).save(any());
        verify(operatorReviewRepository, never()).save(any());
        // Only the three partner notifications that do not mention an operator are seeded
        verify(notificationRepository, times(3)).save(any(Notification.class));

        List<Booking> bookings = captureSavedBookings();
        bookings.forEach(b -> assertNull(b.getOperator(), "Booking must not reference an operator that was never created"));
        assertEquals(BookingStatus.CONFIRMED, bookings.get(2).getStatus(),
                "Booking without an operator must not be marked OPERATOR_ASSIGNED");

        // Non-operator seed data is still created
        verify(partnerRepository, times(1)).save(any(Partner.class));
        verify(farmerRepository, times(1)).save(any(Farmer.class));
        verify(paymentRepository, times(3)).save(any());
        verify(reviewRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Should seed mock data, operators and operator-dependent data when operator seeding is enabled with a password")
    void shouldSeed_WhenPropertyIsTrueAndDevProfileAndDatabaseEmpty_ReturnsTrueAndExecutesSeeding() {
        stubCoreSeeding();
        configureOperatorSeed(true, TEST_OPERATOR_SEED_PASSWORD);

        AtomicLong operatorIds = new AtomicLong(1L);
        when(operatorRepository.save(any(Operator.class))).thenAnswer(inv -> {
            Operator o = inv.getArgument(0);
            o.setId(operatorIds.getAndIncrement());
            return o;
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
        verify(passwordEncoder, times(1)).encode(TEST_OPERATOR_SEED_PASSWORD);

        // All four operators are saved with the encoded seed password
        ArgumentCaptor<Operator> operatorCaptor = ArgumentCaptor.forClass(Operator.class);
        verify(operatorRepository, times(4)).save(operatorCaptor.capture());
        List<Operator> savedOperators = operatorCaptor.getAllValues();
        assertEquals(List.of("9876543220", "9876543221", "9876543222", "7385627789"),
                savedOperators.stream().map(Operator::getMobileNumber).toList());
        savedOperators.forEach(o -> {
            assertNotNull(o.getId());
            assertEquals("hashed-test-password", o.getPassword());
        });

        // Every assignment references a saved operator and a saved booking
        List<Booking> bookings = captureSavedBookings();
        ArgumentCaptor<OperatorJobAssignment> assignmentCaptor = ArgumentCaptor.forClass(OperatorJobAssignment.class);
        verify(operatorJobAssignmentRepository, times(3)).save(assignmentCaptor.capture());
        assignmentCaptor.getAllValues().forEach(a -> {
            assertNotNull(a.getOperator(), "Assignment must reference an operator");
            assertTrue(savedOperators.stream().anyMatch(o -> o == a.getOperator()),
                    "Assignment must reference an operator that was actually saved");
            assertTrue(bookings.stream().anyMatch(b -> b == a.getBooking()),
                    "Assignment must reference a booking that was actually saved");
        });

        ArgumentCaptor<OperatorJobPauseInterval> pauseCaptor = ArgumentCaptor.forClass(OperatorJobPauseInterval.class);
        verify(pauseIntervalRepository, times(1)).save(pauseCaptor.capture());
        assertTrue(savedOperators.stream().anyMatch(o -> o == pauseCaptor.getValue().getOperator()));

        ArgumentCaptor<OperatorReview> reviewCaptor = ArgumentCaptor.forClass(OperatorReview.class);
        verify(operatorReviewRepository, times(1)).save(reviewCaptor.capture());
        assertTrue(savedOperators.stream().anyMatch(o -> o == reviewCaptor.getValue().getOperator()));

        // Bookings with operators reference saved operators; the assigned booking keeps its status
        bookings.stream().map(Booking::getOperator).filter(Objects::nonNull)
                .forEach(op -> assertTrue(savedOperators.stream().anyMatch(o -> o == op)));
        assertEquals(BookingStatus.OPERATOR_ASSIGNED, bookings.get(2).getStatus());

        verify(notificationRepository, times(4)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should skip operators and operator-dependent data when operator seeding is disabled")
    void run_WhenOperatorSeedingDisabled_SkipsOperatorsAndDependentData() {
        stubCoreSeeding();
        configureOperatorSeed(false, TEST_OPERATOR_SEED_PASSWORD);

        assertDoesNotThrow(() -> dataInitializer.run());

        verify(passwordEncoder, never()).encode(TEST_OPERATOR_SEED_PASSWORD);
        assertOperatorDependentDataSkipped();
    }

    @Test
    @DisplayName("Should skip operators and operator-dependent data when operator seed password is blank")
    void run_WhenOperatorSeedPasswordBlank_SkipsOperatorsAndDependentData() {
        stubCoreSeeding();
        configureOperatorSeed(true, "   ");

        assertDoesNotThrow(() -> dataInitializer.run());

        verify(passwordEncoder, never()).encode("   ");
        assertOperatorDependentDataSkipped();
    }

    @Test
    @DisplayName("Should skip operators and operator-dependent data when operator seed password is missing")
    void run_WhenOperatorSeedPasswordMissing_SkipsOperatorsAndDependentData() {
        stubCoreSeeding();
        configureOperatorSeed(true, null);

        assertDoesNotThrow(() -> dataInitializer.run());

        verify(passwordEncoder, never()).encode(null);
        assertOperatorDependentDataSkipped();
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
    @DisplayName("Should skip mock-data seeding when 'dev' is only the implicit default profile (no explicit profile)")
    void shouldSeed_WhenDevIsOnlyImplicitDefaultProfile_ReturnsFalseAndSkips() {
        dataInitializer.setSeedMockDataEnabled(true);
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        when(environment.getDefaultProfiles()).thenReturn(new String[]{"dev"});

        assertFalse(dataInitializer.shouldSeed());
        dataInitializer.run();

        verify(partnerRepository, never()).save(any());
        verify(partnerRepository, never()).count();
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
