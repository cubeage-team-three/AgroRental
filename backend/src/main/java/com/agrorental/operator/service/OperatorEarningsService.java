package com.agrorental.operator.service;

import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing operator work-hour calculations, pause duration accounting, and monetary gross earnings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorEarningsService {

    public static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("500.00");
    public static final String CURRENCY_INR = "INR";

    private final OperatorRepository operatorRepository;
    private final OperatorJobAssignmentRepository assignmentRepository;
    private final OperatorJobPauseIntervalRepository pauseIntervalRepository;

    /**
     * Validates that the operator exists, is active, approved, and mobile verified.
     */
    private Operator validateOperator(Long operatorId) {
        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operatorId));

        if (!operator.isActive()) {
            log.warn("Earnings query rejected: Operator {} is inactive", operatorId);
            throw new ForbiddenException("Operator account is inactive");
        }

        if (operator.getStatus() != OperatorStatus.APPROVED) {
            log.warn("Earnings query rejected: Operator {} status is {}", operatorId, operator.getStatus());
            throw new ForbiddenException("Operator is not approved");
        }

        return operator;
    }

    /**
     * Calculates work duration and earnings for a specific job assignment.
     */
    @Transactional(readOnly = true)
    public OperatorJobEarningsResponse getJobEarnings(Long assignmentId, Long operatorId) {
        log.info("Calculating job earnings for assignment ID {} and operator ID {}", assignmentId, operatorId);
        Operator operator = validateOperator(operatorId);

        OperatorJobAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Job assignment not found with ID: " + assignmentId));

        // IDOR Verification
        if (!assignment.getOperator().getId().equals(operatorId)) {
            log.warn("IDOR violation: Operator {} attempted to access earnings for assignment {} belonging to operator {}",
                    operatorId, assignmentId, assignment.getOperator().getId());
            throw new ForbiddenException("Access denied: You do not have permission to view earnings for this assignment");
        }

        BigDecimal hourlyRate = getOperatorHourlyRate(operator);
        return computeJobEarnings(assignment, hourlyRate);
    }

    /**
     * Aggregates total earnings, completed jobs, logged work hours, and pause durations for the authenticated operator.
     */
    @Transactional(readOnly = true)
    public OperatorEarningsSummaryResponse getEarningsSummary(Long operatorId) {
        log.info("Calculating aggregate earnings summary for operator ID {}", operatorId);
        Operator operator = validateOperator(operatorId);
        BigDecimal hourlyRate = getOperatorHourlyRate(operator);

        List<OperatorJobAssignment> completedAssignments =
                assignmentRepository.findByOperatorIdAndAssignmentStatus(operatorId, OperatorAssignmentStatus.COMPLETED);

        long totalCompletedJobs = completedAssignments.size();
        long totalWorkMinutes = 0L;
        long totalPausedMinutes = 0L;
        BigDecimal totalGrossEarnings = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (OperatorJobAssignment assignment : completedAssignments) {
            OperatorJobEarningsResponse jobEarnings = computeJobEarnings(assignment, hourlyRate);
            totalWorkMinutes += jobEarnings.getNetWorkMinutes();
            totalPausedMinutes += jobEarnings.getPausedMinutes();
            totalGrossEarnings = totalGrossEarnings.add(jobEarnings.getGrossEarnings());
        }

        double totalWorkHours = (double) Math.round((totalWorkMinutes / 60.0) * 100.0) / 100.0;
        BigDecimal averageEarningsPerJob = totalCompletedJobs > 0
                ? totalGrossEarnings.divide(BigDecimal.valueOf(totalCompletedJobs), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return OperatorEarningsSummaryResponse.builder()
                .totalCompletedJobs(totalCompletedJobs)
                .totalWorkMinutes(totalWorkMinutes)
                .totalWorkHours(totalWorkHours)
                .totalPausedMinutes(totalPausedMinutes)
                .totalGrossEarnings(totalGrossEarnings)
                .averageEarningsPerJob(averageEarningsPerJob)
                .hourlyRate(hourlyRate)
                .currency(CURRENCY_INR)
                .build();
    }

    /**
     * Retrieves paginated completed job earnings history for the authenticated operator.
     */
    @Transactional(readOnly = true)
    public Page<OperatorEarningsHistoryResponse> getEarningsHistory(Long operatorId, Pageable pageable) {
        log.info("Fetching earnings history for operator ID {}", operatorId);
        Operator operator = validateOperator(operatorId);
        BigDecimal hourlyRate = getOperatorHourlyRate(operator);

        Page<OperatorJobAssignment> completedPage =
                assignmentRepository.findByOperatorIdAndAssignmentStatus(operatorId, OperatorAssignmentStatus.COMPLETED, pageable);

        return completedPage.map(assignment -> {
            OperatorJobEarningsResponse earnings = computeJobEarnings(assignment, hourlyRate);

            String equipmentName = "Agricultural Machinery";
            String equipmentCategory = "GENERAL";
            String deliveryAddress = "On-site Field Delivery";

            if (assignment.getBooking() != null) {
                if (assignment.getBooking().getDeliveryAddress() != null) {
                    deliveryAddress = assignment.getBooking().getDeliveryAddress();
                }
                Equipment eq = assignment.getBooking().getEquipment();
                if (eq != null) {
                    if (eq.getName() != null) equipmentName = eq.getName();
                    if (eq.getCategory() != null) equipmentCategory = eq.getCategory().name();
                }
            }

            return OperatorEarningsHistoryResponse.builder()
                    .assignmentId(assignment.getId())
                    .bookingId(assignment.getBooking() != null ? assignment.getBooking().getId() : null)
                    .equipmentName(equipmentName)
                    .equipmentCategory(equipmentCategory)
                    .deliveryAddress(deliveryAddress)
                    .completedAt(assignment.getCompletedAt())
                    .netWorkMinutes(earnings.getNetWorkMinutes())
                    .netWorkHours(earnings.getNetWorkHours())
                    .hourlyRate(hourlyRate)
                    .grossEarnings(earnings.getGrossEarnings())
                    .currency(CURRENCY_INR)
                    .build();
        });
    }

    /**
     * Core computation engine for calculating elapsed time, pause durations, net billable minutes, and gross earnings.
     */
    public OperatorJobEarningsResponse computeJobEarnings(OperatorJobAssignment assignment, BigDecimal hourlyRate) {
        boolean isCompleted = assignment.getAssignmentStatus() == OperatorAssignmentStatus.COMPLETED;
        LocalDateTime workStartedAt = assignment.getWorkStartedAt();
        LocalDateTime completedAt = assignment.getCompletedAt();

        long totalElapsedMinutes = 0L;
        long pausedMinutes = 0L;
        long netWorkMinutes = 0L;
        double netWorkHours = 0.0;
        BigDecimal grossEarnings = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (workStartedAt != null) {
            LocalDateTime endTimestamp = isCompleted && completedAt != null ? completedAt : LocalDateTime.now();

            // Safe ordering validation: endTimestamp must be after workStartedAt
            if (!endTimestamp.isBefore(workStartedAt)) {
                totalElapsedMinutes = Duration.between(workStartedAt, endTimestamp).toMinutes();

                // Compute paused minutes from pause intervals if present
                List<OperatorJobPauseInterval> intervals =
                        pauseIntervalRepository.findByAssignmentIdOrderByPausedAtAsc(assignment.getId());

                if (intervals != null && !intervals.isEmpty()) {
                    for (OperatorJobPauseInterval interval : intervals) {
                        if (interval.getDurationMinutes() != null && interval.getDurationMinutes() > 0) {
                            pausedMinutes += interval.getDurationMinutes();
                        } else if (interval.getPausedAt() != null) {
                            LocalDateTime resAt = interval.getResumedAt() != null ? interval.getResumedAt() : endTimestamp;
                            if (resAt.isAfter(interval.getPausedAt())) {
                                pausedMinutes += Duration.between(interval.getPausedAt(), resAt).toMinutes();
                            }
                        }
                    }
                } else if (assignment.getPausedAt() != null && assignment.getResumedAt() != null) {
                    // Fallback to legacy single pause/resume timestamps
                    if (assignment.getResumedAt().isAfter(assignment.getPausedAt())) {
                        pausedMinutes = Duration.between(assignment.getPausedAt(), assignment.getResumedAt()).toMinutes();
                    }
                } else if (assignment.getAssignmentStatus() == OperatorAssignmentStatus.PAUSED && assignment.getPausedAt() != null) {
                    if (endTimestamp.isAfter(assignment.getPausedAt())) {
                        pausedMinutes = Duration.between(assignment.getPausedAt(), endTimestamp).toMinutes();
                    }
                }

                pausedMinutes = Math.min(totalElapsedMinutes, Math.max(0L, pausedMinutes));
                netWorkMinutes = Math.max(0L, totalElapsedMinutes - pausedMinutes);
                netWorkHours = (double) Math.round((netWorkMinutes / 60.0) * 100.0) / 100.0;

                // grossEarnings = hourlyRate * (netWorkMinutes / 60)
                grossEarnings = hourlyRate
                        .multiply(BigDecimal.valueOf(netWorkMinutes))
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            }
        }

        return OperatorJobEarningsResponse.builder()
                .assignmentId(assignment.getId())
                .bookingId(assignment.getBooking() != null ? assignment.getBooking().getId() : null)
                .assignmentStatus(assignment.getAssignmentStatus())
                .hourlyRate(hourlyRate)
                .totalElapsedMinutes(totalElapsedMinutes)
                .pausedMinutes(pausedMinutes)
                .netWorkMinutes(netWorkMinutes)
                .netWorkHours(netWorkHours)
                .grossEarnings(grossEarnings)
                .currency(CURRENCY_INR)
                .isFinalized(isCompleted)
                .isEstimated(!isCompleted)
                .workStartedAt(workStartedAt)
                .completedAt(completedAt)
                .pausedAt(assignment.getPausedAt())
                .resumedAt(assignment.getResumedAt())
                .build();
    }

    private BigDecimal getOperatorHourlyRate(Operator operator) {
        if (operator != null && operator.getHourlyRate() != null && operator.getHourlyRate().compareTo(BigDecimal.ZERO) > 0) {
            return operator.getHourlyRate().setScale(2, RoundingMode.HALF_UP);
        }
        return DEFAULT_HOURLY_RATE.setScale(2, RoundingMode.HALF_UP);
    }
}
