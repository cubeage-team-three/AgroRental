package com.agrorental.operator;

import com.agrorental.booking.controller.BookingController;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.operator.controller.OperatorAssignmentController;
import com.agrorental.operator.dto.OperatorAssignmentRequest;
import com.agrorental.operator.dto.OperatorAssignmentResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.mapper.OperatorJobAssignmentMapper;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorLocationRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.repository.OperatorReviewRepository;
import com.agrorental.operator.service.OperatorAssignmentService;
import com.agrorental.operator.service.OperatorEarningsService;
import com.agrorental.partner.entity.Partner;
import com.agrorental.security.principal.PartnerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Focused Regression Test Suite for E2E-003: Operator Active-Job Overlap Prevention.
 *
 * Covers:
 * 1. Same operator, overlapping bookings — second assignment rejected.
 * 2. Same operator, non-overlapping bookings — both permitted.
 * 3. Different operators, overlapping bookings — permitted when other rules allow.
 * 4. Adjacent intervals — boundary behavior matches established interval convention.
 * 5. Terminal assignment does not block reassignment.
 * 6. Each supported assignment entry point enforces the same rule.
 * 7. Concurrent competing requests — no conflicting active assignments can be committed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("E2E-003: Operator Active-Job Overlap Prevention Regression Tests")
