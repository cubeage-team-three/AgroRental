package com.agrorental.operator;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.dto.OperatorLocationResponse;
import com.agrorental.operator.dto.OperatorLocationUpdateRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorLocation;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorLocationRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorLocationService Unit Tests")
class OperatorLocationServiceTest {

    @Mock
    private OperatorLocationRepository locationRepository;

    @Mock
    private OperatorJobAssignmentRepository assignmentRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @InjectMocks
    private OperatorLocationService locationService;

    private Operator operator;
    private Operator otherOperator;
    private OperatorJobAssignment assignment;
    private OperatorLocation location;

    @BeforeEach
    void setUp() {
        operator = Operator.builder()
                .fullName("Ramesh Shinde")
                .mobileNumber("9876543210")
                .email("ramesh@agrorental.com")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        operator.setId(1L);
        operator.setActive(true);

        otherOperator = Operator.builder()
                .fullName("Suresh Patil")
                .mobileNumber("9876543211")
                .email("suresh@agrorental.com")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        otherOperator.setId(2L);
        otherOperator.setActive(true);

        assignment = OperatorJobAssignment.builder()
                .operator(operator)
                .assignmentStatus(OperatorAssignmentStatus.IN_PROGRESS)
                .assignedAt(LocalDateTime.now())
                .build();
        assignment.setId(100L);

        location = OperatorLocation.builder()
                .assignment(assignment)
                .operator(operator)
                .latitude(18.5204)
                .longitude(73.8567)
                .accuracy(10.0)
                .speed(12.5)
                .heading(90.0)
                .trackingActive(true)
                .recordedAt(LocalDateTime.now())
                .build();
        location.setId(500L);
    }

    @Test
    @DisplayName("startTracking successfully activates tracking for active assignment")
    void startTracking_success() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(locationRepository.findTopByAssignmentIdOrderByRecordedAtDesc(100L)).thenReturn(Optional.empty());
        when(locationRepository.save(any(OperatorLocation.class))).thenReturn(location);

