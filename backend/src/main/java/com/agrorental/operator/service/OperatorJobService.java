package com.agrorental.operator.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.operator.dto.JobAssignRequest;
import com.agrorental.operator.dto.OperatorJobResponse;
import com.agrorental.operator.dto.OperatorJobSummaryResponse;
import com.agrorental.operator.entity.JobStatus;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJob;
import com.agrorental.operator.repository.OperatorJobRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OperatorJobService {

    private final OperatorJobRepository operatorJobRepository;
    private final OperatorRepository operatorRepository;
    private final EquipmentRepository equipmentRepository;
    private final PartnerRepository partnerRepository;

    public OperatorJobService(
            OperatorJobRepository operatorJobRepository,
            OperatorRepository operatorRepository,
            EquipmentRepository equipmentRepository,
            PartnerRepository partnerRepository) {
        this.operatorJobRepository = operatorJobRepository;
        this.operatorRepository = operatorRepository;
        this.equipmentRepository = equipmentRepository;
        this.partnerRepository = partnerRepository;
    }

    @Transactional(readOnly = true)
    public List<OperatorJobResponse> getAssignedJobs(Long operatorId, JobStatus status) {
        if (!operatorRepository.existsById(operatorId)) {
            throw new ResourceNotFoundException("Operator not found with id: " + operatorId);
        }

        List<OperatorJob> jobs;
        if (status != null) {
            jobs = operatorJobRepository.findAllByOperatorIdAndStatusOrderByScheduledDateDescCreatedAtDesc(operatorId, status);
        } else {
            jobs = operatorJobRepository.findAllByOperatorIdOrderByScheduledDateDescCreatedAtDesc(operatorId);
        }

        return jobs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OperatorJobResponse getJobDetails(Long operatorId, Long jobId) {
        OperatorJob job = operatorJobRepository.findByIdAndOperatorId(jobId, operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Job assignment not found or access denied for ID: " + jobId));

        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public OperatorJobSummaryResponse getJobsSummary(Long operatorId) {
        if (!operatorRepository.existsById(operatorId)) {
            throw new ResourceNotFoundException("Operator not found with id: " + operatorId);
        }

        long total = operatorJobRepository.countByOperatorId(operatorId);
        long pending = operatorJobRepository.countByOperatorIdAndStatus(operatorId, JobStatus.PENDING_RESPONSE);
        long accepted = operatorJobRepository.countByOperatorIdAndStatus(operatorId, JobStatus.ACCEPTED);
        long completed = operatorJobRepository.countByOperatorIdAndStatus(operatorId, JobStatus.COMPLETED);
        long rejected = operatorJobRepository.countByOperatorIdAndStatus(operatorId, JobStatus.REJECTED);
        long cancelled = operatorJobRepository.countByOperatorIdAndStatus(operatorId, JobStatus.CANCELLED);

        return OperatorJobSummaryResponse.builder()
                .operatorId(operatorId)
                .totalAssigned(total)
                .pendingResponse(pending)
                .accepted(accepted)
                .completed(completed)
                .rejected(rejected)
                .cancelled(cancelled)
                .build();
    }

    @Transactional
    public OperatorJobResponse assignJob(JobAssignRequest request) {
        Operator operator = operatorRepository.findById(request.getOperatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + request.getOperatorId()));

        Equipment equipment = null;
        if (request.getEquipmentId() != null) {
            equipment = equipmentRepository.findById(request.getEquipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + request.getEquipmentId()));
        }

        Partner partner = null;
        if (request.getPartnerId() != null) {
            partner = partnerRepository.findById(request.getPartnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Partner not found with id: " + request.getPartnerId()));
        } else if (equipment != null && equipment.getPartner() != null) {
            partner = equipment.getPartner();
        }

        OperatorJob job = OperatorJob.builder()
                .operator(operator)
                .equipment(equipment)
                .partner(partner)
                .jobTitle(request.getJobTitle().trim())
                .jobType(request.getJobType() != null ? request.getJobType().trim() : null)
                .jobDescription(request.getJobDescription() != null ? request.getJobDescription().trim() : null)
                .workInstructions(request.getWorkInstructions() != null ? request.getWorkInstructions().trim() : null)
                .customerName(request.getCustomerName().trim())
                .customerMobile(request.getCustomerMobile().trim())
                .workLocation(request.getWorkLocation().trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .scheduledDate(request.getScheduledDate())
                .scheduledStartTime(request.getScheduledStartTime())
                .scheduledEndTime(request.getScheduledEndTime())
                .estimatedDurationHours(request.getEstimatedDurationHours())
                .operatorPayout(request.getOperatorPayout())
                .status(JobStatus.PENDING_RESPONSE)
                .assignedBy(request.getAssignedBy() != null ? request.getAssignedBy().trim() : "Admin / Partner")
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();

        OperatorJob savedJob = operatorJobRepository.save(job);
        log.info("Job assigned successfully with ID: {} to operator: {}", savedJob.getId(), operator.getMobileNumber());

        return mapToResponse(savedJob);
    }

    @Transactional
    public OperatorJobResponse acceptJob(Long operatorId, Long jobId) {
        OperatorJob job = operatorJobRepository.findByIdAndOperatorId(jobId, operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Job assignment not found or access denied for ID: " + jobId));

        if (job.getStatus() != JobStatus.PENDING_RESPONSE && job.getStatus() != JobStatus.ASSIGNED) {
            throw new BadRequestException("Cannot accept job in status: " + job.getStatus());
        }

        job.setStatus(JobStatus.ACCEPTED);
        OperatorJob updated = operatorJobRepository.save(job);
        log.info("Job ID {} accepted by operator ID {}", jobId, operatorId);
        return mapToResponse(updated);
    }

    @Transactional
    public OperatorJobResponse rejectJob(Long operatorId, Long jobId, String reason) {
        OperatorJob job = operatorJobRepository.findByIdAndOperatorId(jobId, operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Job assignment not found or access denied for ID: " + jobId));

        if (job.getStatus() != JobStatus.PENDING_RESPONSE && job.getStatus() != JobStatus.ASSIGNED) {
            throw new BadRequestException("Cannot reject job in status: " + job.getStatus());
        }

        job.setStatus(JobStatus.REJECTED);
        if (reason != null && !reason.trim().isEmpty()) {
            job.setNotes(job.getNotes() != null ? job.getNotes() + " | Rejection Reason: " + reason.trim() : "Rejection Reason: " + reason.trim());
        }
        OperatorJob updated = operatorJobRepository.save(job);
        log.info("Job ID {} rejected by operator ID {}", jobId, operatorId);
        return mapToResponse(updated);
    }

    private OperatorJobResponse mapToResponse(OperatorJob job) {
        Operator operator = job.getOperator();
        Equipment equipment = job.getEquipment();
        Partner partner = job.getPartner();

        return OperatorJobResponse.builder()
                .id(job.getId())
                .operatorId(operator.getId())
                .operatorName(operator.getFullName())
                .operatorMobile(operator.getMobileNumber())
                .equipmentId(equipment != null ? equipment.getId() : null)
                .equipmentName(equipment != null ? equipment.getName() : null)
                .equipmentCategory(equipment != null && equipment.getCategory() != null ? equipment.getCategory().name() : null)
                .equipmentBrand(equipment != null ? equipment.getBrand() : null)
                .equipmentModel(equipment != null ? equipment.getModel() : null)
                .partnerId(partner != null ? partner.getId() : null)
                .partnerName(partner != null ? partner.getFullName() : null)
                .partnerMobile(partner != null ? partner.getMobileNumber() : null)
                .customerName(job.getCustomerName())
                .customerMobile(job.getCustomerMobile())
                .jobTitle(job.getJobTitle())
                .jobType(job.getJobType())
                .jobDescription(job.getJobDescription())
                .workInstructions(job.getWorkInstructions())
                .scheduledDate(job.getScheduledDate())
                .scheduledStartTime(job.getScheduledStartTime())
                .scheduledEndTime(job.getScheduledEndTime())
                .estimatedDurationHours(job.getEstimatedDurationHours())
                .workLocation(job.getWorkLocation())
                .latitude(job.getLatitude())
                .longitude(job.getLongitude())
                .operatorPayout(job.getOperatorPayout())
                .status(job.getStatus())
                .assignedBy(job.getAssignedBy())
                .notes(job.getNotes())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