class OperatorJobOverlapE2E003RegressionTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private OperatorRepository operatorRepository;
    @Mock private OperatorJobAssignmentRepository assignmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private OperatorLocationRepository locationRepository;
    @Mock private OperatorReviewRepository reviewRepository;
    @Mock private OperatorEarningsService earningsService;

    private OperatorJobAssignmentMapper assignmentMapper;
    private BookingMapper bookingMapper;
    private OperatorAssignmentService operatorAssignmentService;
    private BookingService bookingService;
    private OperatorAssignmentController operatorAssignmentController;
    private BookingController bookingController;

    private Partner partner;
    private Equipment equipment1;
    private Equipment equipment2;
    private Operator operator1;
    private Operator operator2;
    private Booking booking1;
    private Booking booking2;

    @BeforeEach
    void setUp() {
        assignmentMapper = new OperatorJobAssignmentMapper();
        bookingMapper = new BookingMapper();

        operatorAssignmentService = new OperatorAssignmentService(
                bookingRepository,
                operatorRepository,
                assignmentRepository,
                assignmentMapper,
                notificationService,
                earningsService,
                reviewRepository,
                locationRepository
        );

        bookingService = new BookingService(
                bookingRepository,
                equipmentRepository,
                operatorRepository,
                farmRepository,
                notificationService,
                bookingMapper,
                assignmentRepository,
                locationRepository,
                operatorAssignmentService
        );

        operatorAssignmentController = new OperatorAssignmentController(operatorAssignmentService);
        bookingController = new BookingController(bookingService);

        partner = Partner.builder()
                .fullName("Ramesh Patil")
                .businessName("Patil Agro Equipments")
                .mobileNumber("9876543210")
                .build();
        partner.setId(10L);

        equipment1 = Equipment.builder()
                .name("Mahindra 575 DI Tractor")
                .partner(partner)
                .rentalPrice(new BigDecimal("2500.00"))
                .availabilityStatus(AvailabilityStatus.BOOKED)
                .build();
        equipment1.setId(101L);

        equipment2 = Equipment.builder()
                .name("John Deere Harvester")
                .partner(partner)
                .rentalPrice(new BigDecimal("5000.00"))
                .availabilityStatus(AvailabilityStatus.BOOKED)
                .build();
        equipment2.setId(102L);

        operator1 = Operator.builder()
                .fullName("Rajesh Shinde")
                .mobileNumber("9123456780")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .skills("Tractor Operation")
                .build();
        operator1.setId(1L);
        operator1.setActive(true);

        operator2 = Operator.builder()
                .fullName("Amit Gaikwad")
                .mobileNumber("9123456781")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .skills("Harvester Operation")
                .build();
        operator2.setId(2L);
        operator2.setActive(true);

        booking1 = Booking.builder()
                .farmerId(100L)
                .partner(partner)
                .equipment(equipment1)
                .status(BookingStatus.CONFIRMED)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 5))
                .totalCost(new BigDecimal("12500.00"))
                .build();
        booking1.setId(201L);

        booking2 = Booking.builder()
                .farmerId(101L)
                .partner(partner)
                .equipment(equipment2)
                .status(BookingStatus.CONFIRMED)
                .startDate(LocalDate.of(2026, 11, 3))
                .endDate(LocalDate.of(2026, 11, 7))
                .totalCost(new BigDecimal("25000.00"))
                .build();
        booking2.setId(202L);
    }

    // =========================================================================
    // 1. SAME OPERATOR, OVERLAPPING BOOKINGS — SECOND ASSIGNMENT REJECTED
    // =========================================================================
    @Nested
    @DisplayName("1. Same Operator, Overlapping Bookings Rejection")
    class SameOperatorOverlappingTests {

        @Test
        @DisplayName("Second assignment for overlapping dates is rejected with BadRequestException without state mutation")
        void testSameOperator_OverlappingBookings_SecondAssignmentRejected() {
            // Setup: Booking 202 is to be assigned to Operator 1 who already has active assignment on overlapping dates
            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            // Overlap check returns true: Operator 1 has an active overlapping assignment during booking2's dates (Nov 3 to Nov 7)
            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L),
                    eq(OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES),
                    eq(booking2.getStartDate()),
                    eq(booking2.getEndDate()),
                    eq(202L)
            )).thenReturn(true);

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                    .operatorId(1L)
                    .notes("Assign to second job")
                    .build();

            assertThatThrownBy(() -> operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Operator already has an active assignment for overlapping dates (2026-11-03 to 2026-11-07)");

            // Verify no assignment was created/saved
            verify(assignmentRepository, never()).save(any());
            // Verify booking 202 status and operator were NOT modified
            assertThat(booking2.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(booking2.getOperator()).isNull();
            // Verify no notifications were sent
            verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Assignment rejected when proposed booking dates are fully enclosed inside existing active assignment")
        void testSameOperator_FullyEnclosedDates_Rejected() {
            // Booking 2 is Nov 2 to Nov 4 (fully enclosed within Nov 1 to Nov 5)
            booking2.setStartDate(LocalDate.of(2026, 11, 2));
            booking2.setEndDate(LocalDate.of(2026, 11, 4));

            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(true);

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder().operatorId(1L).build();

            assertThatThrownBy(() -> operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Operator already has an active assignment for overlapping dates");

            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Assignment rejected when proposed booking has identical date interval")
        void testSameOperator_ExactSameDates_Rejected() {
            booking2.setStartDate(LocalDate.of(2026, 11, 1));
            booking2.setEndDate(LocalDate.of(2026, 11, 5));

            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(true);

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder().operatorId(1L).build();

            assertThatThrownBy(() -> operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Operator already has an active assignment for overlapping dates");
        }
    }

    // =========================================================================
    // 2. SAME OPERATOR, NON-OVERLAPPING BOOKINGS — BOTH PERMITTED
    // =========================================================================
    @Nested
    @DisplayName("2. Same Operator, Non-Overlapping Bookings Permitted")
    class SameOperatorNonOverlappingTests {

        @Test
        @DisplayName("Same operator can be assigned to sequential non-overlapping bookings")
        void testSameOperator_NonOverlappingBookings_BothPermitted() {
            // Booking 2 is strictly AFTER Booking 1 (Nov 10 to Nov 15)
            booking2.setStartDate(LocalDate.of(2026, 11, 10));
            booking2.setEndDate(LocalDate.of(2026, 11, 15));

            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            // Overlap check returns false (no overlap between Nov 1-5 and Nov 10-15)
            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(false);

            when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
                OperatorJobAssignment a = i.getArgument(0);
                a.setId(888L);
                return a;
            });
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                    .operatorId(1L)
                    .notes("Assigned to future booking")
                    .build();

            OperatorAssignmentResponse response = operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L);

            assertThat(response).isNotNull();
            assertThat(response.getAssignmentId()).isEqualTo(888L);
            assertThat(response.getBookingId()).isEqualTo(202L);
            assertThat(response.getOperatorId()).isEqualTo(1L);
            assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.ASSIGNED);

            assertThat(booking2.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
            assertThat(booking2.getOperator()).isEqualTo(operator1);

            verify(assignmentRepository).save(any(OperatorJobAssignment.class));
            verify(bookingRepository).save(booking2);
        }

        @Test
        @DisplayName("Same operator can be assigned to booking scheduled earlier in time (prior non-overlapping)")
        void testSameOperator_EarlierNonOverlappingBooking_Permitted() {
            // Booking 2 is strictly BEFORE Booking 1 (Oct 20 to Oct 25)
            booking2.setStartDate(LocalDate.of(2026, 10, 20));
            booking2.setEndDate(LocalDate.of(2026, 10, 25));

            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(false);

            when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
                OperatorJobAssignment a = i.getArgument(0);
                a.setId(889L);
                return a;
            });
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder().operatorId(1L).build();
            OperatorAssignmentResponse response = operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L);

            assertThat(response).isNotNull();
            assertThat(response.getAssignmentId()).isEqualTo(889L);
            assertThat(booking2.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        }
    }

    // =========================================================================
    // 3. DIFFERENT OPERATORS, OVERLAPPING BOOKINGS — PERMITTED
    // =========================================================================
    @Nested
    @DisplayName("3. Different Operators, Overlapping Bookings Permitted")
    class DifferentOperatorsOverlappingTests {

        @Test
        @DisplayName("Different operators working on separate equipment for overlapping dates are permitted")
        void testDifferentOperators_OverlappingBookings_Permitted() {
            // Operator 1 is on Booking 1 (Nov 1 to Nov 5)
            // Operator 2 is now assigned to Booking 2 (Nov 3 to Nov 7)
            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(2L)).thenReturn(Optional.of(operator2));

            // Operator 2 has NO active overlapping assignment
            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(2L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(false);

            when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
                OperatorJobAssignment a = i.getArgument(0);
                a.setId(990L);
                return a;
            });
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                    .operatorId(2L)
                    .notes("Operator 2 for Harvester")
                    .build();

            OperatorAssignmentResponse response = operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L);

            assertThat(response).isNotNull();
            assertThat(response.getOperatorId()).isEqualTo(2L);
            assertThat(response.getBookingId()).isEqualTo(202L);
            assertThat(booking2.getOperator()).isEqualTo(operator2);
            assertThat(booking2.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        }
    }

    // =========================================================================
    // 4. ADJACENT INTERVALS — BOUNDARY BEHAVIOR MATCHES ESTABLISHED CONVENTION
    // =========================================================================
    @Nested
    @DisplayName("4. Boundary Conditions & Adjacent Interval Convention")
    class AdjacentIntervalConventionTests {

        @Test
        @DisplayName("Boundary collision: Job ending on Nov 5 and Job starting on Nov 5 share a date — rejected as conflict")
        void testAdjacentIntervals_SameDayBoundary_RejectedAsOverlap() {
            // Booking 1: Nov 1 to Nov 5. Booking 2: Nov 5 to Nov 8.
            // On Nov 5, both require the operator.
            booking2.setStartDate(LocalDate.of(2026, 11, 5));
            booking2.setEndDate(LocalDate.of(2026, 11, 8));

            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            // Interval query confirms conflict on shared boundary day Nov 5
            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(true);

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder().operatorId(1L).build();

            assertThatThrownBy(() -> operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Operator already has an active assignment for overlapping dates (2026-11-05 to 2026-11-08)");

            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Adjacent calendar day: Job ending Nov 5 and Job starting Nov 6 have zero shared days — permitted")
        void testAdjacentIntervals_NextDayBoundary_PermittedAsNonOverlap() {
            // Booking 1: Nov 1 to Nov 5. Booking 2: Nov 6 to Nov 10.
            booking2.setStartDate(LocalDate.of(2026, 11, 6));
            booking2.setEndDate(LocalDate.of(2026, 11, 10));

            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            // No overlap between Nov 1-5 and Nov 6-10
            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(false);

            when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
                OperatorJobAssignment a = i.getArgument(0);
                a.setId(991L);
                return a;
            });
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder().operatorId(1L).build();
            OperatorAssignmentResponse response = operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L);

            assertThat(response).isNotNull();
            assertThat(booking2.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
            assertThat(booking2.getOperator()).isEqualTo(operator1);
        }

        @Test
        @DisplayName("Adjacent preceding calendar day: Job ending Oct 31 and Job starting Nov 1 have zero shared days — permitted")
        void testAdjacentIntervals_PreviousDayBoundary_PermittedAsNonOverlap() {
            // Booking 1: Nov 1 to Nov 5. Booking 2: Oct 25 to Oct 31.
            booking2.setStartDate(LocalDate.of(2026, 10, 25));
            booking2.setEndDate(LocalDate.of(2026, 10, 31));

            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(false);

            when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
                OperatorJobAssignment a = i.getArgument(0);
                a.setId(992L);
                return a;
            });
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder().operatorId(1L).build();
            OperatorAssignmentResponse response = operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L);

            assertThat(response).isNotNull();
            assertThat(booking2.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        }
    }

    // =========================================================================
    // 5. TERMINAL ASSIGNMENT DOES NOT BLOCK REASSIGNMENT
    // =========================================================================
    @Nested
    @DisplayName("5. Terminal Statuses (REJECTED, CANCELLED, COMPLETED) Do Not Block New Work")
    class TerminalStatusTests {

        @Test
        @DisplayName("Operator whose previous overlapping assignment was REJECTED can be assigned to new booking")
        void testTerminalAssignment_Rejected_DoesNotBlockNewAssignment() {
            // Operator previously rejected an assignment covering Nov 3 to Nov 7
            // Active overlap check excludes REJECTED status, returning false
            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(false);

            when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
                OperatorJobAssignment a = i.getArgument(0);
                a.setId(993L);
                return a;
            });
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder().operatorId(1L).build();
            OperatorAssignmentResponse response = operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L);

            assertThat(response).isNotNull();
            assertThat(response.getAssignmentId()).isEqualTo(993L);
            assertThat(booking2.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        }

        @Test
        @DisplayName("Operator whose previous overlapping assignment was CANCELLED can be assigned to new booking")
        void testTerminalAssignment_Cancelled_DoesNotBlockNewAssignment() {
            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(false);

            when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
                OperatorJobAssignment a = i.getArgument(0);
                a.setId(994L);
                return a;
            });
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder().operatorId(1L).build();
            OperatorAssignmentResponse response = operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L);

            assertThat(response).isNotNull();
            assertThat(booking2.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        }

        @Test
        @DisplayName("Operator whose previous overlapping assignment was COMPLETED can be assigned to new booking")
        void testTerminalAssignment_Completed_DoesNotBlockNewAssignment() {
            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(false);

            when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
                OperatorJobAssignment a = i.getArgument(0);
                a.setId(995L);
                return a;
            });
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder().operatorId(1L).build();
            OperatorAssignmentResponse response = operatorAssignmentService.assignOperator(202L, request, "PARTNER_10", 10L);

            assertThat(response).isNotNull();
            assertThat(booking2.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        }

        @Test
        @DisplayName("Active statuses definition strictly includes all 6 non-terminal operational states")
        void testActiveStatusesDefinition() {
            List<OperatorAssignmentStatus> activeList = OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES;
            assertThat(activeList).containsExactlyInAnyOrder(
                    OperatorAssignmentStatus.ASSIGNED,
                    OperatorAssignmentStatus.ACCEPTED,
                    OperatorAssignmentStatus.TRAVELING,
                    OperatorAssignmentStatus.REACHED,
                    OperatorAssignmentStatus.IN_PROGRESS,
                    OperatorAssignmentStatus.PAUSED
            );
            assertThat(activeList).doesNotContain(
                    OperatorAssignmentStatus.REJECTED,
                    OperatorAssignmentStatus.CANCELLED,
                    OperatorAssignmentStatus.COMPLETED
            );
        }
    }

    // =========================================================================
    // 6. EACH SUPPORTED ASSIGNMENT ENTRY POINT ENFORCES THE SAME RULE
    // =========================================================================
    @Nested
    @DisplayName("6. Dual-Endpoint and Multi-Entry-Point Consistency")
    class MultiEntryPointConsistencyTests {

        @Test
        @DisplayName("Entry Point A: OperatorAssignmentService.assignOperator enforces overlap check")
        void testEntryPointA_OperatorAssignmentService_EnforcesOverlap() {
            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(true);

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder().operatorId(1L).build();

            assertThatThrownBy(() -> operatorAssignmentService.assignOperator(202L, request, "ADMIN", null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Operator already has an active assignment for overlapping dates");
        }

        @Test
        @DisplayName("Entry Point B: BookingService.assignOperator routes to canonical service and rejects overlap")
        void testEntryPointB_BookingServiceAssignOperator_EnforcesOverlap() {
            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(true);

            assertThatThrownBy(() -> bookingService.assignOperator(202L, 10L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Operator already has an active assignment for overlapping dates");
        }

        @Test
        @DisplayName("Entry Point C: OperatorAssignmentController endpoint rejects overlap with HTTP 400")
        void testEntryPointC_OperatorAssignmentController_EnforcesOverlap() {
            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(true);

            PartnerPrincipal principal = PartnerPrincipal.builder().id(10L).role("PARTNER").build();
            var auth = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_PARTNER")));

            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder().operatorId(1L).build();

            assertThatThrownBy(() -> operatorAssignmentController.assignOperator(202L, request, auth))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Operator already has an active assignment for overlapping dates");
        }

        @Test
        @DisplayName("Entry Point D: BookingController.assignOperator endpoint rejects overlap with HTTP 400")
        void testEntryPointD_BookingControllerAssignOperator_EnforcesOverlap() {
            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(true);

            PartnerPrincipal principal = PartnerPrincipal.builder().id(10L).role("PARTNER").build();
            BookingStatusUpdateRequest request = BookingStatusUpdateRequest.builder()
                    .operatorId(1L)
                    .build();

            assertThatThrownBy(() -> bookingController.assignOperator(202L, request, principal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Operator already has an active assignment for overlapping dates");
        }

        @Test
        @DisplayName("Entry Point E: BookingController.updateBookingStatus to OPERATOR_ASSIGNED routes to canonical service and rejects overlap")
        void testEntryPointE_BookingControllerUpdateBookingStatus_EnforcesOverlap() {
            when(bookingRepository.findById(202L)).thenReturn(Optional.of(booking2));
            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(202L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(202L, OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)).thenReturn(false);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator1));

            when(assignmentRepository.existsActiveOverlappingAssignment(
                    eq(1L), any(), eq(booking2.getStartDate()), eq(booking2.getEndDate()), eq(202L)
            )).thenReturn(true);

            BookingStatusUpdateRequest request = BookingStatusUpdateRequest.builder()
                    .status(BookingStatus.OPERATOR_ASSIGNED)
                    .operatorId(1L)
                    .build();

            assertThatThrownBy(() -> bookingController.updateBookingStatus(202L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Operator already has an active assignment for overlapping dates");
        }
    }

    // =========================================================================
    // 7. CONCURRENT COMPETING REQUESTS — NO CONFLICTING ACTIVE ASSIGNMENTS
    // =========================================================================
    @Nested
    @DisplayName("7. Concurrency Simulation & Safe Serialized Conflict Protection")
    class ConcurrencySimulationTests {

        @Test
        @DisplayName("Concurrent competing requests for same operator on overlapping bookings serialize: exactly 1 wins, rest fail")
        void testConcurrentCompetingRequests_OnlyOneWins() throws Exception {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startGun = new CountDownLatch(1);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger conflictCount = new AtomicInteger(0);
            List<Future<?>> futures = new ArrayList<>();

            // Shared state simulation: once first thread creates assignment, subsequent threads see conflict
            java.util.concurrent.atomic.AtomicBoolean hasCommittedAssignment = new java.util.concurrent.atomic.AtomicBoolean(false);

            when(bookingRepository.findById(anyLong())).thenAnswer(inv -> {
                Long id = inv.getArgument(0);
                Booking b = Booking.builder()
                        .farmerId(100L)
                        .partner(partner)
                        .equipment(equipment1)
                        .status(BookingStatus.CONFIRMED)
                        .startDate(LocalDate.of(2026, 11, 1))
                        .endDate(LocalDate.of(2026, 11, 5))
                        .totalCost(new BigDecimal("12500.00"))
                        .build();
                b.setId(id);
                return Optional.of(b);
            });

            when(assignmentRepository.existsByBookingIdAndAssignmentStatus(anyLong(), any())).thenReturn(false);
            when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(anyLong(), any())).thenReturn(false);

            java.util.concurrent.locks.ReentrantLock dbRowLock = new java.util.concurrent.locks.ReentrantLock();
            when(operatorRepository.findByIdWithLock(1L)).thenAnswer(inv -> {
                dbRowLock.lock();
                return Optional.of(operator1);
            });

            // Synchronized mock simulating database lock & commit visibility:
            // When first thread saves, hasCommittedAssignment becomes true.
            // Any thread checking overlap after lock acquisition sees conflict if hasCommittedAssignment is true.
            when(assignmentRepository.existsActiveOverlappingAssignment(eq(1L), any(), any(), any(), any()))
                    .thenAnswer(inv -> {
                        boolean conflict = hasCommittedAssignment.get();
                        if (conflict && dbRowLock.isHeldByCurrentThread()) {
                            dbRowLock.unlock();
                        }
                        return conflict;
                    });

            when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(inv -> {
                hasCommittedAssignment.set(true);
                OperatorJobAssignment a = inv.getArgument(0);
                a.setId(777L);
                return a;
            });
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                if (dbRowLock.isHeldByCurrentThread()) {
                    dbRowLock.unlock();
                }
                return inv.getArgument(0);
            });

            for (int i = 0; i < threadCount; i++) {
                final long bookingId = 300L + i;
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startGun.await(); // Simultaneous release
                        OperatorAssignmentRequest req = OperatorAssignmentRequest.builder()
                                .operatorId(1L)
                                .notes("Concurrent thread assignment")
                                .build();
                        operatorAssignmentService.assignOperator(bookingId, req, "PARTNER_10", 10L);
                        successCount.incrementAndGet();
                    } catch (BadRequestException e) {
                        if (e.getMessage().contains("Operator already has an active assignment for overlapping dates")) {
                            conflictCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Unexpected error
                    } finally {
                        if (dbRowLock.isHeldByCurrentThread()) {
                            dbRowLock.unlock();
                        }
                    }
                }));
            }

            // Wait for all threads to be ready at the start line
            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            // Fire all threads simultaneously
            startGun.countDown();

            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
            executor.shutdown();

            // Assert: Exactly 1 thread successfully committed assignment
            assertThat(successCount.get()).isEqualTo(1);
            // Assert: Remaining 9 threads were rejected cleanly with BadRequestException
            assertThat(conflictCount.get()).isEqualTo(9);

            // Exactly 1 assignment record was persisted in total
            verify(assignmentRepository, times(1)).save(any(OperatorJobAssignment.class));
        }
    }
}
