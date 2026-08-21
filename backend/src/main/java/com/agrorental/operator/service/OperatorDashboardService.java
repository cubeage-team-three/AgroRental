package com.agrorental.operator.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.entity.EquipmentImage;
import com.agrorental.operator.dto.OperatorDashboardActiveJobResponse;
import com.agrorental.operator.dto.OperatorDashboardMetricsResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Service calculating real-time dashboard KPIs, status breakdowns, and active work assignments for an authenticated Operator.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorDashboardService {

    private final OperatorRepository operatorRepository;
    private final OperatorJobAssignmentRepository assignmentRepository;

    /**
     * Aggregates and returns dashboard metrics for the authenticated operator.
     *
     * @param operatorId Unique ID of the authenticated Operator
     * @return OperatorDashboardMetricsResponse with status counts, rates, and active job
     */
    @Transactional(readOnly = true)
    public OperatorDashboardMetricsResponse getDashboardMetrics(Long operatorId) {
        log.info("Calculating dashboard metrics for operator ID: {}", operatorId);

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operatorId));

        if (!operator.isActive()) {
            log.warn("Dashboard metrics access rejected: Operator {} is inactive", operatorId);
            throw new ForbiddenException("Operator account is inactive");
        }

        if (operator.getStatus() != OperatorStatus.APPROVED) {
            log.warn("Dashboard metrics access rejected: Operator {} status is {}", operatorId, operator.getStatus());
            throw new ForbiddenException("Operator is not approved");
        }

        // Aggregate counts by status
        List<Object[]> statusCounts = assignmentRepository.countGroupedByStatusForOperator(operatorId);
        Map<OperatorAssignmentStatus, Long> countsMap = new EnumMap<>(OperatorAssignmentStatus.class);
        for (Object[] row : statusCounts) {
            OperatorAssignmentStatus status = (OperatorAssignmentStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            countsMap.put(status, count);
        }

        long assignedJobs = countsMap.getOrDefault(OperatorAssignmentStatus.ASSIGNED, 0L);
        long acceptedJobs = countsMap.getOrDefault(OperatorAssignmentStatus.ACCEPTED, 0L);
        long travelingJobs = countsMap.getOrDefault(OperatorAssignmentStatus.TRAVELING, 0L);
        long reachedJobs = countsMap.getOrDefault(OperatorAssignmentStatus.REACHED, 0L);
        long inProgressJobs = countsMap.getOrDefault(OperatorAssignmentStatus.IN_PROGRESS, 0L);
        long pausedJobs = countsMap.getOrDefault(OperatorAssignmentStatus.PAUSED, 0L);
        long completedJobs = countsMap.getOrDefault(OperatorAssignmentStatus.COMPLETED, 0L);
        long rejectedJobs = countsMap.getOrDefault(OperatorAssignmentStatus.REJECTED, 0L);
        long cancelledJobs = countsMap.getOrDefault(OperatorAssignmentStatus.CANCELLED, 0L);

        long totalJobs = assignedJobs + acceptedJobs + travelingJobs + reachedJobs
                + inProgressJobs + pausedJobs + completedJobs + rejectedJobs + cancelledJobs;

        // Schedule counts
        LocalDate today = LocalDate.now();
        Long todayJobs = assignmentRepository.countTodayJobsForOperator(operatorId, today);
        Long upcomingJobs = assignmentRepository.countUpcomingJobsForOperator(operatorId, today);

        // Performance rates calculation
        double completionRate = 0.0;
        if (totalJobs > 0) {
            completionRate = Math.round(((double) completedJobs / (double) totalJobs * 100.0) * 10.0) / 10.0;
        }

        double acceptanceRate = 0.0;
        long acceptedSum = acceptedJobs + travelingJobs + reachedJobs + inProgressJobs + pausedJobs + completedJobs;
        long decisionTotal = acceptedSum + rejectedJobs;
        if (decisionTotal > 0) {
            acceptanceRate = Math.round(((double) acceptedSum / (double) decisionTotal * 100.0) * 10.0) / 10.0;
        }

        // Active job determination
        List<OperatorJobAssignment> activeList = assignmentRepository.findActiveAssignmentsForOperator(operatorId);
        OperatorDashboardActiveJobResponse activeJob = activeList.isEmpty() ? null : mapToActiveJob(activeList.get(0));

        return OperatorDashboardMetricsResponse.builder()
                .totalJobs(totalJobs)
                .assignedJobs(assignedJobs)
                .acceptedJobs(acceptedJobs)
                .travelingJobs(travelingJobs)
                .reachedJobs(reachedJobs)
                .inProgressJobs(inProgressJobs)
                .pausedJobs(pausedJobs)
                .completedJobs(completedJobs)
                .rejectedJobs(rejectedJobs)
                .cancelledJobs(cancelledJobs)
                .todayJobs(todayJobs != null ? todayJobs : 0L)
                .upcomingJobs(upcomingJobs != null ? upcomingJobs : 0L)
                .completionRate(completionRate)
                .acceptanceRate(acceptanceRate)
                .activeJob(activeJob)
                .build();
    }

    private OperatorDashboardActiveJobResponse mapToActiveJob(OperatorJobAssignment assignment) {
        if (assignment == null) {
            return null;
        }

        Booking booking = assignment.getBooking();
        Equipment equipment = booking != null ? booking.getEquipment() : null;

        String primaryImageUrl = null;
        if (equipment != null && equipment.getImages() != null && !equipment.getImages().isEmpty()) {
            primaryImageUrl = equipment.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(EquipmentImage::getImageUrl)
                    .findFirst()
                    .orElse(equipment.getImages().get(0).getImageUrl());
        }

        return OperatorDashboardActiveJobResponse.builder()
                .assignmentId(assignment.getId())
                .bookingId(booking != null ? booking.getId() : null)
                .status(assignment.getAssignmentStatus())
                .startDate(booking != null ? booking.getStartDate() : null)
                .endDate(booking != null ? booking.getEndDate() : null)
                .deliveryAddress(booking != null ? booking.getDeliveryAddress() : null)
                .equipmentId(equipment != null ? equipment.getId() : null)
                .equipmentName(equipment != null ? equipment.getName() : null)
                .equipmentCategory(equipment != null && equipment.getCategory() != null ? equipment.getCategory().name() : null)
                .primaryImageUrl(primaryImageUrl)
                .farmerId(booking != null ? booking.getFarmerId() : null)
                .totalCost(booking != null ? booking.getTotalCost() : null)
                .build();
    }
}
