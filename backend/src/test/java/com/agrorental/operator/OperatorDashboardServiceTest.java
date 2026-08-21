package com.agrorental.operator;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.operator.dto.OperatorDashboardMetricsResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorDashboardService Metrics & KPI Unit Tests")
class OperatorDashboardServiceTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorJobAssignmentRepository assignmentRepository;

    @InjectMocks
    private OperatorDashboardService dashboardService;

    private Operator testOperator;
    private OperatorJobAssignment activeAssignment;

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

        Booking booking = Booking.builder()
                .farmerId(20L)
                .equipment(equipment)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .totalCost(BigDecimal.valueOf(3000))
                .status(BookingStatus.CONFIRMED)
                .deliveryAddress("Plot 10, Pune Farm")
                .build();
        booking.setId(100L);

        activeAssignment = OperatorJobAssignment.builder()
                .operator(testOperator)
                .booking(booking)
                .assignmentStatus(OperatorAssignmentStatus.IN_PROGRESS)
                .assignedAt(LocalDateTime.now().minusHours(4))
                .workStartedAt(LocalDateTime.now().minusHours(1))
                .build();
        activeAssignment.setId(500L);
    }

    @Test
    @DisplayName("Should successfully aggregate dashboard metrics with active job and computed rates")
    void testDashboardMetricsSuccess() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        List<Object[]> statusCounts = new ArrayList<>();
        statusCounts.add(new Object[]{OperatorAssignmentStatus.ASSIGNED, 2L});
        statusCounts.add(new Object[]{OperatorAssignmentStatus.ACCEPTED, 1L});
        statusCounts.add(new Object[]{OperatorAssignmentStatus.TRAVELING, 1L});
        statusCounts.add(new Object[]{OperatorAssignmentStatus.REACHED, 1L});
        statusCounts.add(new Object[]{OperatorAssignmentStatus.IN_PROGRESS, 1L});
        statusCounts.add(new Object[]{OperatorAssignmentStatus.PAUSED, 1L});
        statusCounts.add(new Object[]{OperatorAssignmentStatus.COMPLETED, 8L});
        statusCounts.add(new Object[]{OperatorAssignmentStatus.REJECTED, 2L});
        statusCounts.add(new Object[]{OperatorAssignmentStatus.CANCELLED, 1L});

        when(assignmentRepository.countGroupedByStatusForOperator(1L)).thenReturn(statusCounts);
        when(assignmentRepository.countTodayJobsForOperator(eq(1L), any(LocalDate.class))).thenReturn(3L);
        when(assignmentRepository.countUpcomingJobsForOperator(eq(1L), any(LocalDate.class))).thenReturn(2L);
        when(assignmentRepository.findActiveAssignmentsForOperator(1L)).thenReturn(List.of(activeAssignment));

        OperatorDashboardMetricsResponse response = dashboardService.getDashboardMetrics(1L);

        assertThat(response).isNotNull();
        assertThat(response.getTotalJobs()).isEqualTo(18L);
        assertThat(response.getAssignedJobs()).isEqualTo(2L);
        assertThat(response.getAcceptedJobs()).isEqualTo(1L);
        assertThat(response.getTravelingJobs()).isEqualTo(1L);
        assertThat(response.getReachedJobs()).isEqualTo(1L);
        assertThat(response.getInProgressJobs()).isEqualTo(1L);
        assertThat(response.getPausedJobs()).isEqualTo(1L);
        assertThat(response.getCompletedJobs()).isEqualTo(8L);
        assertThat(response.getRejectedJobs()).isEqualTo(2L);
        assertThat(response.getCancelledJobs()).isEqualTo(1L);
        assertThat(response.getTodayJobs()).isEqualTo(3L);
        assertThat(response.getUpcomingJobs()).isEqualTo(2L);

        // Completion Rate: 8 / 18 * 100 = 44.4%
        assertThat(response.getCompletionRate()).isEqualTo(44.4);

        // Acceptance Rate: Accepted sum = 1+1+1+1+1+8 = 13. Decision total = 13 + 2 = 15. 13 / 15 * 100 = 86.7%
        assertThat(response.getAcceptanceRate()).isEqualTo(86.7);

        assertThat(response.getActiveJob()).isNotNull();
        assertThat(response.getActiveJob().getAssignmentId()).isEqualTo(500L);
        assertThat(response.getActiveJob().getStatus()).isEqualTo(OperatorAssignmentStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should return valid zero metrics for an operator with no assignments")
    void testDashboardMetricsNoJobs() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.countGroupedByStatusForOperator(1L)).thenReturn(Collections.emptyList());
        when(assignmentRepository.countTodayJobsForOperator(eq(1L), any(LocalDate.class))).thenReturn(0L);
        when(assignmentRepository.countUpcomingJobsForOperator(eq(1L), any(LocalDate.class))).thenReturn(0L);
        when(assignmentRepository.findActiveAssignmentsForOperator(1L)).thenReturn(Collections.emptyList());

        OperatorDashboardMetricsResponse response = dashboardService.getDashboardMetrics(1L);

        assertThat(response).isNotNull();
        assertThat(response.getTotalJobs()).isEqualTo(0L);
        assertThat(response.getAssignedJobs()).isEqualTo(0L);
        assertThat(response.getCompletedJobs()).isEqualTo(0L);
        assertThat(response.getCompletionRate()).isEqualTo(0.0);
        assertThat(response.getAcceptanceRate()).isEqualTo(0.0);
        assertThat(response.getActiveJob()).isNull();
    }

    @Test
    @DisplayName("Should correctly handle activeJob null when no active jobs exist")
    void testNoActiveJob() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(assignmentRepository.countGroupedByStatusForOperator(1L)).thenReturn(Collections.emptyList());
        when(assignmentRepository.findActiveAssignmentsForOperator(1L)).thenReturn(Collections.emptyList());

        OperatorDashboardMetricsResponse response = dashboardService.getDashboardMetrics(1L);

        assertThat(response.getActiveJob()).isNull();
    }

    @Test
    @DisplayName("Should reject dashboard retrieval when operator is inactive")
    void testInactiveOperatorRejected() {
        testOperator.setActive(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        assertThatThrownBy(() -> dashboardService.getDashboardMetrics(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator account is inactive");
    }

    @Test
    @DisplayName("Should reject dashboard retrieval when operator is not approved")
    void testUnapprovedOperatorRejected() {
        testOperator.setStatus(OperatorStatus.PENDING);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        assertThatThrownBy(() -> dashboardService.getDashboardMetrics(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator is not approved");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when operator does not exist")
    void testOperatorNotFound() {
        when(operatorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getDashboardMetrics(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Operator not found with ID: 999");
    }
}
