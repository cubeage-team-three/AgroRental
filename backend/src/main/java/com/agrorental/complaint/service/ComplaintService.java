package com.agrorental.complaint.service;

import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.complaint.dto.ComplaintCreateRequest;
import com.agrorental.complaint.dto.ComplaintResponse;
import com.agrorental.complaint.entity.Complaint;
import com.agrorental.complaint.enums.ComplaintStatus;
import com.agrorental.complaint.repository.ComplaintRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final FarmerRepository farmerRepository;

    @Transactional
    public ComplaintResponse createComplaint(ComplaintCreateRequest request) {
        Complaint complaint = Complaint.builder()
                .farmerId(request.getFarmerId())
                .bookingId(request.getBookingId())
                .category(request.getCategory())
                .description(request.getDescription())
                .status(ComplaintStatus.OPEN)
                .build();

        complaint = complaintRepository.save(complaint);
        return mapToResponse(complaint);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsByFarmerId(Long farmerId) {
        return complaintRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsByStatus(ComplaintStatus status) {
        return complaintRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));
        return mapToResponse(complaint);
    }

    @Transactional
    public ComplaintResponse updateStatus(Long complaintId, ComplaintStatus status, String resolutionNote) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        complaint.setStatus(status);
        if (resolutionNote != null && !resolutionNote.isBlank()) {
            complaint.setResolutionNote(resolutionNote);
        }

        complaint = complaintRepository.save(complaint);
        return mapToResponse(complaint);
    }

    private ComplaintResponse mapToResponse(Complaint complaint) {
        Farmer farmer = farmerRepository.findById(complaint.getFarmerId()).orElse(null);

        return ComplaintResponse.builder()
                .id(complaint.getId())
                .farmerId(complaint.getFarmerId())
                .farmerName(farmer != null ? farmer.getFullName() : null)
                .farmerMobile(farmer != null ? farmer.getMobileNumber() : null)
                .bookingId(complaint.getBookingId())
                .category(complaint.getCategory())
                .description(complaint.getDescription())
                .status(complaint.getStatus())
                .resolutionNote(complaint.getResolutionNote())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }
}
