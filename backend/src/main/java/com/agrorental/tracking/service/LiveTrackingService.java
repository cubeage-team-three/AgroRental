package com.agrorental.tracking.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.tracking.dto.TrackingResponse;
import com.agrorental.tracking.dto.TrackingUpdateRequest;
import com.agrorental.tracking.entity.Tracking;
import com.agrorental.tracking.repository.TrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LiveTrackingService {

    private final TrackingRepository trackingRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public TrackingResponse getTrackingByBookingId(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        Tracking tracking = trackingRepository.findByBookingId(bookingId)
                .orElseGet(() -> createMockTrackingForBooking(booking));

        String operatorName = booking.getOperator() != null ? booking.getOperator().getFullName() : "Assigned Operator";
        String operatorMobile = booking.getOperator() != null ? booking.getOperator().getMobileNumber() : "Contact via Partner";
        String equipmentName = booking.getEquipment() != null ? booking.getEquipment().getName() : "Agricultural Equipment";

        return TrackingResponse.builder()
                .id(tracking.getId())
                .bookingId(booking.getId())
                .operatorId(booking.getOperator() != null ? booking.getOperator().getId() : null)
                .operatorName(operatorName)
                .operatorMobile(operatorMobile)
                .equipmentName(equipmentName)
                .latitude(tracking.getLatitude())
                .longitude(tracking.getLongitude())
                .eta(tracking.getEta())
                .routeInformation(tracking.getRouteInformation())
                .workProgress(tracking.getWorkProgress())
                .status(booking.getStatus() != null ? booking.getStatus().name() : "IN_PROGRESS")
                .lastUpdated(tracking.getLastUpdated() != null ? tracking.getLastUpdated() : LocalDateTime.now())
                .build();
    }

    @Transactional
    public TrackingResponse updateTracking(Long bookingId, TrackingUpdateRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

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

        tracking = trackingRepository.save(tracking);
        return getTrackingByBookingId(bookingId);
    }

    private Tracking createMockTrackingForBooking(Booking booking) {
        double baseLat = booking.getEquipment() != null && booking.getEquipment().getLatitude() != null 
                ? booking.getEquipment().getLatitude() : 18.5204;
        double baseLng = booking.getEquipment() != null && booking.getEquipment().getLongitude() != null 
                ? booking.getEquipment().getLongitude() : 73.8567;

        return Tracking.builder()
                .bookingId(booking.getId())
                .operatorId(booking.getOperator() != null ? booking.getOperator().getId() : null)
                .latitude(baseLat + 0.005)
                .longitude(baseLng + 0.005)
                .eta("25 mins")
                .routeInformation("On route via NH-48 towards target farm location")
                .workProgress(45)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
