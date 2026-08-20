package com.agrorental.operator;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.operator.dto.EligibleOperatorResponse;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.dto.OperatorAssignmentRequest;
import com.agrorental.operator.dto.OperatorAssignmentResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.mapper.OperatorJobAssignmentMapper;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorAssignmentService Business Logic Unit Tests")
class OperatorAssignmentServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorJobAssignmentRepository assignmentRepository;

    @Mock
    private NotificationService notificationService;

    @Spy
    private OperatorJobAssignmentMapper assignmentMapper = new OperatorJobAssignmentMapper();

    @InjectMocks
    private OperatorAssignmentService assignmentService;

    private Booking confirmedBooking;
    private Operator approvedOperator;
    private Equipment testEquipment;

    @BeforeEach
    void setUp() {
        testEquipment = Equipment.builder()
                .name("Mahindra 575 DI Tractor")
                .category(EquipmentCategory.TRACTOR)
                .rentalPrice(BigDecimal.valueOf(1500))
                .build();
        testEquipment.setId(10L);

        confirmedBooking = Booking.builder()
                .farmerId(20L)
                .equipment(testEquipment)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(4))
                .totalCost(BigDecimal.valueOf(4500))
                .status(BookingStatus.CONFIRMED)
                .deliveryAddress("Farm Plot 12, Pune")
                .build();
        confirmedBooking.setId(100L);

        approvedOperator = Operator.builder()
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .email("rajesh@agrorental.com")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .experience(6)
                .skills("Tractor Operation")
                .build();
        approvedOperator.setId(1L);
        approvedOperator.setActive(true);
    }

    @Test
    @DisplayName("Should successfully assign eligible operator to confirmed booking")
    void testAssignOperatorSuccess() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(100L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));
        when(assignmentRepository.existsOverlappingAssignment(
                eq(1L), eq(OperatorAssignmentStatus.ASSIGNED), eq(confirmedBooking.getStartDate()), eq(confirmedBooking.getEndDate()))
        ).thenReturn(false);

        when(assignmentRepository.save(any(OperatorJobAssignment.class))).thenAnswer(invocation -> {
            OperatorJobAssignment saved = invocation.getArgument(0);
            saved.setId(500L);
            return saved;
        });
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .notes("Tractor ploughing task assigned")
                .build();

        OperatorAssignmentResponse response = assignmentService.assignOperator(100L, request, "ADMIN_USER");

        assertThat(response).isNotNull();
        assertThat(response.getAssignmentId()).isEqualTo(500L);
        assertThat(response.getBookingId()).isEqualTo(100L);
        assertThat(response.getOperatorId()).isEqualTo(1L);
        assertThat(response.getOperatorName()).isEqualTo("Rajesh Shinde");
        assertThat(response.getEquipmentName()).isEqualTo("Mahindra 575 DI Tractor");
        assertThat(response.getAssignmentStatus()).isEqualTo(OperatorAssignmentStatus.ASSIGNED);
        assertThat(response.getAssignedBy()).isEqualTo("ADMIN_USER");
        assertThat(response.getNotes()).isEqualTo("Tractor ploughing task assigned");

        verify(bookingRepository).save(confirmedBooking);
        verify(assignmentRepository).save(any(OperatorJobAssignment.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when booking does not exist")
    void testAssignOperatorBookingNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> assignmentService.assignOperator(999L, request, "ADMIN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found with ID: 999");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject assignment when booking status is not CONFIRMED (e.g. PENDING)")
    void testAssignOperatorBookingNotConfirmed() {
        confirmedBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(confirmedBooking));

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> assignmentService.assignOperator(100L, request, "ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not eligible for operator assignment");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject assignment when booking is already assigned to an active operator")
    void testAssignOperatorAlreadyAssigned() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(100L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(true);

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> assignmentService.assignOperator(100L, request, "ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Booking is already assigned to an active operator");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when operator does not exist")
    void testAssignOperatorNotFound() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(100L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(operatorRepository.findById(999L)).thenReturn(Optional.empty());

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(999L)
                .build();

        assertThatThrownBy(() -> assignmentService.assignOperator(100L, request, "ADMIN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Operator not found with ID: 999");
    }

    @Test
    @DisplayName("Should reject assignment when operator account is inactive")
    void testAssignOperatorInactive() {
        approvedOperator.setActive(false);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(100L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> assignmentService.assignOperator(100L, request, "ADMIN"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator account is inactive");
    }

    @Test
    @DisplayName("Should reject assignment when operator is not approved (e.g. PENDING)")
    void testAssignOperatorPendingApproval() {
        approvedOperator.setStatus(OperatorStatus.PENDING);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(100L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> assignmentService.assignOperator(100L, request, "ADMIN"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator is not approved");
    }

    @Test
    @DisplayName("Should reject assignment when operator mobile number is unverified")
    void testAssignOperatorMobileUnverified() {
        approvedOperator.setMobileVerified(false);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(100L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> assignmentService.assignOperator(100L, request, "ADMIN"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator mobile number is not verified");
    }

    @Test
    @DisplayName("Should reject assignment when operator has an active overlapping assignment")
    void testAssignOperatorScheduleConflict() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(confirmedBooking));
        when(assignmentRepository.existsByBookingIdAndAssignmentStatus(100L, OperatorAssignmentStatus.ASSIGNED)).thenReturn(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));
        when(assignmentRepository.existsOverlappingAssignment(
                eq(1L), eq(OperatorAssignmentStatus.ASSIGNED), eq(confirmedBooking.getStartDate()), eq(confirmedBooking.getEndDate()))
        ).thenReturn(true);

        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .build();

        assertThatThrownBy(() -> assignmentService.assignOperator(100L, request, "ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Operator already has an active assignment for overlapping dates");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve paginated assigned jobs for authenticated operator")
    void testGetAssignedJobsSuccess() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));

        OperatorJobAssignment assignment = OperatorJobAssignment.builder()
                .operator(approvedOperator)
                .booking(confirmedBooking)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();
        assignment.setId(500L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<OperatorJobAssignment> page = new PageImpl<>(List.of(assignment), pageable, 1);

        when(assignmentRepository.findByOperatorId(1L, pageable)).thenReturn(page);

        Page<OperatorAssignedJobResponse> result = assignmentService.getAssignedJobs(1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAssignmentId()).isEqualTo(500L);
        assertThat(result.getContent().get(0).getBookingId()).isEqualTo(100L);
        assertThat(result.getContent().get(0).getEquipmentName()).isEqualTo("Mahindra 575 DI Tractor");
    }

    @Test
    @DisplayName("Should retrieve single assigned job detail for authenticated operator")
    void testGetAssignedJobDetailSuccess() {
        OperatorJobAssignment assignment = OperatorJobAssignment.builder()
                .operator(approvedOperator)
                .booking(confirmedBooking)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();
        assignment.setId(500L);

        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(assignment));

        OperatorAssignedJobResponse response = assignmentService.getAssignedJob(1L, 500L);

        assertThat(response).isNotNull();
        assertThat(response.getAssignmentId()).isEqualTo(500L);
        assertThat(response.getBookingId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should block IDOR when operator attempts to access another operator's assignment")
    void testGetAssignedJobIdorBlocked() {
        Operator otherOperator = Operator.builder()
                .fullName("Other Operator")
                .mobileNumber("9999999999")
                .build();
        otherOperator.setId(2L);

        OperatorJobAssignment assignment = OperatorJobAssignment.builder()
                .operator(otherOperator)
                .booking(confirmedBooking)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();
        assignment.setId(500L);

        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> assignmentService.getAssignedJob(1L, 500L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied: You do not have permission to view this assignment");
    }

    @Test
    @DisplayName("Should find and return eligible operators with search filtering")
    void testFindEligibleOperators() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Operator> page = new PageImpl<>(List.of(approvedOperator), pageable, 1);

        when(operatorRepository.searchEligibleOperators(OperatorStatus.APPROVED, true, true, "tractor", pageable))
                .thenReturn(page);

        Page<EligibleOperatorResponse> result = assignmentService.findEligibleOperators("tractor", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getOperatorId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Rajesh Shinde");
    }
}
