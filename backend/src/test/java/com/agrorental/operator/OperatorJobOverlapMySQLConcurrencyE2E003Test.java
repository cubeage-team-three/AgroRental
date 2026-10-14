package com.agrorental.operator;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.notification.repository.NotificationRepository;
import com.agrorental.operator.dto.OperatorAssignmentRequest;
import com.agrorental.operator.dto.OperatorAssignmentResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorAssignmentService;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency Verification Test on Real MySQL 8.0 Engine (InnoDB).
 * Connects to disposable MySQL 8.0 container on port 33306.
 *
 * Verifies that:
 * 1. Concurrent attempts to assign the same operator to overlapping bookings serialize correctly on MySQL.
 * 2. Pessimistic row locking on Operator + Isolation.READ_COMMITTED prevents phantom / stale MVCC snapshots.
 * 3. At most one conflicting assignment commits across multiple threads.
 * 4. Rejected transactions leave no partial assignments, keep booking status CONFIRMED, and generate zero notifications.
 * 5. Notifications are not duplicated.
 * 6. Mixed entry points (OperatorAssignmentService and BookingService) enforce the exact same serialized guarantee.
 */
@SpringBootTest
@EnabledIf("isDisposableMysqlRunning")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://127.0.0.1:33306/agrorental_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true",
    "spring.datasource.username=root",
    "spring.datasource.password=root",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=test-secret-key-32-chars-minimum-length-for-hmac-sha256",
    "server.port=0"
})
@DisplayName("E2E-003.1: Operator Assignment Concurrency Verification on Disposable MySQL 8.0")
class OperatorJobOverlapMySQLConcurrencyE2E003Test {

    private static final Logger log = LoggerFactory.getLogger(OperatorJobOverlapMySQLConcurrencyE2E003Test.class);

    static boolean isDisposableMysqlRunning() {
        try (Socket socket = new Socket("localhost", 33306)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Autowired
    private OperatorAssignmentService operatorAssignmentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private OperatorJobAssignmentRepository assignmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Partner testPartner;
    private Operator testOperator;
    private Farmer testFarmer;
    private Equipment testEquipment;
    private final List<Booking> testBookings = new ArrayList<>();

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        assignmentRepository.deleteAll();
        bookingRepository.deleteAll();
        equipmentRepository.deleteAll();
        operatorRepository.deleteAll();
        farmerRepository.deleteAll();
        partnerRepository.deleteAll();
        testBookings.clear();

        testPartner = Partner.builder()
                .fullName("Shivaji Patil")
                .businessName("Patil Agro Rentals")
                .mobileNumber("9822001122")
                .email("patil.rentals@test.com")
                .password("hashed_pass")
                .build();
        testPartner = partnerRepository.save(testPartner);

        testFarmer = Farmer.builder()
                .fullName("Santosh Shinde")
                .mobileNumber("9822003344")
                .email("santosh.farmer@test.com")
                .address("Baramati, Pune")
                .build();
        testFarmer = farmerRepository.save(testFarmer);

        testOperator = Operator.builder()
                .fullName("Tukaram Ghadge")
                .mobileNumber("9822005566")
                .email("tukaram.op@test.com")
                .address("Baramati, Pune")
                .aadhaarNumber("123456789012")
                .drivingLicenseNumber("MH1220200001234")
                .experience(5)
                .skills("Tractor, Harvester")
                .password("hashed_pwd")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        testOperator.setActive(true);
        testOperator = operatorRepository.save(testOperator);

        testEquipment = Equipment.builder()
                .name("John Deere 5050D")
                .category(EquipmentCategory.TRACTOR)
                .brand("John Deere")
                .model("5050D")
                .manufacturingYear(2022)
                .capacity("50 HP")
                .rentalPrice(new BigDecimal("2000.00"))
                .fuelType(com.agrorental.equipment.enums.FuelType.DIESEL)
                .description("Heavy duty farming tractor")
                .locationAddress("Baramati, Pune")
                .latitude(18.5204)
                .longitude(73.8567)
                .partner(testPartner)
                .availabilityStatus(AvailabilityStatus.BOOKED)
                .build();
        testEquipment = equipmentRepository.save(testEquipment);

        for (int i = 1; i <= 5; i++) {
            Booking b = Booking.builder()
                    .farmerId(testFarmer.getId())
                    .partner(testPartner)
                    .equipment(testEquipment)
                    .status(BookingStatus.CONFIRMED)
                    .startDate(LocalDate.of(2026, 11, 1))
                    .endDate(LocalDate.of(2026, 11, 5))
                    .totalCost(new BigDecimal("10000.00"))
                    .deliveryAddress("Farm Field " + i + ", Pune")
                    .build();
            testBookings.add(bookingRepository.save(b));
        }
    }

    @Test
    @DisplayName("Barrier-coordinated concurrent competing requests on MySQL 8.0: exactly 1 commits, 4 fail with conflict error, no duplicate notifications")
    void testConcurrentCompetingOperatorAssignmentsOnMySQL() throws Exception {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Future<OperatorAssignmentResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final Long bookingId = testBookings.get(i).getId();
            futures.add(executor.submit(() -> {
                try {
                    // Barrier coordination ensures all 5 threads release simultaneously
                    barrier.await(5, TimeUnit.SECONDS);
                    OperatorAssignmentRequest req = OperatorAssignmentRequest.builder()
                            .operatorId(testOperator.getId())
                            .notes("Concurrent request on MySQL for booking " + bookingId)
                            .build();
                    OperatorAssignmentResponse resp = operatorAssignmentService.assignOperator(
                            bookingId, req, "PARTNER_" + testPartner.getId(), testPartner.getId()
                    );
                    successCount.incrementAndGet();
                    return resp;
                } catch (BadRequestException e) {
                    if (e.getMessage().contains("Operator already has an active assignment for overlapping dates")) {
                        conflictCount.incrementAndGet();
                    } else {
                        log.error("Unexpected BadRequestException: {}", e.getMessage());
                    }
                    return null;
                } catch (Exception e) {
                    log.error("Unexpected exception: ", e);
                    return null;
                }
            }));
        }

        for (Future<OperatorAssignmentResponse> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        log.info("MySQL Concurrency Test 1 Results: successCount={}, conflictCount={}", successCount.get(), conflictCount.get());

        // 1. Exactly 1 thread successfully committed assignment
        assertThat(successCount.get()).isEqualTo(1);
        // 2. Exactly 4 competing threads failed with the expected business conflict error
        assertThat(conflictCount.get()).isEqualTo(4);

        // 3. Database query confirms strictly 1 active assignment exists for this operator
        List<OperatorJobAssignment> activeAssignments = assignmentRepository.findByOperatorIdAndAssignmentStatus(
                testOperator.getId(), OperatorAssignmentStatus.ASSIGNED
        );
        assertThat(activeAssignments).hasSize(1);

        // 4. Booking repository confirms exactly 1 booking has OPERATOR_ASSIGNED status
        long assignedBookingsCount = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.OPERATOR_ASSIGNED)
                .count();
        assertThat(assignedBookingsCount).isEqualTo(1);

        long confirmedBookingsCount = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED && b.getOperator() == null)
                .count();
        assertThat(confirmedBookingsCount).isEqualTo(4);

