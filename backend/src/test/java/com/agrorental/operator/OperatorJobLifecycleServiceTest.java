package com.agrorental.operator;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.dto.OperatorJobCompletionRequest;
import com.agrorental.operator.dto.OperatorJobPauseRequest;
import com.agrorental.operator.dto.OperatorJobRejectionRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.mapper.OperatorJobAssignmentMapper;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorJobPauseIntervalRepository;
import com.agrorental.operator.repository.OperatorLocationRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorJobLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorJobLifecycleService Business Logic Unit Tests")
class OperatorJobLifecycleServiceTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorJobAssignmentRepository assignmentRepository;

    @Mock
    private OperatorLocationRepository locationRepository;

    @Mock
    private OperatorJobPauseIntervalRepository pauseIntervalRepository;

    @Mock
    private com.agrorental.booking.repository.BookingRepository bookingRepository;

    @Mock
    private NotificationService notificationService;

    @Spy
    private OperatorJobAssignmentMapper assignmentMapper = new OperatorJobAssignmentMapper();

    @InjectMocks
    private OperatorJobLifecycleService lifecycleService;

    private Operator testOperator;
    private Booking testBooking;
    private OperatorJobAssignment testAssignment;

    @BeforeEach
    void setUp() {
        testOperator = Operator.builder()
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        testOperator.setId(1L);
        testOperator.setActive(true);

        Equipment equipment = Equipment.builder()
                .name("Mahindra 575 DI Tractor")
                .category(EquipmentCategory.TRACTOR)
                .rentalPrice(BigDecimal.valueOf(1500))
                .build();
        equipment.setId(10L);

        // A booking whose operator phase is running, assigned to this job's operator (as the canonical
        // assignment leaves it)
        testBooking = Booking.builder()
                .farmerId(20L)
                .equipment(equipment)
                .operator(testOperator)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .totalCost(BigDecimal.valueOf(4500))
                .status(BookingStatus.OPERATOR_ASSIGNED)
                .deliveryAddress("Pune Farm Plot 10")
                .build();
        testBooking.setId(100L);

        testAssignment = OperatorJobAssignment.builder()
                .operator(testOperator)
                .booking(testBooking)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .assignedBy("PARTNER_ADMIN")
                .notes("Tractor ploughing task")
                .build();
        testAssignment.setId(500L);
    }

    // ==========================================
    // 1. ACCEPT JOB TESTS
    // ==========================================

    @Test
    @DisplayName("Should successfully accept assigned job (ASSIGNED -> ACCEPTED)")
    void testAcceptJobSuccess() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        OperatorAssignedJobResponse response = lifecycleService.acceptJob(500L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.ACCEPTED);
        assertThat(response.getAcceptedAt()).isNotNull();
        verify(assignmentRepository).save(testAssignment);
    }

    @Test
    @DisplayName("Should reject accept when current status is already ACCEPTED or invalid")
    void testAcceptJobInvalidTransition() {
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.ACCEPTED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));

        assertThatThrownBy(() -> lifecycleService.acceptJob(500L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid job status transition from ACCEPTED to ACCEPTED");
    }

    // ==========================================
    // 2. REJECT JOB TESTS
    // ==========================================

    @Test
    @DisplayName("Should successfully decline assigned job with stated reason (ASSIGNED -> REJECTED)")
    void testRejectJobSuccess() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        OperatorJobRejectionRequest request = OperatorJobRejectionRequest.builder()
                .rejectionReason("Prior machinery engagement conflict")
                .build();

        OperatorAssignedJobResponse response = lifecycleService.rejectJob(500L, 1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.REJECTED);
        assertThat(response.getRejectedAt()).isNotNull();
        assertThat(response.getRejectionReason()).isEqualTo("Prior machinery engagement conflict");
        // The booking returns to CONFIRMED without an operator, ready for reassignment
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(testBooking.getOperator()).isNull();
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("Rejection notifications quote at most 300 chars of the reason, never split a surrogate pair, and stay <= 500 chars")
    void testRejectNotificationQuotesBoundedReason() {
        String reason = "R".repeat(296) + "😀" + "S".repeat(100);
        testOperator.setFullName("N".repeat(255));
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        OperatorAssignedJobResponse response = lifecycleService.rejectJob(500L, 1L,
                OperatorJobRejectionRequest.builder().rejectionReason(reason).build());

        assertThat(response.getRejectionReason()).isEqualTo(reason);
        org.mockito.ArgumentCaptor<String> message = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendNotification(org.mockito.ArgumentMatchers.eq("FARMER"), any(),
                org.mockito.ArgumentMatchers.eq("Job Declined"), message.capture(), any(), any());
        assertThat(message.getValue()).hasSizeLessThanOrEqualTo(500).endsWith("...");
        assertThat(message.getValue().codePoints().noneMatch(cp -> Character.isSurrogate((char) cp) && cp <= 0xFFFF))
                .as("no lone surrogate").isTrue();
    }

    @Test
    @DisplayName("Should fail rejection when reason is blank or too short")
    void testRejectJobMissingReason() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));

        OperatorJobRejectionRequest invalidRequest = OperatorJobRejectionRequest.builder()
                .rejectionReason("  ")
                .build();

        assertThatThrownBy(() -> lifecycleService.rejectJob(500L, 1L, invalidRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Rejection reason is required");
    }

    // ==========================================
    // 3. START TRAVEL TESTS
    // ==========================================

    @Test
    @DisplayName("Should start traveling from ACCEPTED status (ACCEPTED -> TRAVELING)")
    void testStartTravelSuccess() {
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.ACCEPTED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        OperatorAssignedJobResponse response = lifecycleService.startTravel(500L, 1L);

        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.TRAVELING);
        assertThat(response.getTravelingAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject start travel from ASSIGNED status without prior acceptance")
    void testStartTravelDirectlyFromAssignedFails() {
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.ASSIGNED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));

        assertThatThrownBy(() -> lifecycleService.startTravel(500L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid job status transition from ASSIGNED to TRAVELING");
    }

    // ==========================================
    // 4. MARK REACHED TESTS
    // ==========================================

    @Test
    @DisplayName("Should mark arrival at location (TRAVELING -> REACHED)")
    void testMarkReachedSuccess() {
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.TRAVELING);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        OperatorAssignedJobResponse response = lifecycleService.markReached(500L, 1L);

        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.REACHED);
        assertThat(response.getReachedAt()).isNotNull();
    }

    // ==========================================
    // 5. START WORK TESTS
    // ==========================================

    @Test
    @DisplayName("Should start field operations (REACHED -> IN_PROGRESS)")
    void testStartWorkSuccess() {
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.REACHED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        OperatorAssignedJobResponse response = lifecycleService.startWork(500L, 1L);

        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.IN_PROGRESS);
        assertThat(response.getWorkStartedAt()).isNotNull();
    }

    // ==========================================
    // 6. PAUSE & RESUME WORK TESTS
    // ==========================================

    @Test
    @DisplayName("Should pause field work (IN_PROGRESS -> PAUSED) with mandatory reason")
    void testPauseWorkSuccess() {
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.IN_PROGRESS);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        OperatorJobPauseRequest request = OperatorJobPauseRequest.builder()
                .pauseReason("Heavy monsoon downpour")
                .build();

        OperatorAssignedJobResponse response = lifecycleService.pauseWork(500L, 1L, request);

        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.PAUSED);
        assertThat(response.getPausedAt()).isNotNull();
        assertThat(response.getPauseReason()).isEqualTo("Heavy monsoon downpour");
    }

    @Test
    @DisplayName("Should resume field work (PAUSED -> IN_PROGRESS)")
    void testResumeWorkSuccess() {
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.PAUSED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        OperatorAssignedJobResponse response = lifecycleService.resumeWork(500L, 1L);

        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.IN_PROGRESS);
        assertThat(response.getResumedAt()).isNotNull();
    }

    // ==========================================
    // 7. COMPLETE WORK TESTS
    // ==========================================

    @Test
    @DisplayName("Should complete field work (IN_PROGRESS -> COMPLETED) with completion notes")
    void testCompleteWorkSuccess() {
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.IN_PROGRESS);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        OperatorJobCompletionRequest request = OperatorJobCompletionRequest.builder()
                .completionNotes("Ploughed entire 5-acre sugarcane field")
                .build();

        OperatorAssignedJobResponse response = lifecycleService.completeWork(500L, 1L, request);

        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
        assertThat(response.getCompletionNotes()).isEqualTo("Ploughed entire 5-acre sugarcane field");
    }

    @Test
    @DisplayName("Completion runs at READ_COMMITTED and locks the booking row before changing it (lock order booking -> others)")
    void testCompletionLocksBookingFirst() throws Exception {
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.IN_PROGRESS);
        testBooking.setStatus(BookingStatus.WORK_STARTED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        lifecycleService.completeWork(500L, 1L, null);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(bookingRepository, assignmentRepository);
        order.verify(bookingRepository).findByIdWithLock(100L);
        order.verify(bookingRepository).save(testBooking);
        order.verify(assignmentRepository).save(testAssignment);
        org.springframework.transaction.annotation.Transactional tx = OperatorJobLifecycleService.class
                .getMethod("completeWork", Long.class, Long.class, OperatorJobCompletionRequest.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(tx.isolation()).isEqualTo(org.springframework.transaction.annotation.Isolation.READ_COMMITTED);
    }

    @Test
    @DisplayName("Should reject complete directly from ASSIGNED status")
    void testCompleteDirectlyFromAssignedFails() {
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.ASSIGNED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));

        assertThatThrownBy(() -> lifecycleService.completeWork(500L, 1L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid job status transition from ASSIGNED to COMPLETED");
    }

    @Test
    @DisplayName("Should reject mutating an already COMPLETED job")
    void testMutatingCompletedJobFails() {
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.COMPLETED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));

        assertThatThrownBy(() -> lifecycleService.startTravel(500L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid job status transition from COMPLETED to TRAVELING");
    }

    // ==========================================
    // 8. SECURITY & IDOR ENFORCEMENT
    // ==========================================

    @Test
    @DisplayName("Should block IDOR when Operator A attempts to mutate Operator B's assignment")
    void testIdorBlockedForDifferentOperator() {
        Operator otherOperator = Operator.builder().fullName("Other Operator").build();
        otherOperator.setId(2L);
        testAssignment.setOperator(otherOperator);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));

        assertThatThrownBy(() -> lifecycleService.acceptJob(500L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied: You do not have permission to modify this assignment");
    }

    // ==========================================
    // 9. BOOKING CONSISTENCY GUARD
    // ==========================================

    @Test
    @DisplayName("Should refuse any job step once the booking was closed elsewhere, and leave the booking untouched")
    void testClosedBookingFreezesJob() {
        for (BookingStatus closed : new BookingStatus[]{BookingStatus.COMPLETED, BookingStatus.CANCELLED,
                BookingStatus.REJECTED, BookingStatus.CONFIRMED}) {
            testBooking.setStatus(closed);
            testAssignment.setAssignmentStatus(OperatorAssignmentStatus.ACCEPTED);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
            when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));

            assertThatThrownBy(() -> lifecycleService.startTravel(500L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Booking #100 is " + closed + "; this job can no longer be updated.");
            assertThat(testBooking.getStatus()).isEqualTo(closed);
            assertThat(testAssignment.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.ACCEPTED);
        }
        verify(bookingRepository, never()).save(any());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should refuse a job step when the booking now belongs to another operator")
    void testRepointedBookingFreezesJob() {
        Operator otherOperator = Operator.builder().fullName("Other Operator").build();
        otherOperator.setId(2L);
        testBooking.setOperator(otherOperator);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));

        assertThatThrownBy(() -> lifecycleService.rejectJob(500L, 1L,
                OperatorJobRejectionRequest.builder().rejectionReason("Not available").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Booking #100 is no longer assigned to you; this job can no longer be updated.");
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        assertThat(testBooking.getOperator()).isSameAs(otherOperator);
    }

    @Test
    @DisplayName("Should never move the booking backwards when it is already further along")
    void testBookingNeverMovesBackwards() {
        testBooking.setStatus(BookingStatus.WORK_STARTED);
        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.ACCEPTED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        OperatorAssignedJobResponse response = lifecycleService.startTravel(500L, 1L);

        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.TRAVELING);
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.WORK_STARTED);
    }

    @Test
    @DisplayName("Should move the booking forward with the job (OPERATOR_ASSIGNED -> ON_THE_WAY -> WORK_STARTED -> COMPLETED)")
    void testBookingMovesForwardWithJob() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        testAssignment.setAssignmentStatus(OperatorAssignmentStatus.ACCEPTED);
        lifecycleService.startTravel(500L, 1L);
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.ON_THE_WAY);
        lifecycleService.markReached(500L, 1L);
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.ON_THE_WAY);
        lifecycleService.startWork(500L, 1L);
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.WORK_STARTED);
        lifecycleService.completeWork(500L, 1L, null);
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(testAssignment.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should reject lifecycle action when operator account is inactive")
    void testInactiveOperatorRejected() {
        testOperator.setActive(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        assertThatThrownBy(() -> lifecycleService.acceptJob(500L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator account is inactive");
    }

    @Test
    @DisplayName("Contract v1.1: an APPROVED, active operator with an unverified mobile number can act on its job")
    void testUnverifiedOperatorRejected() {
        testOperator.setMobileVerified(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> i.getArgument(0));

        OperatorAssignedJobResponse response = lifecycleService.acceptJob(500L, 1L);

        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.ACCEPTED);
        assertThat(testOperator.isMobileVerified()).as("no workaround: the flag is left untouched").isFalse();
    }

    @Test
    @DisplayName("Contract v1.1: a PENDING or REJECTED operator stays blocked from lifecycle actions")
    void testNotApprovedOperatorRejected() {
        for (OperatorStatus status : new OperatorStatus[]{OperatorStatus.PENDING, OperatorStatus.REJECTED}) {
            testOperator.setStatus(status);
            when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

            assertThatThrownBy(() -> lifecycleService.acceptJob(500L, 1L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Operator is not approved");
        }
        assertThat(testAssignment.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.ASSIGNED);
    }
}
