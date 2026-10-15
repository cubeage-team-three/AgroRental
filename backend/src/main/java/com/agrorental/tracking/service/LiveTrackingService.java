package com.agrorental.tracking.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorLocation;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorLocationRepository;
import com.agrorental.security.principal.CurrentCaller;
import com.agrorental.tracking.dto.TrackingResponse;
import com.agrorental.tracking.dto.TrackingUpdateRequest;
import com.agrorental.tracking.entity.Tracking;
import com.agrorental.tracking.repository.TrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Farmer live tracking. Never invents data: positions, timestamps, ETA, route and progress come only from what
 * was recorded (operator GPS fixes or the stored tracking record) or from the booking itself.
 *
 * <ul>
 *   <li>Not yet in the field (PENDING, ACCEPTED, CONFIRMED) or closed (CANCELLED, REJECTED): 200 with no position,
 *       no ETA/route/progress, {@code trackingAvailable=false} and an explanatory {@code message}.</li>
 *   <li>In the field (OPERATOR_ASSIGNED, ON_THE_WAY, WORK_STARTED): the latest operator GPS fix
 *       ({@code locationSource=OPERATOR_GPS}), else the stored tracking record ({@code TRACKING_RECORD}); with
 *       neither, the equipment's registered location as a labelled reference ({@code EQUIPMENT_BASE},
 *       {@code trackingAvailable=false}, no timestamp) or no position at all if the equipment has none.</li>
 *   <li>COMPLETED: progress 100, the last recorded position if any, no ETA/route, {@code trackingAvailable=false}.</li>
 * </ul>
 * Ownership is checked before any data is read.
 */
@Service
@RequiredArgsConstructor
public class LiveTrackingService {

    public static final String SOURCE_OPERATOR_GPS = "OPERATOR_GPS";
    public static final String SOURCE_TRACKING_RECORD = "TRACKING_RECORD";
    public static final String SOURCE_EQUIPMENT_BASE = "EQUIPMENT_BASE";

    /** Booking states in which an operator is assigned to the job and live tracking is meaningful. */
    private static final Set<BookingStatus> IN_FIELD_STATUSES =
            EnumSet.of(BookingStatus.OPERATOR_ASSIGNED, BookingStatus.ON_THE_WAY, BookingStatus.WORK_STARTED);

    private final TrackingRepository trackingRepository;
    private final BookingRepository bookingRepository;
    private final OperatorJobAssignmentRepository assignmentRepository;
    private final OperatorLocationRepository operatorLocationRepository;

    @Transactional(readOnly = true)
    public TrackingResponse getTrackingByBookingId(Long bookingId, CurrentCaller caller) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));
        assertBookingOwner(booking, caller);

        BookingStatus status = booking.getStatus();
        boolean inField = IN_FIELD_STATUSES.contains(status);
        boolean completed = status == BookingStatus.COMPLETED;

        TrackingResponse response = TrackingResponse.builder()
                .bookingId(booking.getId())
                .status(status != null ? status.name() : null)
                .equipmentName(booking.getEquipment() != null ? booking.getEquipment().getName() : null)
                .trackingAvailable(false)
                .build();

        if (!inField && !completed) {
            response.setMessage(status == BookingStatus.CANCELLED || status == BookingStatus.REJECTED
                    ? "Booking is " + status + "; live tracking is not available."
                    : "Live tracking starts once an operator is assigned to this booking.");
            return response;
        }

        Operator operator = booking.getOperator();
        if (operator != null) {
            response.setOperatorId(operator.getId());
            response.setOperatorName(operator.getFullName());
            response.setOperatorMobile(operator.getMobileNumber());
        }

        Tracking record = trackingRepository.findByBookingId(bookingId).orElse(null);
        // A booking can hold several assignments (decline + reassignment): use the current one
        Optional<OperatorLocation> latestFix = assignmentRepository.findCurrentAssignmentForBooking(bookingId)
                .flatMap(a -> operatorLocationRepository.findTopByAssignmentIdOrderByRecordedAtDesc(a.getId()));

        if (record != null) {
            response.setId(record.getId());
            if (inField) {
                response.setEta(record.getEta());
                response.setRouteInformation(record.getRouteInformation());
                response.setWorkProgress(record.getWorkProgress());
            }
        }

        if (latestFix.isPresent()) {
            OperatorLocation fix = latestFix.get();
            setPosition(response, fix.getLatitude(), fix.getLongitude(), fix.getRecordedAt(), SOURCE_OPERATOR_GPS);
            response.setTrackingAvailable(inField);
        } else if (record != null && record.getLatitude() != null && record.getLongitude() != null) {
            setPosition(response, record.getLatitude(), record.getLongitude(), record.getLastUpdated(), SOURCE_TRACKING_RECORD);
            response.setTrackingAvailable(inField);
        } else if (inField) {
            Equipment equipment = booking.getEquipment();
            if (equipment != null && equipment.getLatitude() != null && equipment.getLongitude() != null) {
                // A labelled reference point (where the machine is registered), never a live position or timestamp
                setPosition(response, equipment.getLatitude(), equipment.getLongitude(), null, SOURCE_EQUIPMENT_BASE);
            }
        }

        if (completed) {
            response.setWorkProgress(100);
            response.setMessage("The job is completed.");
        } else if (response.isTrackingAvailable()) {
            response.setMessage(SOURCE_OPERATOR_GPS.equals(response.getLocationSource())
                    ? "Live position from the operator's device."
                    : "Position from the latest tracking record.");
        } else {
            response.setMessage(response.getLocationSource() != null
                    ? "No live position has been recorded yet; showing the equipment's registered location."
                    : "No live position has been recorded yet.");
        }
        return response;
    }

    @Transactional
    public TrackingResponse updateTracking(Long bookingId, TrackingUpdateRequest request, CurrentCaller caller) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));
        assertBookingOwner(booking, caller);

        Tracking tracking = trackingRepository.findByBookingId(bookingId)
                .orElse(Tracking.builder()
                        .bookingId(bookingId)
                        .operatorId(booking.getOperator() != null ? booking.getOperator().getId() : null)
                        .build());

        if (request.getLatitude() != null) tracking.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) tracking.setLongitude(request.getLongitude());
        if (request.getEta() != null) tracking.setEta(request.getEta());
        if (request.getRouteInformation() != null) tracking.setRouteInformation(request.getRouteInformation());
        if (request.getWorkProgress() != null) tracking.setWorkProgress(request.getWorkProgress());
        tracking.setLastUpdated(LocalDateTime.now());

        trackingRepository.save(tracking);
        return getTrackingByBookingId(bookingId, caller);
    }

    private static void setPosition(TrackingResponse response, Double latitude, Double longitude,
                                    LocalDateTime recordedAt, String source) {
        response.setLatitude(latitude);
        response.setLongitude(longitude);
        response.setLastUpdated(recordedAt);
        response.setLocationSource(source);
    }

    /**
     * Tracking (operator identity, mobile and live location) is visible to, and writable by, only the
     * farmer who owns the booking (or an admin). Checked before any tracking data is read or written.
     */
    private void assertBookingOwner(Booking booking, CurrentCaller caller) {
        if (caller == null || !(caller.isAdmin() || caller.is(CurrentCaller.Kind.FARMER, booking.getFarmerId()))) {
            throw new ForbiddenException("Access is denied. You are not authorized to access tracking for this booking.");
        }
    }
}
