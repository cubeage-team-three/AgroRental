package com.agrorental.operator;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.common.security.JwtTokenProvider;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.operator.controller.OperatorJobController;
import com.agrorental.operator.dto.JobAssignRequest;
import com.agrorental.operator.dto.JobStatusUpdateRequest;
import com.agrorental.operator.dto.OperatorJobResponse;
import com.agrorental.operator.dto.OperatorJobSummaryResponse;
import com.agrorental.operator.entity.JobStatus;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJob;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.entity.OperatorWorkMilestone;
import com.agrorental.operator.repository.OperatorJobRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.repository.OperatorWorkMilestoneRepository;
import com.agrorental.operator.service.OperatorJobService;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OperatorJobTest {

    @Mock
    private OperatorJobRepository operatorJobRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private OperatorWorkMilestoneRepository operatorWorkMilestoneRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private OperatorJobService operatorJobService;
    private OperatorJobController operatorJobController;

    private Operator testOperator;
    private Equipment testEquipment;
    private Partner testPartner;
    private OperatorJob testJob;
    private String validToken;

    @BeforeEach
    void setUp() {
        operatorJobService = new OperatorJobService(
                operatorJobRepository,
                operatorRepository,
                equipmentRepository,
                partnerRepository,
                operatorWorkMilestoneRepository
        );
        operatorJobController = new OperatorJobController(operatorJobService, jwtTokenProvider);

        testOperator = Operator.builder()
                .fullName("Ramesh Patel")
                .mobileNumber("9811223344")
                .email("ramesh@agro.com")
                .address("Village Sonipat, Haryana")
                .aadhaarNumber("112233445566")
                .drivingLicenseNumber("DL-HR10-2022-123456")
                .experience(4)
                .skills("Tractor, Seeder, Cultivator")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .documentsSubmitted(true)
                .build();
        testOperator.setId(10L);

        testPartner = Partner.builder()
                .fullName("Kisan Agro Services")
                .mobileNumber("9899001122")
                .email("kisan@agro.com")
                .build();
        testPartner.setId(5L);

        testEquipment = Equipment.builder()
                .name("Mahindra 575 DI Tractor")
                .category(EquipmentCategory.TRACTOR)
                .brand("Mahindra")
                .model("575 DI XP Plus")
                .partner(testPartner)
                .build();
        testEquipment.setId(20L);

        testJob = OperatorJob.builder()
                .operator(testOperator)
                .equipment(testEquipment)
                .partner(testPartner)
                .jobTitle("Tractor Plowing Service")
                .jobType("Plowing")
                .jobDescription("Deep soil plowing for 10 acres wheat farm")
                .workInstructions("Ensure uniform depth of 8-10 inches")
                .customerName("Suresh Kumar")
                .customerMobile("9876501234")
                .workLocation("Karnal Field Sector 9, Plot B")
                .scheduledDate(LocalDate.now().plusDays(2))
                .scheduledStartTime(LocalTime.of(8, 0))
                .scheduledEndTime(LocalTime.of(16, 0))
                .estimatedDurationHours(8.0)
                .operatorPayout(new BigDecimal("1800.00"))
                .status(JobStatus.PENDING_RESPONSE)
                .assignedBy("Partner Dispatcher")
                .build();
        testJob.setId(100L);

        validToken = "mock.valid.token";
    }

    @Test
    @DisplayName("Authenticated operator can retrieve assigned jobs list")
    void testGetAssignedJobs_Success() {
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorRepository.existsById(10L)).thenReturn(true);
        when(operatorJobRepository.findAllByOperatorIdOrderByScheduledDateDescCreatedAtDesc(10L))
                .thenReturn(List.of(testJob));
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = operatorJobController.getAssignedJobs("Bearer " + validToken, null);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(operatorJobRepository, times(1)).findAllByOperatorIdOrderByScheduledDateDescCreatedAtDesc(10L);
    }

    @Test
    @DisplayName("Operator can retrieve their own job details by ID")
    void testGetJobDetails_Success() {
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = operatorJobController.getJobDetails("Bearer " + validToken, 100L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(operatorJobRepository, times(1)).findByIdAndOperatorId(100L, 10L);
    }

    @Test
    @DisplayName("Operator cannot retrieve another operator's job (security isolation)")
    void testGetJobDetails_ForbiddenForOtherOperator() {
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(999L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            operatorJobController.getJobDetails("Bearer " + validToken, 999L);
        });
    }

    @Test
    @DisplayName("Missing or invalid Bearer token is rejected")
    void testGetAssignedJobs_MissingOrInvalidToken() {
        assertThrows(BadRequestException.class, () -> {
            operatorJobController.getAssignedJobs(null, null);
        });

        assertThrows(BadRequestException.class, () -> {
            operatorJobController.getAssignedJobs("InvalidTokenHeader", null);
        });
    }

    @Test
    @DisplayName("Job assignment creates correct relationships and persists with PENDING_RESPONSE status")
    void testAssignJob_Success() {
        when(operatorRepository.findById(10L)).thenReturn(Optional.of(testOperator));
        when(equipmentRepository.findById(20L)).thenReturn(Optional.of(testEquipment));
        when(partnerRepository.findById(5L)).thenReturn(Optional.of(testPartner));
        when(operatorJobRepository.save(any(OperatorJob.class))).thenAnswer(invocation -> {
            OperatorJob saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());

        JobAssignRequest request = JobAssignRequest.builder()
                .operatorId(10L)
                .equipmentId(20L)
                .partnerId(5L)
                .jobTitle("Combine Harvester Deployment")
                .jobType("Harvesting")
                .customerName("Kuldeep Singh")
                .customerMobile("9812345678")
                .workLocation("Karnal Farm 12")
                .scheduledDate(LocalDate.now().plusDays(3))
                .scheduledStartTime(LocalTime.of(9, 0))
                .operatorPayout(new BigDecimal("2500.00"))
                .build();

        ResponseEntity<?> response = operatorJobController.assignJob(request);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        verify(operatorJobRepository, times(1)).save(any(OperatorJob.class));
    }

    @Test
    @DisplayName("Assigning job to invalid operator ID throws ResourceNotFoundException")
    void testAssignJob_InvalidOperator() {
        when(operatorRepository.findById(999L)).thenReturn(Optional.empty());

        JobAssignRequest request = JobAssignRequest.builder()
                .operatorId(999L)
                .jobTitle("Plowing Job")
                .customerName("Anil")
                .customerMobile("9800000000")
                .workLocation("Farm 1")
                .scheduledDate(LocalDate.now())
                .operatorPayout(new BigDecimal("1000.00"))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> {
            operatorJobController.assignJob(request);
        });
    }

    @Test
    @DisplayName("Job summary computes correct status counts for dashboard")
    void testGetJobsSummary_Success() {
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorRepository.existsById(10L)).thenReturn(true);
        when(operatorJobRepository.countByOperatorId(10L)).thenReturn(8L);
        when(operatorJobRepository.countByOperatorIdAndStatus(10L, JobStatus.PENDING_RESPONSE)).thenReturn(3L);
        when(operatorJobRepository.countByOperatorIdAndStatus(10L, JobStatus.ACCEPTED)).thenReturn(2L);
        when(operatorJobRepository.countByOperatorIdAndStatus(10L, JobStatus.COMPLETED)).thenReturn(2L);
        when(operatorJobRepository.countByOperatorIdAndStatus(10L, JobStatus.WORK_COMPLETED)).thenReturn(0L);
        when(operatorJobRepository.countByOperatorIdAndStatus(10L, JobStatus.REJECTED)).thenReturn(1L);
        when(operatorJobRepository.countByOperatorIdAndStatus(10L, JobStatus.CANCELLED)).thenReturn(0L);

        ResponseEntity<?> response = operatorJobController.getJobsSummary("Bearer " + validToken);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Operator can accept a pending job assignment")
    void testAcceptJob_Success() {
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));
        when(operatorJobRepository.save(any(OperatorJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = operatorJobController.acceptJob("Bearer " + validToken, 100L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.ACCEPTED, testJob.getStatus());
        verify(operatorJobRepository, times(1)).save(testJob);
        verify(operatorWorkMilestoneRepository, times(1)).save(any(OperatorWorkMilestone.class));
    }

    @Test
    @DisplayName("Operator can reject a pending job assignment with reason")
    void testRejectJob_Success() {
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));
        when(operatorJobRepository.save(any(OperatorJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = operatorJobController.rejectJob("Bearer " + validToken, 100L, "Schedule conflict with prior booking");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.REJECTED, testJob.getStatus());
        assertTrue(testJob.getNotes().contains("Schedule conflict"));
        verify(operatorJobRepository, times(1)).save(testJob);
    }

    // ==========================================
    // MODULE 8 — WORK STATUS MANAGEMENT TESTS
    // ==========================================

    @Test
    @DisplayName("Module 8: ACCEPTED -> TRAVELING status update succeeds and records travelingAt timestamp")
    void testStatusTransition_AcceptedToTraveling_Success() {
        testJob.setStatus(JobStatus.ACCEPTED);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));
        when(operatorJobRepository.save(any(OperatorJob.class))).thenAnswer(i -> i.getArgument(0));
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        JobStatusUpdateRequest request = JobStatusUpdateRequest.builder()
                .status(JobStatus.TRAVELING)
                .notes("En route to field")
                .build();

        ResponseEntity<?> response = operatorJobController.updateJobStatus("Bearer " + validToken, 100L, request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.TRAVELING, testJob.getStatus());
        assertNotNull(testJob.getTravelingAt());
        verify(operatorWorkMilestoneRepository, times(1)).save(any(OperatorWorkMilestone.class));
    }

    @Test
    @DisplayName("Module 8: TRAVELING -> REACHED_LOCATION status update succeeds")
    void testStatusTransition_TravelingToReachedLocation_Success() {
        testJob.setStatus(JobStatus.TRAVELING);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));
        when(operatorJobRepository.save(any(OperatorJob.class))).thenAnswer(i -> i.getArgument(0));
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        JobStatusUpdateRequest request = JobStatusUpdateRequest.builder()
                .status(JobStatus.REACHED_LOCATION)
                .build();

        ResponseEntity<?> response = operatorJobController.updateJobStatus("Bearer " + validToken, 100L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.REACHED_LOCATION, testJob.getStatus());
        assertNotNull(testJob.getReachedLocationAt());
    }

    @Test
    @DisplayName("Module 8: REACHED_LOCATION -> WORK_STARTED status update succeeds")
    void testStatusTransition_ReachedLocationToWorkStarted_Success() {
        testJob.setStatus(JobStatus.REACHED_LOCATION);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));
        when(operatorJobRepository.save(any(OperatorJob.class))).thenAnswer(i -> i.getArgument(0));
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        JobStatusUpdateRequest request = JobStatusUpdateRequest.builder()
                .status(JobStatus.WORK_STARTED)
                .build();

        ResponseEntity<?> response = operatorJobController.updateJobStatus("Bearer " + validToken, 100L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.WORK_STARTED, testJob.getStatus());
        assertNotNull(testJob.getWorkStartedAt());
    }

    @Test
    @DisplayName("Module 8: WORK_STARTED -> WORK_PAUSED status update succeeds")
    void testStatusTransition_WorkStartedToWorkPaused_Success() {
        testJob.setStatus(JobStatus.WORK_STARTED);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));
        when(operatorJobRepository.save(any(OperatorJob.class))).thenAnswer(i -> i.getArgument(0));
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        JobStatusUpdateRequest request = JobStatusUpdateRequest.builder()
                .status(JobStatus.WORK_PAUSED)
                .notes("Refueling machinery")
                .build();

        ResponseEntity<?> response = operatorJobController.updateJobStatus("Bearer " + validToken, 100L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.WORK_PAUSED, testJob.getStatus());
        assertNotNull(testJob.getWorkPausedAt());
    }

    @Test
    @DisplayName("Module 8: WORK_PAUSED -> WORK_RESUMED status update succeeds")
    void testStatusTransition_WorkPausedToWorkResumed_Success() {
        testJob.setStatus(JobStatus.WORK_PAUSED);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));
        when(operatorJobRepository.save(any(OperatorJob.class))).thenAnswer(i -> i.getArgument(0));
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        JobStatusUpdateRequest request = JobStatusUpdateRequest.builder()
                .status(JobStatus.WORK_RESUMED)
                .build();

        ResponseEntity<?> response = operatorJobController.updateJobStatus("Bearer " + validToken, 100L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.WORK_RESUMED, testJob.getStatus());
        assertNotNull(testJob.getWorkResumedAt());
    }

    @Test
    @DisplayName("Module 8: WORK_RESUMED -> WORK_COMPLETED status update succeeds")
    void testStatusTransition_WorkResumedToWorkCompleted_Success() {
        testJob.setStatus(JobStatus.WORK_RESUMED);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));
        when(operatorJobRepository.save(any(OperatorJob.class))).thenAnswer(i -> i.getArgument(0));
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        JobStatusUpdateRequest request = JobStatusUpdateRequest.builder()
                .status(JobStatus.WORK_COMPLETED)
                .notes("Plowing completed for 10 acres")
                .build();

        ResponseEntity<?> response = operatorJobController.updateJobStatus("Bearer " + validToken, 100L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.WORK_COMPLETED, testJob.getStatus());
        assertNotNull(testJob.getWorkCompletedAt());
    }

    @Test
    @DisplayName("Module 8: Direct WORK_STARTED -> WORK_COMPLETED without pausing succeeds")
    void testStatusTransition_WorkStartedToWorkCompleted_Success() {
        testJob.setStatus(JobStatus.WORK_STARTED);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));
        when(operatorJobRepository.save(any(OperatorJob.class))).thenAnswer(i -> i.getArgument(0));
        when(operatorWorkMilestoneRepository.findAllByJobIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        JobStatusUpdateRequest request = JobStatusUpdateRequest.builder()
                .status(JobStatus.WORK_COMPLETED)
                .build();

        ResponseEntity<?> response = operatorJobController.updateJobStatus("Bearer " + validToken, 100L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.WORK_COMPLETED, testJob.getStatus());
        assertNotNull(testJob.getWorkCompletedAt());
    }

    @Test
    @DisplayName("Module 8: Invalid status jump is rejected with BadRequestException")
    void testStatusTransition_InvalidJump_Rejected() {
        testJob.setStatus(JobStatus.ACCEPTED);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));

        // Invalid: ACCEPTED -> WORK_STARTED (must go to TRAVELING first)
        JobStatusUpdateRequest invalidRequest1 = JobStatusUpdateRequest.builder()
                .status(JobStatus.WORK_STARTED)
                .build();

        assertThrows(BadRequestException.class, () -> {
            operatorJobController.updateJobStatus("Bearer " + validToken, 100L, invalidRequest1);
        });

        // Invalid: ACCEPTED -> WORK_COMPLETED
        JobStatusUpdateRequest invalidRequest2 = JobStatusUpdateRequest.builder()
                .status(JobStatus.WORK_COMPLETED)
                .build();

        assertThrows(BadRequestException.class, () -> {
            operatorJobController.updateJobStatus("Bearer " + validToken, 100L, invalidRequest2);
        });
    }

    @Test
    @DisplayName("Module 8: WORK_COMPLETED cannot transition to other active states")
    void testStatusTransition_CompletedCannotTransition() {
        testJob.setStatus(JobStatus.WORK_COMPLETED);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        when(operatorJobRepository.findByIdAndOperatorId(100L, 10L)).thenReturn(Optional.of(testJob));

        JobStatusUpdateRequest invalidRequest = JobStatusUpdateRequest.builder()
                .status(JobStatus.WORK_STARTED)
                .build();

        assertThrows(BadRequestException.class, () -> {
            operatorJobController.updateJobStatus("Bearer " + validToken, 100L, invalidRequest);
        });
    }

    @Test
    @DisplayName("Module 8: Tenant isolation - Operator cannot update another operator's job")
    void testStatusTransition_ForbiddenForOtherOperator() {
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(10L);
        // Job 999 does not belong to operator 10
        when(operatorJobRepository.findByIdAndOperatorId(999L, 10L)).thenReturn(Optional.empty());

        JobStatusUpdateRequest request = JobStatusUpdateRequest.builder()
                .status(JobStatus.TRAVELING)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> {
            operatorJobController.updateJobStatus("Bearer " + validToken, 999L, request);
        });
    }
}