        // 5. Notifications are strictly 2 (1 for OPERATOR, 1 for FARMER for the winning booking)
        long totalNotifications = notificationRepository.count();
        assertThat(totalNotifications).isEqualTo(2);
    }

    @Test
    @DisplayName("Barrier-coordinated mixed entry points (OperatorAssignmentService & BookingService) serialize safely on MySQL")
    void testConcurrentCompetingMixedEntryPointsOnMySQL() throws Exception {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final Long bookingId = testBookings.get(i).getId();
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                    if (index % 2 == 0) {
                        // Entry point 1: Canonical OperatorAssignmentService
                        OperatorAssignmentRequest req = OperatorAssignmentRequest.builder()
                                .operatorId(testOperator.getId())
                                .notes("Mixed concurrent request A for booking " + bookingId)
                                .build();
                        operatorAssignmentService.assignOperator(
                                bookingId, req, "PARTNER_" + testPartner.getId(), testPartner.getId()
                        );
                    } else {
                        // Entry point 2: Delegating BookingService.assignOperator
                        bookingService.assignOperator(bookingId, testPartner.getId(), testOperator.getId());
                    }
                    successCount.incrementAndGet();
                } catch (BadRequestException e) {
                    if (e.getMessage().contains("Operator already has an active assignment for overlapping dates")) {
                        conflictCount.incrementAndGet();
                    } else {
                        log.error("Unexpected BadRequestException in mixed test: {}", e.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Unexpected exception in mixed test: ", e);
                }
                return null;
            }));
        }

        for (Future<Void> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        log.info("MySQL Concurrency Test 2 (Mixed Entry Points) Results: successCount={}, conflictCount={}", successCount.get(), conflictCount.get());

        // Strictly 1 succeeded, 3 failed with overlap conflict
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(3);

        List<OperatorJobAssignment> activeAssignments = assignmentRepository.findByOperatorIdAndAssignmentStatus(
                testOperator.getId(), OperatorAssignmentStatus.ASSIGNED
        );
        assertThat(activeAssignments).hasSize(1);

        long assignedBookingsCount = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.OPERATOR_ASSIGNED)
                .count();
        assertThat(assignedBookingsCount).isEqualTo(1);

        long totalNotifications = notificationRepository.count();
        assertThat(totalNotifications).isEqualTo(2);
    }
}
