package com.agrorental.operator;

import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Targeted Regression Tests for E2E-001:
 * Validates that all operator-assignment paths delegate to the canonical assignment operation,
 * maintaining consistent Booking (status = OPERATOR_ASSIGNED, operator set) and
 * OperatorJobAssignment (status = ASSIGNED) records without duplicate assignments or partial updates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("E2E-001: Operator Assignment Dual-Endpoint Consistency Regression Tests")
class OperatorAssignmentE2E001RegressionTest {

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

    private Partner partner;
    private Equipment equipment;
    private Booking confirmedBooking;
    private Operator approvedOperator;

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

        partner = Partner.builder()
                .fullName("Suresh Patil")
                .businessName("Patil Agro Services")
                .mobileNumber("9876543210")
                .build();
        partner.setId(10L);

        equipment = Equipment.builder()
                .name("Mahindra 575 DI Tractor")
                .partner(partner)
                .rentalPrice(new BigDecimal("2500.00"))
                .availabilityStatus(AvailabilityStatus.BOOKED)
                .build();
        equipment.setId(50L);

        confirmedBooking = Booking.builder()
                .farmerId(100L)
                .partner(partner)
                .equipment(equipment)
                .status(BookingStatus.CONFIRMED)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 5))
                .totalCost(new BigDecimal("12500.00"))
                .build();
        confirmedBooking.setId(200L);

        approvedOperator = Operator.builder()
                .fullName("Rajesh Shinde")
                .mobileNumber("9123456780")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .skills("Tractor Ploughing")
                .build();
        approvedOperator.setId(1L);
        approvedOperator.setActive(true);
    }

    @Test
    @DisplayName("1. Partner assignment updates Booking status to OPERATOR_ASSIGNED and persists OperatorJobAssignment in ASSIGNED status")
    void testPartnerAssignment_UpdatesBookingStatusAndPersistsAssignmentRecord() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(200L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));
        when(assignmentRepository.existsActiveOverlappingAssignment(eq(1L), any(), any(), any(), any())).thenReturn(false);

        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
            OperatorJobAssignment a = i.getArgument(0);
            a.setId(901L);
            return a;
        });
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .notes("Ploughing 5 acres")
                .build();

        OperatorAssignmentResponse response = operatorAssignmentService.assignOperator(200L, request, "PARTNER_10", 10L);

        assertThat(response).isNotNull();
        assertThat(response.getAssignmentId()).isEqualTo(901L);
        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.ASSIGNED);
        assertThat(response.getOperatorId()).isEqualTo(1L);

        // Verify Booking entity was mutated and saved with OPERATOR_ASSIGNED
        assertThat(confirmedBooking.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        assertThat(confirmedBooking.getOperator()).isEqualTo(approvedOperator);

        verify(bookingRepository).save(confirmedBooking);
        verify(assignmentRepository).save(any(OperatorJobAssignment.class));
    }

    @Test
    @DisplayName("2. Operator assigned-jobs query retrieves the created OperatorJobAssignment")
    void testOperatorAssignedJobsQuery_ReturnsPersistedAssignment() {
        OperatorJobAssignment savedAssignment = OperatorJobAssignment.builder()
                .operator(approvedOperator)
                .booking(confirmedBooking)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .assignedBy("PARTNER_10")
                .build();
        savedAssignment.setId(901L);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));
        when(assignmentRepository.findByOperatorId(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(savedAssignment)));

        Page<OperatorAssignedJobResponse> jobsPage = operatorAssignmentService.getAssignedJobs(1L, PageRequest.of(0, 10));

        assertThat(jobsPage).isNotEmpty();
        assertThat(jobsPage.getTotalElements()).isEqualTo(1);
        OperatorAssignedJobResponse job = jobsPage.getContent().get(0);
        assertThat(job.getAssignmentId()).isEqualTo(901L);
        assertThat(job.getBookingId()).isEqualTo(200L);
        assertThat(job.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.ASSIGNED);
        assertThat(job.getEquipmentName()).isEqualTo("Mahindra 575 DI Tractor");
    }

    @Test
    @DisplayName("3. Alternative endpoint (BookingService.assignOperator) routes to canonical assignment with consistent records")
    void testBookingServiceAssignOperator_ProducesIdenticalConsistentState() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(200L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));
        when(assignmentRepository.existsActiveOverlappingAssignment(eq(1L), any(), any(), any(), any())).thenReturn(false);

        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
            OperatorJobAssignment a = i.getArgument(0);
            a.setId(902L);
            return a;
        });
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.assignOperator(200L, 10L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        assertThat(confirmedBooking.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        assertThat(confirmedBooking.getOperator()).isEqualTo(approvedOperator);

        // Verify assignment record was saved
        ArgumentCaptor<OperatorJobAssignment> captor = ArgumentCaptor.forClass(OperatorJobAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        OperatorJobAssignment captured = captor.getValue();
        assertThat(captured.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.ASSIGNED);
        assertThat(captured.getOperator().getId()).isEqualTo(1L);
        assertThat(captured.getBooking().getId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("4. Admin status update to OPERATOR_ASSIGNED routes through canonical assignment service")
    void testAdminBookingStatusUpdate_RoutesThroughCanonicalAssignment() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(200L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));
        when(assignmentRepository.existsActiveOverlappingAssignment(eq(1L), any(), any(), any(), any())).thenReturn(false);

        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
            OperatorJobAssignment a = i.getArgument(0);
            a.setId(903L);
            return a;
        });
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingStatusUpdateRequest updateRequest = BookingStatusUpdateRequest.builder()
                .status(BookingStatus.OPERATOR_ASSIGNED)
                .operatorId(1L)
                .build();

        BookingResponse response = bookingService.updateBookingStatus(200L, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        assertThat(confirmedBooking.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        verify(assignmentRepository).save(any(OperatorJobAssignment.class));
    }

    @Test
    @DisplayName("5. Repeated assignment request is rejected and does not create duplicate active assignments")
    void testRepeatedAssignment_DoesNotCreateDuplicateActiveAssignments() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(200L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(true);

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> operatorAssignmentService.assignOperator(200L, request, "PARTNER_10", 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Booking is already assigned to an active operator");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Unauthorized partner cannot assign operator to another partner's booking")
    void testUnauthorizedPartner_CannotAssignOperatorToAnotherPartnersBooking() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(confirmedBooking));

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        // Partner 999 attempts to assign Partner 10's booking
        assertThatThrownBy(() -> operatorAssignmentService.assignOperator(200L, request, "PARTNER_999", 999L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unauthorized: Booking does not belong to the specified partner.");

        verify(assignmentRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Ineligible operator (inactive or unapproved) is rejected")
    void testIneligibleOperator_Rejected() {
        approvedOperator.setActive(false);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(200L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> operatorAssignmentService.assignOperator(200L, request, "PARTNER_10", 10L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator account is inactive");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. Invalid booking status (e.g. PENDING) is rejected")
    void testInvalidBookingStatus_Rejected() {
        confirmedBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(confirmedBooking));

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> operatorAssignmentService.assignOperator(200L, request, "PARTNER_10", 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not eligible for operator assignment");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("9. Duplicate assignment is prevented across all active states: ACCEPTED, TRAVELING, REACHED, IN_PROGRESS, PAUSED")
    void testDuplicatePrevention_AcrossAllActiveAssignmentStates() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(200L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(eq(200L), any())).thenReturn(true);

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> operatorAssignmentService.assignOperator(200L, request, "PARTNER_10", 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Booking is already assigned to an active operator");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("10. Reassignment after operator rejection clears stale reference and leaves exactly one active assignment")
    void testReassignmentAfterRejection_ClearsStaleReferenceAndCreatesSingleActiveAssignment() {
        // Step 1: Old assignment 1 was rejected by Operator 1
        OperatorJobAssignment oldRejectedAssignment = OperatorJobAssignment.builder()
                .operator(approvedOperator)
                .booking(confirmedBooking)
                .assignmentStatus(OperatorAssignmentStatus.REJECTED)
                .assignedAt(LocalDateTime.now().minusHours(2))
                .assignedBy("PARTNER_10")
                .rejectionReason("Prior machinery engagement conflict")
                .build();
        oldRejectedAssignment.setId(501L);

        // Booking status returned to CONFIRMED with operator cleared
        confirmedBooking.setStatus(BookingStatus.CONFIRMED);
        confirmedBooking.setOperator(null);

        // Step 2: Prepare Operator 2 for reassignment
        Operator secondOperator = Operator.builder()
                .fullName("Vikram Patil")
                .mobileNumber("9876543211")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        secondOperator.setId(2L);
        secondOperator.setActive(true);

        when(bookingRepository.findById(200L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(200L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(assignmentRepository.existsByBookingIdAndAssignmentStatusIn(eq(200L), any())).thenReturn(false);
        when(operatorRepository.findById(2L)).thenReturn(Optional.of(secondOperator));
        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(i -> {
            OperatorJobAssignment a = i.getArgument(0);
            a.setId(502L);
            return a;
        });
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        OperatorAssignmentRequest reassignRequest = OperatorAssignmentRequest.builder()
                .operatorId(2L)
                .notes("Reassigned after previous operator declined")
                .build();

        OperatorAssignmentResponse response = operatorAssignmentService.assignOperator(
                200L, reassignRequest, "PARTNER_10", 10L);

        assertThat(response).isNotNull();
        assertThat(response.getAssignmentId()).isEqualTo(502L);
        assertThat(response.getOperatorId()).isEqualTo(2L);
        assertThat(response.getOperatorName()).isEqualTo("Vikram Patil");
        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.ASSIGNED);

        // Assert that Booking now references Operator 2 and NOT Operator 1
        assertThat(confirmedBooking.getStatus()).isEqualTo(BookingStatus.OPERATOR_ASSIGNED);
        assertThat(confirmedBooking.getOperator()).isEqualTo(secondOperator);
        assertThat(confirmedBooking.getOperator().getId()).isEqualTo(2L);

        // Assert that old assignment remains REJECTED and new assignment is ASSIGNED
        assertThat(oldRejectedAssignment.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.REJECTED);
        verify(assignmentRepository).save(any(OperatorJobAssignment.class));
    }

    @Test
    @DisplayName("11. Reassignment to a cancelled booking is rejected and creates no assignment")
    void testReassignmentToCancelledBooking_IsRejected() {
        confirmedBooking.setStatus(BookingStatus.CANCELLED);
        confirmedBooking.setOperator(null);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(confirmedBooking));

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> operatorAssignmentService.assignOperator(200L, request, "PARTNER_10", 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Booking with status CANCELLED is not eligible for operator assignment");

        verify(assignmentRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }
}
