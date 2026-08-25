package com.agrorental.operator;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.operator.controller.OperatorAssignmentController;
import com.agrorental.operator.dto.OperatorJobEarningsResponse;
import com.agrorental.operator.dto.OperatorJobHistoryResponse;
import com.agrorental.operator.dto.OperatorJobHistorySummaryResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorReview;
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
import com.agrorental.security.principal.OperatorPrincipal;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperatorJobHistoryTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorJobAssignmentRepository assignmentRepository;

    @Mock
    private OperatorJobAssignmentMapper assignmentMapper;

    @Mock
    private OperatorEarningsService earningsService;

    @Mock
    private OperatorReviewRepository reviewRepository;

    @Mock
    private OperatorLocationRepository locationRepository;

    @InjectMocks
    private OperatorAssignmentService assignmentService;

    private Operator testOperator;
    private OperatorJobAssignment testAssignment;
    private Booking testBooking;
    private Equipment testEquipment;

    @BeforeEach
    void setUp() {
        testOperator = new Operator();
        testOperator.setId(1L);
        testOperator.setFullName("Santosh Gaikwad");
        testOperator.setMobileNumber("9876543220");
        testOperator.setStatus(OperatorStatus.APPROVED);
        testOperator.setActive(true);
        testOperator.setMobileVerified(true);
        testOperator.setHourlyRate(new BigDecimal("500.00"));

        testEquipment = Equipment.builder()
                .name("Kubota DC-68G Combine Harvester")
                .brand("Kubota")
                .model("DC-68G")
                .category(EquipmentCategory.HARVESTER)
                .build();

        testBooking = Booking.builder()
                .farmerId(10L)
                .equipment(testEquipment)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().minusDays(4))
                .deliveryAddress("Survey No. 42, Baramati, Pune")
                .status(BookingStatus.CONFIRMED)
                .build();

        testAssignment = OperatorJobAssignment.builder()
                .operator(testOperator)
                .booking(testBooking)
                .assignmentStatus(OperatorAssignmentStatus.COMPLETED)
                .assignedAt(LocalDateTime.now().minusDays(5))
                .acceptedAt(LocalDateTime.now().minusDays(5))
                .workStartedAt(LocalDateTime.now().minusDays(5).plusHours(1))
                .completedAt(LocalDateTime.now().minusDays(5).plusHours(6))
                .build();
        testAssignment.setId(100L);
    }

    @Test
    @DisplayName("Should successfully retrieve paginated job history for active operator")
    void getJobHistory_success() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        Pageable pageable = PageRequest.of(0, 10);
        Page<OperatorJobAssignment> page = new PageImpl<>(List.of(testAssignment), pageable, 1);

        when(assignmentRepository.findJobHistory(eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        OperatorJobEarningsResponse mockEarnings = OperatorJobEarningsResponse.builder()
                .assignmentId(100L)
                .netWorkHours(5.0)
                .totalElapsedMinutes(300L)
                .pausedMinutes(0L)
                .grossEarnings(new BigDecimal("2500.00"))
                .build();

        when(earningsService.computeJobEarnings(eq(testAssignment), any(BigDecimal.class)))
                .thenReturn(mockEarnings);

        OperatorReview mockReview = OperatorReview.builder()
                .rating(5)
                .comment("Outstanding harvesting job!")
                .build();

        when(reviewRepository.findByAssignmentId(100L)).thenReturn(Optional.of(mockReview));
        when(locationRepository.findTopByAssignmentIdOrderByRecordedAtDesc(100L)).thenReturn(Optional.empty());

        Page<OperatorJobHistoryResponse> result = assignmentService.getJobHistory(
                1L, null, null, null, null, null, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        OperatorJobHistoryResponse item = result.getContent().get(0);
        assertEquals(100L, item.getAssignmentId());
        assertEquals("Kubota DC-68G Combine Harvester", item.getEquipmentName());
        assertEquals(OperatorAssignmentStatus.COMPLETED, item.getAssignmentStatus());
        assertEquals(5, item.getCustomerRating());
        assertEquals("Outstanding harvesting job!", item.getCustomerReview());
        assertEquals(new BigDecimal("2500.00"), item.getGrossEarnings());
    }

    @Test
    @DisplayName("Should reject job history query when operator is inactive")
    void getJobHistory_inactiveOperator_throwsForbidden() {
        testOperator.setActive(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(ForbiddenException.class, () ->
                assignmentService.getJobHistory(1L, null, null, null, null, null, pageable)
        );
    }

    @Test
    @DisplayName("Should calculate correct historical summary metrics")
    void getJobHistorySummary_success() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        when(assignmentRepository.findJobHistoryForSummary(eq(1L), isNull(), isNull(), isNull()))
                .thenReturn(List.of(testAssignment));

        OperatorJobEarningsResponse mockEarnings = OperatorJobEarningsResponse.builder()
                .netWorkMinutes(300L)
                .pausedMinutes(30L)
                .grossEarnings(new BigDecimal("2500.00"))
                .build();

        when(earningsService.computeJobEarnings(eq(testAssignment), any(BigDecimal.class)))
                .thenReturn(mockEarnings);

        OperatorReview mockReview = OperatorReview.builder()
                .rating(5)
                .build();
        when(reviewRepository.findByOperatorId(1L)).thenReturn(List.of(mockReview));

        OperatorJobHistorySummaryResponse summary = assignmentService.getJobHistorySummary(1L, null, null, null);

        assertNotNull(summary);
        assertEquals(1L, summary.getTotalHistoricalJobs());
        assertEquals(1L, summary.getCompletedJobs());
        assertEquals(0L, summary.getRejectedJobs());
        assertEquals(new BigDecimal("5.00"), summary.getTotalWorkHours());
        assertEquals(30L, summary.getTotalPausedMinutes());
        assertEquals(new BigDecimal("2500.00"), summary.getTotalGrossEarnings());
        assertEquals(5.0, summary.getAverageRating());
        assertEquals(1L, summary.getTotalReviewsCount());
    }

    @Test
    @DisplayName("Controller should return 200 OK for history archive and summary")
    void controller_historyEndpoints() {
        OperatorAssignmentService mockService = mock(OperatorAssignmentService.class);
        OperatorAssignmentController controller = new OperatorAssignmentController(mockService);
        OperatorPrincipal principal = OperatorPrincipal.builder()
                .id(1L)
                .mobileNumber("9876543220")
                .fullName("Santosh Gaikwad")
                .role("OPERATOR")
                .build();

        Page<OperatorJobHistoryResponse> emptyPage = new PageImpl<>(Collections.emptyList());
        when(mockService.getJobHistory(eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        ResponseEntity<ApiResponse<Page<OperatorJobHistoryResponse>>> res1 =
                controller.getJobHistory(principal, null, null, null, null, null, PageRequest.of(0, 10));

        assertEquals(HttpStatus.OK, res1.getStatusCode());
        assertTrue(res1.getBody().isSuccess());

        OperatorJobHistorySummaryResponse mockSum = OperatorJobHistorySummaryResponse.builder()
                .totalHistoricalJobs(0)
                .build();
        when(mockService.getJobHistorySummary(eq(1L), isNull(), isNull(), isNull()))
                .thenReturn(mockSum);

        ResponseEntity<ApiResponse<OperatorJobHistorySummaryResponse>> res2 =
                controller.getJobHistorySummary(principal, null, null, null);

        assertEquals(HttpStatus.OK, res2.getStatusCode());
        assertTrue(res2.getBody().isSuccess());
    }
}
