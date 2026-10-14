package com.agrorental.operator;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency Integration Test for E2E-003:
 * Verifies that concurrent competing assignment requests targeting the same operator
 * with overlapping booking dates are safely serialized by database pessimistic locking,
 * guaranteeing that exactly one assignment is committed and no conflicting active assignments
 * can be persisted in the database.
 *
 * Runs against an isolated, disposable in-memory H2 database.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:e2e003_concurrency_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=test-secret-key-32-chars-minimum-length-for-hmac-sha256",
    "server.port=0"
})
@DisplayName("E2E-003: Operator Assignment Concurrency Integration Test (Isolated In-Memory H2 DB)")
class OperatorJobOverlapConcurrencyE2E003IntegrationTest {

    @Autowired
    private OperatorAssignmentService operatorAssignmentService;

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

    private Partner testPartner;
    private Operator testOperator;
    private Farmer testFarmer;
    private Equipment testEquipment;
    private final List<Booking> testBookings = new ArrayList<>();

    @BeforeEach
    void setUp() {
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

        // Create 5 distinct confirmed bookings with identical overlapping date range (Nov 1 to Nov 5)
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
    @DisplayName("Concurrent competing requests for same operator on overlapping bookings: strictly 1 succeeds, 4 fail with conflict error")
    void testConcurrentCompetingOperatorAssignments() throws Exception {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGun = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Future<OperatorAssignmentResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final Long bookingId = testBookings.get(i).getId();
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startGun.await(); // Release all 5 threads simultaneously
                    OperatorAssignmentRequest req = OperatorAssignmentRequest.builder()
                            .operatorId(testOperator.getId())
                            .notes("Concurrent request for booking " + bookingId)
                            .build();
                    OperatorAssignmentResponse resp = operatorAssignmentService.assignOperator(
                            bookingId, req, "PARTNER_" + testPartner.getId(), testPartner.getId()
                    );
                    successCount.incrementAndGet();
                    return resp;
                } catch (BadRequestException e) {
                    if (e.getMessage().contains("Operator already has an active assignment for overlapping dates")) {
                        conflictCount.incrementAndGet();
                    }
                    return null;
                }
            }));
        }

        // Wait for all worker threads to reach starting line
        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        // Fire start gun
        startGun.countDown();

        for (Future<OperatorAssignmentResponse> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Verification 1: Exactly 1 thread successfully committed assignment
        assertThat(successCount.get()).isEqualTo(1);
        // Verification 2: Exactly 4 competing threads failed with the expected business conflict error
        assertThat(conflictCount.get()).isEqualTo(4);

        // Verification 3: Database query confirms strictly 1 active assignment exists for this operator
        List<OperatorJobAssignment> activeAssignments = assignmentRepository.findByOperatorIdAndAssignmentStatus(
                testOperator.getId(), OperatorAssignmentStatus.ASSIGNED
        );
        assertThat(activeAssignments).hasSize(1);

        // Verification 4: Booking repository confirms exactly 1 booking has OPERATOR_ASSIGNED status
        long assignedBookingsCount = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.OPERATOR_ASSIGNED)
                .count();
        assertThat(assignedBookingsCount).isEqualTo(1);

        long confirmedBookingsCount = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED && b.getOperator() == null)
                .count();
        assertThat(confirmedBookingsCount).isEqualTo(4);
    }
}
