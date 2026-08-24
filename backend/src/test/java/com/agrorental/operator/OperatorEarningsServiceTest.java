package com.agrorental.operator;

import com.agrorental.booking.entity.Booking;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.operator.dto.OperatorEarningsHistoryResponse;
import com.agrorental.operator.dto.OperatorEarningsSummaryResponse;
import com.agrorental.operator.dto.OperatorJobEarningsResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorJobPauseInterval;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorJobPauseIntervalRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorEarningsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorEarningsService Business Logic & Calculation Unit Tests")
class OperatorEarningsServiceTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorJobAssignmentRepository assignmentRepository;

    @Mock
    private OperatorJobPauseIntervalRepository pauseIntervalRepository;

    @InjectMocks
    private OperatorEarningsService earningsService;

    private Operator operator;
    private Operator otherOperator;
    private OperatorJobAssignment completedAssignment;
    private Booking booking;

    @BeforeEach
    void setUp() {
        operator = Operator.builder()
                .fullName("Ramesh Shinde")
                .mobileNumber("9876543210")
                .email("ramesh@agrorental.com")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .hourlyRate(new BigDecimal("500.00"))
                .build();
        operator.setId(1L);
        operator.setActive(true);

        otherOperator = Operator.builder()
                .fullName("Suresh Patil")
                .mobileNumber("9876543211")
                .email("suresh@agrorental.com")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .hourlyRate(new BigDecimal("600.00"))
                .build();
        otherOperator.setId(2L);
        otherOperator.setActive(true);

        Equipment equipment = Equipment.builder()
                .name("John Deere 5310 4WD Tractor")
                .category(EquipmentCategory.TRACTOR)
                .build();
        equipment.setId(10L);

        booking = Booking.builder()
                .equipment(equipment)
                .deliveryAddress("Shirur Farm Yard, Pune")
                .build();
        booking.setId(200L);

        completedAssignment = OperatorJobAssignment.builder()
                .operator(operator)
                .booking(booking)
                .assignmentStatus(OperatorAssignmentStatus.COMPLETED)
                .assignedAt(LocalDateTime.of(2026, 8, 20, 8, 0))
                .acceptedAt(LocalDateTime.of(2026, 8, 20, 8, 30))
                .travelingAt(LocalDateTime.of(2026, 8, 20, 9, 0))
                .reachedAt(LocalDateTime.of(2026, 8, 20, 9, 30))
                .workStartedAt(LocalDateTime.of(2026, 8, 20, 10, 0))
                .completedAt(LocalDateTime.of(2026, 8, 20, 16, 0)) // 6 hours total
                .build();
        completedAssignment.setId(100L);
    }

    @Test
    @DisplayName("1. Calculates earnings for completed job with no pauses")
    void calculateEarnings_completedJob_noPauses() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(completedAssignment));
        when(pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(100L)).thenReturn(Collections.emptyList());

        OperatorJobEarningsResponse response = earningsService.getJobEarnings(100L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getAssignmentId()).isEqualTo(100L);
        assertThat(response.getHourlyRate()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.getTotalElapsedMinutes()).isEqualTo(360L); // 6 hours
        assertThat(response.getPausedMinutes()).isEqualTo(0L);
        assertThat(response.getNetWorkMinutes()).isEqualTo(360L);
        assertThat(response.getNetWorkHours()).isEqualTo(6.0);
        // 6 hrs * ₹500 = ₹3000.00
        assertThat(response.getGrossEarnings()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(response.getIsFinalized()).isTrue();
        assertThat(response.getIsEstimated()).isFalse();
        assertThat(response.getCurrency()).isEqualTo("INR");
    }

    @Test
    @DisplayName("2. Calculates earnings for completed job with single pause cycle")
    void calculateEarnings_completedJob_singlePause() {
        OperatorJobPauseInterval pause1 = OperatorJobPauseInterval.builder()
                .assignment(completedAssignment)
                .operator(operator)
                .pausedAt(LocalDateTime.of(2026, 8, 20, 12, 0))
                .resumedAt(LocalDateTime.of(2026, 8, 20, 13, 0))
                .durationMinutes(60L) // 1 hour pause
                .build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(completedAssignment));
        when(pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(100L)).thenReturn(List.of(pause1));

        OperatorJobEarningsResponse response = earningsService.getJobEarnings(100L, 1L);

        assertThat(response.getTotalElapsedMinutes()).isEqualTo(360L);
        assertThat(response.getPausedMinutes()).isEqualTo(60L);
        assertThat(response.getNetWorkMinutes()).isEqualTo(300L); // 5 hours net
        assertThat(response.getNetWorkHours()).isEqualTo(5.0);
        // 5 hrs * ₹500 = ₹2500.00
        assertThat(response.getGrossEarnings()).isEqualByComparingTo(new BigDecimal("2500.00"));
    }

    @Test
    @DisplayName("3. Calculates earnings for completed job with multiple pause/resume cycles")
    void calculateEarnings_completedJob_multiplePauses() {
        OperatorJobPauseInterval pause1 = OperatorJobPauseInterval.builder()
                .assignment(completedAssignment)
                .operator(operator)
                .pausedAt(LocalDateTime.of(2026, 8, 20, 11, 30))
                .resumedAt(LocalDateTime.of(2026, 8, 20, 12, 0))
                .durationMinutes(30L)
                .build();

        OperatorJobPauseInterval pause2 = OperatorJobPauseInterval.builder()
                .assignment(completedAssignment)
                .operator(operator)
                .pausedAt(LocalDateTime.of(2026, 8, 20, 14, 0))
                .resumedAt(LocalDateTime.of(2026, 8, 20, 14, 45))
                .durationMinutes(45L)
                .build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(completedAssignment));
        when(pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(100L)).thenReturn(List.of(pause1, pause2));

        OperatorJobEarningsResponse response = earningsService.getJobEarnings(100L, 1L);

        assertThat(response.getTotalElapsedMinutes()).isEqualTo(360L);
        assertThat(response.getPausedMinutes()).isEqualTo(75L); // 30 + 45 = 75 mins
        assertThat(response.getNetWorkMinutes()).isEqualTo(285L); // 360 - 75 = 285 mins (4.75 hours)
        assertThat(response.getNetWorkHours()).isEqualTo(4.75);
        // 285 / 60 * 500 = 4.75 * 500 = ₹2375.00
        assertThat(response.getGrossEarnings()).isEqualByComparingTo(new BigDecimal("2375.00"));
    }

    @Test
    @DisplayName("4. Calculates earnings with custom operator hourly rate and rounding mode")
    void calculateEarnings_customRate_halfUpRounding() {
        // Set hourly rate to 450.55
        operator.setHourlyRate(new BigDecimal("450.55"));

        // Net work = 125 minutes (2.0833 hours)
        OperatorJobAssignment shortJob = OperatorJobAssignment.builder()
                .operator(operator)
                .booking(booking)
                .assignmentStatus(OperatorAssignmentStatus.COMPLETED)
                .workStartedAt(LocalDateTime.of(2026, 8, 20, 10, 0))
                .completedAt(LocalDateTime.of(2026, 8, 20, 12, 5)) // 125 mins
                .build();
        shortJob.setId(101L);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(101L)).thenReturn(Optional.of(shortJob));
        when(pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(101L)).thenReturn(Collections.emptyList());

        OperatorJobEarningsResponse response = earningsService.getJobEarnings(101L, 1L);

        assertThat(response.getNetWorkMinutes()).isEqualTo(125L);
        // 450.55 * 125 / 60 = 938.645833... -> 938.65
        assertThat(response.getGrossEarnings()).isEqualByComparingTo(new BigDecimal("938.65"));
        assertThat(response.getGrossEarnings().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("5. Falls back to default hourly rate (500.00) when operator hourlyRate is null or 0")
    void calculateEarnings_nullRate_usesDefault() {
        operator.setHourlyRate(null);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(completedAssignment));
        when(pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(100L)).thenReturn(Collections.emptyList());

        OperatorJobEarningsResponse response = earningsService.getJobEarnings(100L, 1L);

        assertThat(response.getHourlyRate()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.getGrossEarnings()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("6. Safe fallback for legacy assignments without interval records using pausedAt/resumedAt")
    void calculateEarnings_legacyPauseFallback() {
        completedAssignment.setPausedAt(LocalDateTime.of(2026, 8, 20, 12, 0));
        completedAssignment.setResumedAt(LocalDateTime.of(2026, 8, 20, 13, 30)); // 90 min pause

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(completedAssignment));
        when(pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(100L)).thenReturn(Collections.emptyList());

        OperatorJobEarningsResponse response = earningsService.getJobEarnings(100L, 1L);

        assertThat(response.getTotalElapsedMinutes()).isEqualTo(360L);
        assertThat(response.getPausedMinutes()).isEqualTo(90L);
        assertThat(response.getNetWorkMinutes()).isEqualTo(270L); // 4.5 hours
        assertThat(response.getGrossEarnings()).isEqualByComparingTo(new BigDecimal("2250.00"));
    }

    @Test
    @DisplayName("7. Handles missing workStartedAt safely returning 0 duration and 0 earnings")
    void calculateEarnings_missingWorkStartedAt_returnsZero() {
        completedAssignment.setWorkStartedAt(null);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(completedAssignment));

        OperatorJobEarningsResponse response = earningsService.getJobEarnings(100L, 1L);

        assertThat(response.getTotalElapsedMinutes()).isEqualTo(0L);
        assertThat(response.getNetWorkMinutes()).isEqualTo(0L);
        assertThat(response.getNetWorkHours()).isEqualTo(0.0);
        assertThat(response.getGrossEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("8. Handles invalid timestamp ordering (completedAt before workStartedAt) safely")
    void calculateEarnings_invalidTimestampOrder_returnsZero() {
        completedAssignment.setWorkStartedAt(LocalDateTime.of(2026, 8, 20, 16, 0));
        completedAssignment.setCompletedAt(LocalDateTime.of(2026, 8, 20, 10, 0)); // invalid before start

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(completedAssignment));

        OperatorJobEarningsResponse response = earningsService.getJobEarnings(100L, 1L);

        assertThat(response.getTotalElapsedMinutes()).isEqualTo(0L);
        assertThat(response.getGrossEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("9. Active IN_PROGRESS job returns estimated earnings (isFinalized=false, isEstimated=true)")
    void calculateEarnings_inProgressJob_returnsEstimated() {
        OperatorJobAssignment activeJob = OperatorJobAssignment.builder()
                .operator(operator)
                .booking(booking)
                .assignmentStatus(OperatorAssignmentStatus.IN_PROGRESS)
                .workStartedAt(LocalDateTime.now().minusHours(2))
                .build();
        activeJob.setId(102L);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(102L)).thenReturn(Optional.of(activeJob));
        when(pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(102L)).thenReturn(Collections.emptyList());

        OperatorJobEarningsResponse response = earningsService.getJobEarnings(102L, 1L);

        assertThat(response.getIsFinalized()).isFalse();
        assertThat(response.getIsEstimated()).isTrue();
        assertThat(response.getNetWorkMinutes()).isGreaterThanOrEqualTo(118L);
        assertThat(response.getGrossEarnings()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("10. IDOR Defense: Cross-operator access to assignment earnings throws 403 Forbidden")
    void getJobEarnings_crossOperatorAccess_throws403() {
        when(operatorRepository.findById(2L)).thenReturn(Optional.of(otherOperator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(completedAssignment)); // belongs to op 1

        assertThatThrownBy(() -> earningsService.getJobEarnings(100L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied: You do not have permission to view earnings for this assignment");
    }

    @Test
    @DisplayName("11. Inactive operator access throws 403 Forbidden")
    void getJobEarnings_inactiveOperator_throws403() {
        operator.setActive(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));

        assertThatThrownBy(() -> earningsService.getJobEarnings(100L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator account is inactive");
    }

    @Test
    @DisplayName("12. Unapproved operator access throws 403 Forbidden")
    void getJobEarnings_unapprovedOperator_throws403() {
        operator.setStatus(OperatorStatus.PENDING);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));

        assertThatThrownBy(() -> earningsService.getJobEarnings(100L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator is not approved");
    }

    @Test
    @DisplayName("13. Non-existent assignment ID throws 404 ResourceNotFoundException")
    void getJobEarnings_notFound_throws404() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> earningsService.getJobEarnings(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Job assignment not found with ID: 999");
    }

    @Test
    @DisplayName("14. Aggregates earnings summary for operator across multiple completed jobs")
    void getEarningsSummary_aggregatesCorrectly() {
        OperatorJobAssignment job1 = OperatorJobAssignment.builder()
                .operator(operator)
                .booking(booking)
                .assignmentStatus(OperatorAssignmentStatus.COMPLETED)
                .workStartedAt(LocalDateTime.of(2026, 8, 18, 9, 0))
                .completedAt(LocalDateTime.of(2026, 8, 18, 13, 0)) // 4 hrs (240 mins) -> ₹2000
                .build();
        job1.setId(101L);

        OperatorJobAssignment job2 = OperatorJobAssignment.builder()
                .operator(operator)
                .booking(booking)
                .assignmentStatus(OperatorAssignmentStatus.COMPLETED)
                .workStartedAt(LocalDateTime.of(2026, 8, 19, 10, 0))
                .completedAt(LocalDateTime.of(2026, 8, 19, 16, 0)) // 6 hrs (360 mins)
                .pausedAt(LocalDateTime.of(2026, 8, 19, 12, 0))
                .resumedAt(LocalDateTime.of(2026, 8, 19, 13, 0)) // 1 hr pause -> 5 hrs net (300 mins) -> ₹2500
                .build();
        job2.setId(102L);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findByOperatorIdAndAssignmentStatus(1L, OperatorAssignmentStatus.COMPLETED))
                .thenReturn(List.of(job1, job2));
        when(pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(101L)).thenReturn(Collections.emptyList());
        when(pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(102L)).thenReturn(Collections.emptyList());

        OperatorEarningsSummaryResponse summary = earningsService.getEarningsSummary(1L);

        assertThat(summary.getTotalCompletedJobs()).isEqualTo(2L);
        assertThat(summary.getTotalWorkMinutes()).isEqualTo(540L); // 240 + 300 = 540 mins (9 hrs)
        assertThat(summary.getTotalWorkHours()).isEqualTo(9.0);
        assertThat(summary.getTotalPausedMinutes()).isEqualTo(60L);
        assertThat(summary.getTotalGrossEarnings()).isEqualByComparingTo(new BigDecimal("4500.00")); // 2000 + 2500
        assertThat(summary.getAverageEarningsPerJob()).isEqualByComparingTo(new BigDecimal("2250.00")); // 4500 / 2
        assertThat(summary.getHourlyRate()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(summary.getCurrency()).isEqualTo("INR");
    }

    @Test
    @DisplayName("15. Aggregates earnings summary when operator has zero completed jobs")
    void getEarningsSummary_zeroJobs_returnsZeroes() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findByOperatorIdAndAssignmentStatus(1L, OperatorAssignmentStatus.COMPLETED))
                .thenReturn(Collections.emptyList());

        OperatorEarningsSummaryResponse summary = earningsService.getEarningsSummary(1L);

        assertThat(summary.getTotalCompletedJobs()).isEqualTo(0L);
        assertThat(summary.getTotalWorkMinutes()).isEqualTo(0L);
        assertThat(summary.getTotalWorkHours()).isEqualTo(0.0);
        assertThat(summary.getTotalGrossEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getAverageEarningsPerJob()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("16. Retrieves paginated completed jobs earnings history with metadata")
    void getEarningsHistory_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OperatorJobAssignment> page = new PageImpl<>(List.of(completedAssignment), pageable, 1);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findByOperatorIdAndAssignmentStatus(1L, OperatorAssignmentStatus.COMPLETED, pageable))
                .thenReturn(page);
        when(pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(100L)).thenReturn(Collections.emptyList());

        Page<OperatorEarningsHistoryResponse> result = earningsService.getEarningsHistory(1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        OperatorEarningsHistoryResponse item = result.getContent().get(0);
        assertThat(item.getAssignmentId()).isEqualTo(100L);
        assertThat(item.getBookingId()).isEqualTo(200L);
        assertThat(item.getEquipmentName()).isEqualTo("John Deere 5310 4WD Tractor");
        assertThat(item.getEquipmentCategory()).isEqualTo("TRACTOR");
        assertThat(item.getDeliveryAddress()).isEqualTo("Shirur Farm Yard, Pune");
        assertThat(item.getNetWorkMinutes()).isEqualTo(360L);
        assertThat(item.getNetWorkHours()).isEqualTo(6.0);
        assertThat(item.getGrossEarnings()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(item.getCurrency()).isEqualTo("INR");
    }
}
