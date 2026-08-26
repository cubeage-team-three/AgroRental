package com.agrorental.common.config;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import com.agrorental.equipment.entity.EquipmentImage;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.notification.entity.Notification;
import com.agrorental.notification.repository.NotificationRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.entity.OperatorJobPauseInterval;
import com.agrorental.operator.entity.OperatorReview;
import com.agrorental.operator.repository.OperatorJobPauseIntervalRepository;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.repository.OperatorReviewRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.review.entity.Review;
import com.agrorental.review.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Initializes rich mock seed data on application boot if in-memory database is empty.
 * Guarantees Partner ID 1, Farmer ID 1, Equipment, Operators, Bookings, Earnings, and Reviews exist.
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final PartnerRepository partnerRepository;
    private final FarmerRepository farmerRepository;
    private final FarmRepository farmRepository;
    private final EquipmentRepository equipmentRepository;
    private final OperatorRepository operatorRepository;
    private final OperatorJobAssignmentRepository operatorJobAssignmentRepository;
    private final OperatorJobPauseIntervalRepository pauseIntervalRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;
    private final OperatorReviewRepository operatorReviewRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            PartnerRepository partnerRepository,
            FarmerRepository farmerRepository,
            FarmRepository farmRepository,
            EquipmentRepository equipmentRepository,
            OperatorRepository operatorRepository,
            OperatorJobAssignmentRepository operatorJobAssignmentRepository,
            OperatorJobPauseIntervalRepository pauseIntervalRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            NotificationRepository notificationRepository,
            ReviewRepository reviewRepository,
            OperatorReviewRepository operatorReviewRepository,
            PasswordEncoder passwordEncoder) {
        this.partnerRepository = partnerRepository;
        this.farmerRepository = farmerRepository;
        this.farmRepository = farmRepository;
        this.equipmentRepository = equipmentRepository;
        this.operatorRepository = operatorRepository;
        this.operatorJobAssignmentRepository = operatorJobAssignmentRepository;
        this.pauseIntervalRepository = pauseIntervalRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.reviewRepository = reviewRepository;
        this.operatorReviewRepository = operatorReviewRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (partnerRepository.count() > 0) {
            log.info("Database already seeded. Skipping DataInitializer.");
            return;
        }

        log.info("Seeding initial mock data for AgroRental platform...");

        // 1. Seed Partner #1
        Partner partner = Partner.builder()
                .fullName("Rajesh Patel")
                .businessName("Kisan Agro Rental Services")
                .mobileNumber("9876543210")
                .email("partner@agrorent.in")
                .address("Shop 12, APMC Market Yard, Pune, Maharashtra 411037")
                .gstNumber("27AABCK1234F1Z8")
                .aadhaarNumber("548912347890")
                .panNumber("ABCDE1234F")
                .password(passwordEncoder.encode("Partner@123"))
                .profilePhoto("https://images.unsplash.com/photo-1560250097-0b93528c311a?w=400&auto=format&fit=crop&q=80")
                .otpVerified(true)
                .verificationStatus(Partner.VerificationStatus.APPROVED)
                .build();
        partner = partnerRepository.save(partner);
        log.info("Seeded Partner ID: {}", partner.getId());

        // 2. Seed Farmer #1 & Farm #1
        Farmer farmer = Farmer.builder()
                .fullName("Ramesh Yadav")
                .mobileNumber("9876543211")
                .email("ramesh@agrorent.in")
                .address("Plot 45, Shirur Village, Pune 412210")
                .password(passwordEncoder.encode("Farmer@123"))
                .profileImage("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80")
                .accountStatus("ACTIVE")
                .build();
        farmer = farmerRepository.save(farmer);

        Farm farm = Farm.builder()
                .farmerId(farmer.getId())
                .farmName("Green Valley Farm")
                .village("Shirur")
                .taluka("Shirur")
                .district("Pune")
                .state("Maharashtra")
                .farmArea(new BigDecimal("5.5"))
                .cropType("Sugarcane & Wheat")
                .build();
        farm = farmRepository.save(farm);

        // 3. Seed Equipment listings
        Equipment tractor1 = Equipment.builder()
                .name("John Deere 5310 4WD Tractor")
                .category(EquipmentCategory.TRACTOR)
                .brand("John Deere")
                .model("5310 4WD")
                .manufacturingYear(2022)
                .capacity("55 HP")
                .fuelType(FuelType.DIESEL)
                .rentalPrice(new BigDecimal("1800.00"))
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .partner(partner)
                .locationAddress("Shirur MIDC Road, Pune, Maharashtra 412210")
                .latitude(18.8256)
                .longitude(74.3789)
                .description("55 HP powerful 4WD tractor equipped with power steering and dual clutch, ideal for deep tillage, heavy ploughing, and haulage.")
                .isDisabled(false)
                .build();
        EquipmentImage img1 = EquipmentImage.builder()
                .imageUrl("https://images.unsplash.com/photo-1592878904946-b3cd8ae243d0?w=600&auto=format&fit=crop&q=80")
                .isPrimary(true)
                .equipment(tractor1)
                .build();
        tractor1.setImages(List.of(img1));
        tractor1 = equipmentRepository.save(tractor1);

        Equipment tractor2 = Equipment.builder()
                .name("Mahindra 575 DI XP Plus Tractor")
                .category(EquipmentCategory.TRACTOR)
                .brand("Mahindra")
                .model("575 DI XP Plus")
                .manufacturingYear(2023)
                .capacity("47 HP")
                .fuelType(FuelType.DIESEL)
                .rentalPrice(new BigDecimal("1400.00"))
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .partner(partner)
                .locationAddress("Khed Agro Hub, Pune, Maharashtra 410501")
                .latitude(18.8500)
                .longitude(73.9100)
                .description("47 HP reliable fuel-efficient tractor, perfect for medium agricultural tasks, sowing, and trailer haulage.")
                .isDisabled(false)
                .build();
        EquipmentImage img2 = EquipmentImage.builder()
                .imageUrl("https://images.unsplash.com/photo-1589923188900-85dae523342b?w=600&auto=format&fit=crop&q=80")
                .isPrimary(true)
                .equipment(tractor2)
                .build();
        tractor2.setImages(List.of(img2));
        tractor2 = equipmentRepository.save(tractor2);

        Equipment harvester = Equipment.builder()
                .name("Kubota DC-68G Combine Harvester")
                .category(EquipmentCategory.HARVESTER)
                .brand("Kubota")
                .model("DC-68G")
                .manufacturingYear(2021)
                .capacity("68 HP")
                .fuelType(FuelType.DIESEL)
                .rentalPrice(new BigDecimal("3500.00"))
                .availabilityStatus(AvailabilityStatus.BOOKED)
                .partner(partner)
                .locationAddress("Talegaon Dabhade Farm Yard, Pune, Maharashtra 410506")
                .latitude(18.7300)
                .longitude(73.6800)
                .description("68 HP high efficiency paddy and wheat combine harvester with rubber crawler tracks for wet field operation.")
                .isDisabled(false)
                .build();
        EquipmentImage img3 = EquipmentImage.builder()
                .imageUrl("https://images.unsplash.com/photo-1586771107445-d3ca888129ff?w=600&auto=format&fit=crop&q=80")
                .isPrimary(true)
                .equipment(harvester)
                .build();
        harvester.setImages(List.of(img3));
        harvester = equipmentRepository.save(harvester);

        Equipment rotavator = Equipment.builder()
                .name("Shaktiman Regular Light Rotavator")
                .category(EquipmentCategory.TILLER)
                .brand("Shaktiman")
                .model("Regular Light 6ft")
                .manufacturingYear(2023)
                .capacity("6 Feet")
                .fuelType(FuelType.MANUAL_HUMAN_POWERED)
                .rentalPrice(new BigDecimal("800.00"))
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .partner(partner)
                .locationAddress("Manchar Mandi Road, Pune, Maharashtra 410503")
                .latitude(19.0000)
                .longitude(73.9400)
                .description("6 Feet 42 blade multi-speed gearbox rotavator for fine seedbed preparation in single pass.")
                .isDisabled(false)
                .build();
        EquipmentImage img4 = EquipmentImage.builder()
                .imageUrl("https://images.unsplash.com/photo-1500937386664-56d1dfef3854?w=600&auto=format&fit=crop&q=80")
                .isPrimary(true)
                .equipment(rotavator)
                .build();
        rotavator.setImages(List.of(img4));
        rotavator = equipmentRepository.save(rotavator);

        // 4. Seed Operators
        Operator op1 = new Operator();
        op1.setFullName("Santosh Gaikwad");
        op1.setMobileNumber("9876543220");
        op1.setEmail("santosh.operator@agrorent.in");
        op1.setAddress("Village Khed, Taluka Rajgurunagar, Pune");
        op1.setAadhaarNumber("654398761234");
        op1.setDrivingLicenseNumber("MH-14-2018-009876");
        op1.setExperience(5);
        op1.setSkills("Tractor & Harvester Specialist");
        op1.setPassword(passwordEncoder.encode("Operator@123"));
        op1.setStatus(OperatorStatus.APPROVED);
        op1.setMobileVerified(true);
        op1.setPartner(partner);
        op1 = operatorRepository.save(op1);

        Operator op2 = new Operator();
        op2.setFullName("Balasaheb Kadam");
        op2.setMobileNumber("9876543221");
        op2.setEmail("balasaheb.operator@agrorent.in");
        op2.setAddress("Narayangaon, Junnar, Pune");
        op2.setAadhaarNumber("789012345678");
        op2.setDrivingLicenseNumber("MH-14-2016-004321");
        op2.setExperience(7);
        op2.setSkills("Heavy Machinery & Rotavator");
        op2.setPassword(passwordEncoder.encode("Operator@123"));
        op2.setStatus(OperatorStatus.APPROVED);
        op2.setMobileVerified(true);
        op2.setPartner(partner);
        op2 = operatorRepository.save(op2);

        Operator op3 = new Operator();
        op3.setFullName("Anil Jadhav");
        op3.setMobileNumber("9876543222");
        op3.setEmail("anil.operator@agrorent.in");
        op3.setAddress("Chakan, Pune");
        op3.setAadhaarNumber("890123456789");
        op3.setDrivingLicenseNumber("MH-14-2020-001234");
        op3.setExperience(4);
        op3.setSkills("Tractor Driver & Land Preparation");
        op3.setPassword(passwordEncoder.encode("Operator@123"));
        op3.setStatus(OperatorStatus.APPROVED);
        op3.setMobileVerified(true);
        op3.setPartner(partner);
        op3 = operatorRepository.save(op3);

        // 5. Seed Bookings
        LocalDate today = LocalDate.now();

        // Booking 1: Pending Request (FR-17)
        Booking b1 = Booking.builder()
                .farmerId(farmer.getId())
                .farm(farm)
                .equipment(tractor1)
                .partner(partner)
                .startDate(today.plusDays(1))
                .endDate(today.plusDays(3))
                .totalCost(new BigDecimal("5400.00"))
                .status(BookingStatus.PENDING)
                .deliveryAddress("Green Valley Farm, Shirur, Pune")
                .notes("Need tractor early morning at 7:00 AM for tillage")
                .build();
        b1 = bookingRepository.save(b1);

        // Booking 2: Confirmed Request
        Booking b2 = Booking.builder()
                .farmerId(farmer.getId())
                .farm(farm)
                .equipment(tractor2)
                .partner(partner)
                .startDate(today.plusDays(5))
                .endDate(today.plusDays(6))
                .totalCost(new BigDecimal("2800.00"))
                .status(BookingStatus.CONFIRMED)
                .deliveryAddress("Green Valley Farm, Shirur, Pune")
                .notes("Sowing preparation for gram crop")
                .build();
        b2 = bookingRepository.save(b2);

        // Booking 3: Operator Assigned (FR-18)
        Booking b3 = Booking.builder()
                .farmerId(farmer.getId())
                .farm(farm)
                .equipment(harvester)
                .partner(partner)
                .operator(op1)
                .startDate(today.plusDays(2))
                .endDate(today.plusDays(4))
                .totalCost(new BigDecimal("10500.00"))
                .status(BookingStatus.OPERATOR_ASSIGNED)
                .deliveryAddress("Green Valley Farm, Shirur, Pune")
                .notes("Paddy harvesting on 4 acres")
                .build();
        b3 = bookingRepository.save(b3);

        // Booking 4: Completed Booking (FR-19, FR-21)
        Booking b4 = Booking.builder()
                .farmerId(farmer.getId())
                .farm(farm)
                .equipment(rotavator)
                .partner(partner)
                .operator(op2)
                .startDate(today.minusDays(12))
                .endDate(today.minusDays(10))
                .totalCost(new BigDecimal("2400.00"))
                .status(BookingStatus.COMPLETED)
                .deliveryAddress("Green Valley Farm, Shirur, Pune")
                .notes("Soil leveling completed smoothly")
                .build();
        b4 = bookingRepository.save(b4);

        // Booking 5: Completed Booking (FR-19, FR-21)
        Booking b5 = Booking.builder()
                .farmerId(farmer.getId())
                .farm(farm)
                .equipment(tractor1)
                .partner(partner)
                .operator(op3)
                .startDate(today.minusDays(24))
                .endDate(today.minusDays(20))
                .totalCost(new BigDecimal("7200.00"))
                .status(BookingStatus.COMPLETED)
                .deliveryAddress("Green Valley Farm, Shirur, Pune")
                .notes("4 days deep plowing")
                .build();
        b5 = bookingRepository.save(b5);

        // 5b. Seed Operator Job Assignments
        OperatorJobAssignment assign1 = OperatorJobAssignment.builder()
                .operator(op1)
                .booking(b3)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now().minusHours(2))
                .assignedBy(String.valueOf(partner.getId()))
                .notes("Paddy harvesting field operations")
                .build();
        operatorJobAssignmentRepository.save(assign1);

        OperatorJobAssignment assign2 = OperatorJobAssignment.builder()
                .operator(op2)
                .booking(b4)
                .assignmentStatus(OperatorAssignmentStatus.COMPLETED)
                .assignedAt(LocalDateTime.now().minusDays(12))
                .acceptedAt(LocalDateTime.now().minusDays(12).plusMinutes(30))
                .travelingAt(LocalDateTime.now().minusDays(12).plusHours(1))
                .reachedAt(LocalDateTime.now().minusDays(12).plusHours(2))
                .workStartedAt(LocalDateTime.now().minusDays(12).plusHours(3))
                .completedAt(LocalDateTime.now().minusDays(10))
                .assignedBy(String.valueOf(partner.getId()))
                .completionNotes("Rotavator operation completed smoothly.")
                .build();
        operatorJobAssignmentRepository.save(assign2);

        OperatorJobAssignment assign3 = OperatorJobAssignment.builder()
                .operator(op1)
                .booking(b5)
                .assignmentStatus(OperatorAssignmentStatus.COMPLETED)
                .assignedAt(LocalDateTime.now().minusDays(5))
                .acceptedAt(LocalDateTime.now().minusDays(5).plusMinutes(20))
                .travelingAt(LocalDateTime.now().minusDays(5).plusHours(1))
                .reachedAt(LocalDateTime.now().minusDays(5).plusHours(2))
                .workStartedAt(LocalDateTime.now().minusDays(5).plusHours(3)) // 10:00
                .completedAt(LocalDateTime.now().minusDays(5).plusHours(9))   // 16:00 (6 hours total = 360 mins)
                .pausedAt(LocalDateTime.now().minusDays(5).plusHours(5))      // 12:00
                .resumedAt(LocalDateTime.now().minusDays(5).plusHours(6))     // 13:00 (60 mins pause -> 300 mins net = 5.0 hrs)
                .pauseReason("Machine cool-down and lunch interval")
                .assignedBy(String.valueOf(partner.getId()))
                .completionNotes("Plowing job executed with high precision.")
                .build();
        assign3 = operatorJobAssignmentRepository.save(assign3);

        OperatorJobPauseInterval pi1 = OperatorJobPauseInterval.builder()
                .assignment(assign3)
                .operator(op1)
                .pausedAt(LocalDateTime.now().minusDays(5).plusHours(5))
                .resumedAt(LocalDateTime.now().minusDays(5).plusHours(6))
                .pauseReason("Machine cool-down and lunch interval")
                .durationMinutes(60L)
                .build();
        pauseIntervalRepository.save(pi1);

        // 6. Seed Payments (FR-19)
        Payment p1 = Payment.builder()
                .bookingId(b4.getId())
                .farmerId(farmer.getId())
                .partnerId(partner.getId())
                .amount(new BigDecimal("2400.00"))
                .paymentMethod(PaymentMethod.UPI)
                .transactionId("TXN-2026-90412")
                .paymentReference("UPI-REF-90412")
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentDate(LocalDateTime.now().minusDays(10))
                .invoiceReference("INV-2026-00004")
                .build();
        paymentRepository.save(p1);

        Payment p2 = Payment.builder()
                .bookingId(b5.getId())
                .farmerId(farmer.getId())
                .partnerId(partner.getId())
                .amount(new BigDecimal("7200.00"))
                .paymentMethod(PaymentMethod.NET_BANKING)
                .transactionId("TXN-2026-90205")
                .paymentReference("NB-REF-90205")
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentDate(LocalDateTime.now().minusDays(20))
                .invoiceReference("INV-2026-00005")
                .build();
        paymentRepository.save(p2);

        Payment p3 = Payment.builder()
                .bookingId(b3.getId())
                .farmerId(farmer.getId())
                .partnerId(partner.getId())
                .amount(new BigDecimal("10500.00"))
                .paymentMethod(PaymentMethod.UPI)
                .transactionId("TXN-2026-90501")
                .paymentReference("UPI-REF-90501")
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentDate(LocalDateTime.now().minusDays(1))
                .invoiceReference("INV-2026-00003")
                .build();
        paymentRepository.save(p3);

        // 7. Seed Reviews (FR-21)
        Review r1 = Review.builder()
                .bookingId(b4.getId())
                .farmerId(farmer.getId())
                .equipmentId(rotavator.getId())
                .partnerId(partner.getId())
                .rating(5)
                .comment("Excellent machinery condition! Rotavator worked flawlessly on black soil. Highly recommended.")
                .build();
        reviewRepository.save(r1);

        Review r2 = Review.builder()
                .bookingId(b5.getId())
                .farmerId(farmer.getId())
                .equipmentId(tractor1.getId())
                .partnerId(partner.getId())
                .rating(5)
                .comment("Punctual delivery and great support from operator Anil. Will book again for next harvest season.")
                .build();
        reviewRepository.save(r2);

        // 7b. Seed Operator Review for Completed Assignment #3 (Operator 1)
        OperatorReview opReview1 = OperatorReview.builder()
                .assignment(assign3)
                .operator(op1)
                .booking(b5)
                .farmerId(farmer.getId())
                .rating(5)
                .comment("Santosh operated the tractor with exceptional skill and leveled the entire 6-acre field smoothly. Very punctual and respectful.")
                .build();
        operatorReviewRepository.save(opReview1);

        // 8. Seed Notifications for Partner #1 (FR-20)
        Notification n1 = Notification.builder()
                .recipientRole("PARTNER")
                .recipientId(partner.getId())
                .title("New Booking Request")
                .message("New booking request #" + b1.getId() + " received for " + tractor1.getName() + ".")
                .notificationType("BOOKING_CREATED")
                .bookingId(b1.getId())
                .isRead(false)
                .build();
        notificationRepository.save(n1);

        Notification n2 = Notification.builder()
                .recipientRole("PARTNER")
                .recipientId(partner.getId())
                .title("Operator Assigned")
                .message("Operator " + op1.getFullName() + " has been assigned to booking #" + b3.getId() + ".")
                .notificationType("OPERATOR_ASSIGNED")
                .bookingId(b3.getId())
                .isRead(false)
                .build();
        notificationRepository.save(n2);

        Notification n3 = Notification.builder()
                .recipientRole("PARTNER")
                .recipientId(partner.getId())
                .title("Payment Received")
                .message("Payment of ₹10,500 received for booking #" + b3.getId() + " via UPI.")
                .notificationType("PAYMENT_SUCCESS")
                .bookingId(b3.getId())
                .isRead(true)
                .build();
        notificationRepository.save(n3);

        Notification n4 = Notification.builder()
                .recipientRole("PARTNER")
                .recipientId(partner.getId())
                .title("New 5-Star Review Received")
                .message("Farmer " + farmer.getFullName() + " left a 5-star rating for " + rotavator.getName() + ".")
                .notificationType("REVIEW_RECEIVED")
                .bookingId(b4.getId())
                .isRead(true)
                .build();
        notificationRepository.save(n4);

        log.info("DataInitializer completed successfully: Seeded Partner, Farmer, Machinery, Operators, Bookings, Payments, Reviews, and Notifications.");
    }
}
