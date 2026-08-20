package com.agrorental.operator.mapper;

import com.agrorental.booking.entity.Booking;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.entity.EquipmentImage;
import com.agrorental.operator.dto.EligibleOperatorResponse;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.dto.OperatorAssignmentResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting OperatorJobAssignment entities and Operator entities to safe Phase 4 and Phase 5 DTOs.
 */
@Component
public class OperatorJobAssignmentMapper {

    public OperatorAssignmentResponse toAssignmentResponse(OperatorJobAssignment assignment) {
        if (assignment == null) {
            return null;
        }

        Operator operator = assignment.getOperator();
        Booking booking = assignment.getBooking();
        Equipment equipment = booking != null ? booking.getEquipment() : null;

        return OperatorAssignmentResponse.builder()
                .assignmentId(assignment.getId())
                .bookingId(booking != null ? booking.getId() : null)
                .operatorId(operator != null ? operator.getId() : null)
                .operatorName(operator != null ? operator.getFullName() : null)
                .operatorMobile(operator != null ? operator.getMobileNumber() : null)
                .equipmentId(equipment != null ? equipment.getId() : null)
                .equipmentName(equipment != null ? equipment.getName() : null)
                .assignmentStatus(assignment.getAssignmentStatus())
                .assignedAt(assignment.getAssignedAt())
                .assignedBy(assignment.getAssignedBy())
                .notes(assignment.getNotes())
                .build();
    }

    public OperatorAssignedJobResponse toAssignedJobResponse(OperatorJobAssignment assignment) {
        if (assignment == null) {
            return null;
        }

        Operator operator = assignment.getOperator();
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

        return OperatorAssignedJobResponse.builder()
                .assignmentId(assignment.getId())
                .bookingId(booking != null ? booking.getId() : null)
                .operatorId(operator != null ? operator.getId() : null)
                .assignmentStatus(assignment.getAssignmentStatus())
                .assignedAt(assignment.getAssignedAt())
                .assignedBy(assignment.getAssignedBy())
                .notes(assignment.getNotes())
                // Phase 5 Lifecycle fields
                .acceptedAt(assignment.getAcceptedAt())
                .rejectedAt(assignment.getRejectedAt())
                .rejectionReason(assignment.getRejectionReason())
                .travelingAt(assignment.getTravelingAt())
                .reachedAt(assignment.getReachedAt())
                .workStartedAt(assignment.getWorkStartedAt())
                .pausedAt(assignment.getPausedAt())
                .pauseReason(assignment.getPauseReason())
                .resumedAt(assignment.getResumedAt())
                .completedAt(assignment.getCompletedAt())
                .completionNotes(assignment.getCompletionNotes())
                // Machinery details
                .equipmentId(equipment != null ? equipment.getId() : null)
                .equipmentName(equipment != null ? equipment.getName() : null)
                .equipmentCategory(equipment != null && equipment.getCategory() != null ? equipment.getCategory().name() : null)
                .primaryImageUrl(primaryImageUrl)
                // Service & Schedule details
                .farmerId(booking != null ? booking.getFarmerId() : null)
                .deliveryAddress(booking != null ? booking.getDeliveryAddress() : null)
                .startDate(booking != null ? booking.getStartDate() : null)
                .endDate(booking != null ? booking.getEndDate() : null)
                .totalCost(booking != null ? booking.getTotalCost() : null)
                .bookingStatus(booking != null ? booking.getStatus() : null)
                .build();
    }

    public EligibleOperatorResponse toEligibleResponse(Operator operator) {
        if (operator == null) {
            return null;
        }

        return EligibleOperatorResponse.builder()
                .operatorId(operator.getId())
                .fullName(operator.getFullName())
                .mobileNumber(operator.getMobileNumber())
                .skills(operator.getSkills())
                .experience(operator.getExperience())
                .address(operator.getAddress())
                .profilePhoto(operator.getProfilePhoto())
                .status(operator.getStatus())
                .mobileVerified(operator.isMobileVerified())
                .active(operator.isActive())
                .build();
    }
}
