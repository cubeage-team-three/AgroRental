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

        testBooking = Booking.builder()
                .farmerId(20L)
                .equipment(equipment)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .totalCost(BigDecimal.valueOf(4500))
                .status(BookingStatus.CONFIRMED)
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
    @DisplayName("Should reject lifecycle action when operator is unverified")
    void testUnverifiedOperatorRejected() {
        testOperator.setMobileVerified(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        assertThatThrownBy(() -> lifecycleService.acceptJob(500L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator mobile number is not verified");
    }
}