        OperatorLocationResponse response = locationService.startTracking(100L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.isTrackingActive()).isTrue();
        verify(locationRepository).deactivateTrackingForAssignment(100L);
        verify(locationRepository).save(any(OperatorLocation.class));
    }

    @Test
    @DisplayName("updateLocation successfully saves GPS point")
    void updateLocation_success() {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .accuracy(5.0)
                .speed(15.0)
                .heading(180.0)
                .build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(locationRepository.save(any(OperatorLocation.class))).thenReturn(location);

        OperatorLocationResponse response = locationService.updateLocation(100L, 1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getLatitude()).isEqualTo(18.5204);
        assertThat(response.getLongitude()).isEqualTo(73.8567);
        verify(locationRepository).save(any(OperatorLocation.class));
    }

    @Test
    @DisplayName("getLatestLocation successfully returns latest GPS record")
    void getLatestLocation_success() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(locationRepository.findTopByAssignmentIdOrderByRecordedAtDesc(100L)).thenReturn(Optional.of(location));

        OperatorLocationResponse response = locationService.getLatestLocation(100L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getLatitude()).isEqualTo(18.5204);
        assertThat(response.getLongitude()).isEqualTo(73.8567);
        assertThat(response.isTrackingActive()).isTrue();
    }

    @Test
    @DisplayName("stopTracking marks tracking inactive")
    void stopTracking_success() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(locationRepository.findTopByAssignmentIdOrderByRecordedAtDesc(100L)).thenReturn(Optional.of(location));
        when(locationRepository.save(any(OperatorLocation.class))).thenReturn(location);

        OperatorLocationResponse response = locationService.stopTracking(100L, 1L);

        assertThat(response).isNotNull();
        verify(locationRepository).deactivateTrackingForAssignment(100L);
    }

    @Test
    @DisplayName("updateLocation with invalid latitude > 90 throws BadRequestException")
    void updateLocation_invalidLatitude_tooHigh() {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(91.0)
                .longitude(73.8567)
                .build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> locationService.updateLocation(100L, 1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Latitude must be between -90.0 and 90.0");
    }

    @Test
    @DisplayName("updateLocation with invalid latitude < -90 throws BadRequestException")
    void updateLocation_invalidLatitude_tooLow() {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(-91.0)
                .longitude(73.8567)
                .build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> locationService.updateLocation(100L, 1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Latitude must be between -90.0 and 90.0");
    }

    @Test
    @DisplayName("updateLocation with invalid longitude > 180 throws BadRequestException")
    void updateLocation_invalidLongitude_tooHigh() {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(18.5204)
                .longitude(181.0)
                .build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> locationService.updateLocation(100L, 1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Longitude must be between -180.0 and 180.0");
    }

    @Test
    @DisplayName("updateLocation with invalid longitude < -180 throws BadRequestException")
    void updateLocation_invalidLongitude_tooLow() {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(18.5204)
                .longitude(-181.0)
                .build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> locationService.updateLocation(100L, 1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Longitude must be between -180.0 and 180.0");
    }

    @Test
    @DisplayName("updateLocation with negative accuracy throws BadRequestException")
    void updateLocation_invalidAccuracy_negative() {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .accuracy(-5.0)
                .build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> locationService.updateLocation(100L, 1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Accuracy must be >= 0.0");
    }

    @Test
    @DisplayName("updateLocation with negative speed throws BadRequestException")
    void updateLocation_invalidSpeed_negative() {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .speed(-1.0)
                .build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> locationService.updateLocation(100L, 1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Speed must be >= 0.0");
    }

    @Test
    @DisplayName("updateLocation with heading >= 360 throws BadRequestException")
    void updateLocation_invalidHeading_tooHigh() {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .heading(360.0)
                .build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> locationService.updateLocation(100L, 1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Heading must be >= 0.0 and < 360.0");
    }

    @Test
    @DisplayName("updateLocation by different operator throws ForbiddenException (IDOR)")
    void updateLocation_wrongOperator_throwsForbiddenException() {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        when(operatorRepository.findById(2L)).thenReturn(Optional.of(otherOperator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> locationService.updateLocation(100L, 2L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied: You are not assigned to this job");
    }

    @Test
    @DisplayName("updateLocation for inactive operator throws ForbiddenException")
    void updateLocation_inactiveOperator_throwsForbiddenException() {
        operator.setActive(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));

        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        assertThatThrownBy(() -> locationService.updateLocation(100L, 1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator account is inactive");
    }

    @Test
    @DisplayName("updateLocation for unapproved operator throws ForbiddenException")
    void updateLocation_unapprovedOperator_throwsForbiddenException() {
        operator.setStatus(OperatorStatus.PENDING);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));

        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        assertThatThrownBy(() -> locationService.updateLocation(100L, 1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator account is not approved");
    }

    @Test
    @DisplayName("startTracking for COMPLETED assignment throws BadRequestException")
    void startTracking_terminalStatusCompleted_throwsBadRequestException() {
        assignment.setAssignmentStatus(OperatorAssignmentStatus.COMPLETED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> locationService.startTracking(100L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Location tracking cannot be started for assignment in status: COMPLETED");
    }

    @Test
    @DisplayName("startTracking for REJECTED assignment throws BadRequestException")
    void startTracking_terminalStatusRejected_throwsBadRequestException() {
        assignment.setAssignmentStatus(OperatorAssignmentStatus.REJECTED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> locationService.startTracking(100L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Location tracking cannot be started for assignment in status: REJECTED");
    }

    @Test
    @DisplayName("startTracking for ASSIGNED (unaccepted) assignment throws BadRequestException")
    void startTracking_initialStatusAssigned_throwsBadRequestException() {
        assignment.setAssignmentStatus(OperatorAssignmentStatus.ASSIGNED);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> locationService.startTracking(100L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Location tracking cannot be started for assignment in status: ASSIGNED");
    }
}
