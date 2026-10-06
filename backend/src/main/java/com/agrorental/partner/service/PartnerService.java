package com.agrorental.partner.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.dto.PartnerChangePasswordRequest;
import com.agrorental.partner.dto.PartnerDashboardResponse;
import com.agrorental.partner.dto.PartnerProfileResponse;
import com.agrorental.partner.dto.PartnerProfileUpdateRequest;
import com.agrorental.partner.dto.PartnerRegistrationRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.review.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final OperatorRepository operatorRepository;

    public PartnerService(
            PartnerRepository partnerRepository,
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) EquipmentRepository equipmentRepository,
            @Autowired(required = false) BookingRepository bookingRepository,
            @Autowired(required = false) PaymentRepository paymentRepository,
            @Autowired(required = false) ReviewRepository reviewRepository,
            @Autowired(required = false) OperatorRepository operatorRepository) {
        this.partnerRepository = partnerRepository;
        this.passwordEncoder = passwordEncoder;
        this.equipmentRepository = equipmentRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.operatorRepository = operatorRepository;
    }

    @Transactional
    public Partner registerPartner(PartnerRegistrationRequest request) {
        if (partnerRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new BadRequestException("Mobile number already registered");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty() &&
                partnerRepository.existsByEmail(request.getEmail().trim())) {
            throw new BadRequestException("Email already registered");
        }

        Partner partner = Partner.builder()
                .fullName(request.getFullName().trim())
                .businessName(request.getBusinessName() != null ? request.getBusinessName().trim() : null)
                .mobileNumber(request.getMobileNumber().trim())
                .email(request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .gstNumber(request.getGstNumber() != null ? request.getGstNumber().trim() : null)
                .aadhaarNumber(request.getAadhaarNumber() != null ? request.getAadhaarNumber().trim() : null)
                .panNumber(request.getPanNumber() != null ? request.getPanNumber().trim() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .profilePhoto(request.getProfilePhoto() != null ? request.getProfilePhoto().trim() : null)
                .otpVerified(false)
                .verificationStatus(
                        Partner.VerificationStatus.PENDING
                )
                .build();

        return partnerRepository.save(partner);
    }

    @Transactional(readOnly = true)
    public PartnerProfileResponse getPartnerProfile(Long id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found with ID: " + id));

        return toProfileResponse(partner);
    }

    @Transactional
    public PartnerProfileResponse updatePartnerProfile(Long id, PartnerProfileUpdateRequest request) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found with ID: " + id));

        // Check if email changed and is already taken by another partner
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(partner.getEmail())) {
                partnerRepository.findByEmail(newEmail).ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BadRequestException("Email " + newEmail + " is already in use by another account.");
                    }
                });
            }
            partner.setEmail(newEmail);
        } else {
            partner.setEmail(null);
        }

        partner.setFullName(request.getFullName().trim());
        partner.setBusinessName(request.getBusinessName() != null ? request.getBusinessName().trim() : null);
        partner.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        partner.setGstNumber(request.getGstNumber() != null ? request.getGstNumber().trim() : null);
        partner.setAadhaarNumber(request.getAadhaarNumber() != null ? request.getAadhaarNumber().trim() : null);
        partner.setPanNumber(request.getPanNumber() != null ? request.getPanNumber().trim() : null);

        if (request.getProfilePhoto() != null) {
            partner.setProfilePhoto(request.getProfilePhoto().trim());
        }

        Partner updated = partnerRepository.save(partner);
        log.info("Successfully updated partner profile for ID: {}", id);
        return toProfileResponse(updated);
    }

    @Transactional
    public void changePartnerPassword(Long id, PartnerChangePasswordRequest request) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found with ID: " + id));

        // Validate current password
        if (partner.getPassword() != null && !passwordEncoder.matches(request.getCurrentPassword(), partner.getPassword())) {
            throw new BadRequestException("Current password provided is incorrect.");
        }

        // Check if new password is identical to old password
        if (passwordEncoder.matches(request.getNewPassword(), partner.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the current password.");
        }

        partner.setPassword(passwordEncoder.encode(request.getNewPassword()));
        partnerRepository.save(partner);
        log.info("Successfully changed password for partner ID: {}", id);
    }

    @Transactional(readOnly = true)
    public Optional<PartnerDashboardResponse> getPartnerDashboard(Long id) {
        Optional<Partner> partnerOpt = partnerRepository.findById(id);
        if (partnerOpt.isEmpty()) {
            return Optional.empty();
        }
        Partner partner = partnerOpt.get();

        // 1. Machinery metrics
        long totalMachines = 0;
        long activeMachines = 0;
        List<Equipment> partnerEquipment = new ArrayList<>();
        if (equipmentRepository != null) {
            partnerEquipment = equipmentRepository.findByPartnerId(id);
            totalMachines = partnerEquipment.size();
            activeMachines = partnerEquipment.stream()
                    .filter(e -> e.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE && !Boolean.TRUE.equals(e.getIsDisabled()))
                    .count();
        }

        // 2. Booking metrics & Status distribution
        long pendingBookings = 0;
        long completedBookings = 0;
        List<Booking> partnerBookings = new ArrayList<>();
        Map<String, Long> statusDistribution = new HashMap<>();
        statusDistribution.put("PENDING", 0L);
        statusDistribution.put("CONFIRMED", 0L);
        statusDistribution.put("ACCEPTED", 0L);
        statusDistribution.put("OPERATOR_ASSIGNED", 0L);
        statusDistribution.put("WORK_STARTED", 0L);
        statusDistribution.put("COMPLETED", 0L);
        statusDistribution.put("CANCELLED", 0L);
        statusDistribution.put("REJECTED", 0L);

        if (bookingRepository != null) {
            partnerBookings = bookingRepository.findByPartnerId(id);
            for (Booking b : partnerBookings) {
                if (b.getStatus() == BookingStatus.PENDING) pendingBookings++;
                if (b.getStatus() == BookingStatus.COMPLETED) completedBookings++;
                if (b.getStatus() != null) {
                    String stName = b.getStatus().name();
                    statusDistribution.put(stName, statusDistribution.getOrDefault(stName, 0L) + 1L);
                }
            }
        }

        // 3. Payment & Revenue metrics
        BigDecimal monthlyRevenue = BigDecimal.ZERO;
        List<Payment> partnerPayments = new ArrayList<>();
        if (paymentRepository != null) {
            partnerPayments = paymentRepository.findByPartnerIdOrderByCreatedAtDesc(id);
            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            monthlyRevenue = partnerPayments.stream()
                    .filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS && p.getPaymentDate() != null && !p.getPaymentDate().isBefore(startOfMonth))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 4. Operator metrics
        long activeOperators = 0;
        if (operatorRepository != null) {
            activeOperators = operatorRepository.findByStatus(OperatorStatus.APPROVED).size();
        }

        // 5. Customer Rating metrics
        double customerRatings = 0.0;
        if (reviewRepository != null) {
            Double avg = reviewRepository.findAverageRatingByPartnerId(id);
            if (avg != null && avg > 0) {
                customerRatings = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
            } else {
                customerRatings = 4.9;
            }
        }

        // 6. 5 Analytical Chart Series
        LocalDate now = LocalDate.now();
        List<PartnerDashboardResponse.MonthlyChartEntry> monthlyRevenueChart = new ArrayList<>();
        List<PartnerDashboardResponse.MonthlyCountEntry> bookingTrendChart = new ArrayList<>();
        List<PartnerDashboardResponse.MonthlyCountEntry> customerGrowth = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            String monthName = ym.getMonth().name().substring(0, 3);

            BigDecimal rev = partnerPayments.stream()
                    .filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS && p.getPaymentDate() != null && YearMonth.from(p.getPaymentDate().toLocalDate()).equals(ym))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long bCount = partnerBookings.stream()
                    .filter(b -> b.getCreatedAt() != null && YearMonth.from(b.getCreatedAt().toLocalDate()).equals(ym))
                    .count();

            long cCount = partnerBookings.stream()
                    .filter(b -> b.getCreatedAt() != null && YearMonth.from(b.getCreatedAt().toLocalDate()).equals(ym))
                    .map(Booking::getFarmerId)
                    .distinct()
                    .count();

            monthlyRevenueChart.add(new PartnerDashboardResponse.MonthlyChartEntry(monthName, rev, bCount));
            bookingTrendChart.add(new PartnerDashboardResponse.MonthlyCountEntry(monthName, bCount));
            customerGrowth.add(new PartnerDashboardResponse.MonthlyCountEntry(monthName, cCount));
        }

        // Machine Utilization
        List<PartnerDashboardResponse.MachineUtilizationEntry> machineUtilization = new ArrayList<>();
        for (Equipment eq : partnerEquipment) {
            List<Booking> eqBookings = partnerBookings.stream()
                    .filter(b -> b.getEquipment() != null && b.getEquipment().getId().equals(eq.getId()))
                    .toList();

            long days = 0;
            for (Booking b : eqBookings) {
                if (b.getStartDate() != null && b.getEndDate() != null) {
                    days += ChronoUnit.DAYS.between(b.getStartDate(), b.getEndDate()) + 1;
                }
            }
            double utilRate = Math.min(100.0, (days * 100.0) / 30.0);
            machineUtilization.add(PartnerDashboardResponse.MachineUtilizationEntry.builder()
                    .equipmentId(eq.getId())
                    .machineName(eq.getName())
                    .category(eq.getCategory() != null ? eq.getCategory().name() : "MACHINERY")
                    .totalBookings(eqBookings.size())
                    .totalRentalDays(days)
                    .utilizationRate(BigDecimal.valueOf(utilRate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                    .build());
        }

        return Optional.of(PartnerDashboardResponse.builder()
                .id(partner.getId())
                .fullName(partner.getFullName())
                .businessName(partner.getBusinessName())
                .mobileNumber(partner.getMobileNumber())
                .email(partner.getEmail())
                .address(partner.getAddress())
                .profilePhoto(partner.getProfilePhoto())
                .otpVerified(partner.isOtpVerified())
                .verificationStatus(partner.getVerificationStatus())
                .totalMachines(totalMachines)
                .activeMachines(activeMachines)
                .pendingBookings(pendingBookings)
                .completedBookings(completedBookings)
                .monthlyRevenue(monthlyRevenue)
                .activeOperators(activeOperators)
                .customerRatings(customerRatings)
                .monthlyRevenueChart(monthlyRevenueChart)
                .bookingTrendChart(bookingTrendChart)
                .machineUtilization(machineUtilization)
                .customerGrowth(customerGrowth)
                .bookingStatusDistribution(statusDistribution)
                .build());
    }
    @Transactional
public String sendOtp(Long partnerId) {

    Partner partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Partner not found with ID: " + partnerId));

    if (partner.isOtpVerified()) {
        throw new BadRequestException("Partner mobile number is already verified.");
    }

    String otp = String.format("%06d", new Random().nextInt(1000000));

    partner.setOtpCode(otp);
    partner.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

    partnerRepository.save(partner);

    log.info("OTP generated for partner ID: {}. OTP: {}", partnerId, otp);

    return otp;
}

@Transactional
public PartnerProfileResponse verifyOtp(Long partnerId, String otp) {

    Partner partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Partner not found with ID: " + partnerId));

    if (partner.isOtpVerified()) {
        throw new BadRequestException("Mobile number is already verified.");
    }

    if (partner.getOtpCode() == null) {
        throw new BadRequestException("OTP not generated. Please request a new OTP.");
    }

    if (partner.getOtpExpiry() == null ||
            LocalDateTime.now().isAfter(partner.getOtpExpiry())) {

        throw new BadRequestException("OTP has expired. Please request a new OTP.");
    }

    if (!partner.getOtpCode().equals(otp)) {
        throw new BadRequestException("Invalid OTP.");
    }

    partner.setOtpVerified(true);
    partner.setOtpCode(null);
    partner.setOtpExpiry(null);

    Partner updatedPartner = partnerRepository.save(partner);

    log.info("Partner OTP verified successfully for ID: {}", partnerId);

    return toProfileResponse(updatedPartner);
}

@Transactional
public String resendOtp(Long partnerId) {
    return sendOtp(partnerId);
}

    public PartnerProfileResponse toProfileResponse(Partner partner) {
        return PartnerProfileResponse.builder()
                .id(partner.getId())
                .fullName(partner.getFullName())
                .businessName(partner.getBusinessName())
                .mobileNumber(partner.getMobileNumber())
                .email(partner.getEmail())
                .address(partner.getAddress())
                .gstNumber(partner.getGstNumber())
                .aadhaarNumber(partner.getAadhaarNumber())
                .panNumber(partner.getPanNumber())
                .profilePhoto(partner.getProfilePhoto())
                .otpVerified(partner.isOtpVerified())
                .verificationStatus(partner.getVerificationStatus())
                .active(partner.isActive())
                .createdAt(partner.getCreatedAt())
                .updatedAt(partner.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PartnerProfileResponse> getAllPartners() {
        return partnerRepository.findAll().stream()
                .map(this::toProfileResponse)
                .toList();
    }

    @Transactional
    public PartnerProfileResponse approvePartnerKyc(Long partnerId) {

    Partner partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Partner not found with ID: " + partnerId));

    partner.setVerificationStatus(Partner.VerificationStatus.APPROVED);
    partner.setActive(true);

    Partner updatedPartner = partnerRepository.save(partner);

    log.info("Partner KYC approved for partner ID: {}", partnerId);

    return toProfileResponse(updatedPartner);
}

   @Transactional
public PartnerProfileResponse rejectPartnerKyc(Long partnerId) {

    Partner partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Partner not found with ID: " + partnerId));

    partner.setVerificationStatus(Partner.VerificationStatus.REJECTED);
    partner.setActive(false);

    Partner updatedPartner = partnerRepository.save(partner);

    log.info("Partner KYC rejected for partner ID: {}", partnerId);

    return toProfileResponse(updatedPartner);
}
}