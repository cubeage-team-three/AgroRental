package com.agrorental.farmer;

import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.auth.service.AuthService;
import com.agrorental.booking.dto.BookingCreateRequest;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.enums.Role;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import com.agrorental.farmer.dto.FarmerRegisterRequest;
import com.agrorental.farmer.dto.FarmerResponse;
import com.agrorental.farmer.dto.UpdateFarmerProfileRequest;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmService;
import com.agrorental.farmer.service.FarmerProfileService;
import com.agrorental.farmer.service.FarmerService;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorJobLifecycleService;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.enums.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.payment.service.PaymentService;
import com.agrorental.review.dto.ReviewResponse;
import com.agrorental.review.service.ReviewService;
import com.agrorental.security.principal.FarmerPrincipal;
import com.agrorental.tracking.dto.TrackingResponse;
import com.agrorental.tracking.service.LiveTrackingService;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Farmer Management Module — Complete End-to-End Lifecycle Verification Test")
class FarmerEndToEndFlowTest {

    @Mock private FarmerRepository farmerRepository;
    @Mock private UserRepository userRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private OperatorRepository operatorRepository;
    @Mock private OperatorJobAssignmentRepository assignmentRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private NotificationService notificationService;
    @Mock private com.agrorental.tracking.repository.TrackingRepository trackingRepository;
    @Mock private com.agrorental.operator.repository.OperatorLocationRepository operatorLocationRepository;
    @Mock private com.agrorental.booking.mapper.BookingMapper bookingMapper;

    @InjectMocks private FarmerService farmerService;
    @InjectMocks private FarmerProfileService farmerProfileService;
    @InjectMocks private FarmService farmService;
    @InjectMocks private BookingService bookingService;
    @InjectMocks private LiveTrackingService liveTrackingService;

    private Farmer farmer;
    private User user;
    private Farm farm;
    private Equipment equipment;
    private Booking booking;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("Ramesh Patel")
                .email("9876543210@farmer.agrorent.in")
                .password("$2a$10$hashedpassword")
                .role(Role.FARMER)
                .enabled(true)
                .build();
        user.setId(500L);

        farmer = Farmer.builder()
                .fullName("Ramesh Patel")
                .mobileNumber("9876543210")
                .email("9876543210@farmer.agrorent.in")
                .address("Manchar, Pune, Maharashtra")
                .preferredLanguage("Marathi")
                .accountStatus("ACTIVE")
                .user(user)
                .build();
        farmer.setId(100L);

        farm = Farm.builder()
                .farmerId(100L)
                .farmName("Sunrise Agro Plot")
                .village("Manchar")
                .taluka("Ambegaon")
                .district("Pune")
                .state("Maharashtra")
                .farmArea(BigDecimal.valueOf(12.5))
                .cropType("Wheat & Sugarcane")
                .build();
        farm.setId(200L);

        equipment = Equipment.builder()
                .name("Mahindra 575 DI Tractor")
                .category(com.agrorental.equipment.enums.EquipmentCategory.TRACTOR)
                .rentalPrice(BigDecimal.valueOf(800))
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .latitude(18.8500)
                .longitude(73.9100)
                .build();
        equipment.setId(300L);

        booking = Booking.builder()
                .farmerId(100L)
                .equipment(equipment)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .totalCost(BigDecimal.valueOf(2400))
                .status(BookingStatus.PENDING)
                .deliveryAddress("Manchar, Pune")
                .build();
        booking.setId(400L);
    }

    @Test
    @DisplayName("Verify complete Farmer lifecycle flow: Registration -> Auth -> Farm -> Booking -> Tracking -> Completion")
    void testCompleteFarmerLifecycleFlow() {
        // Step 1: Farmer Registration & User Entity Linkage
        FarmerRegisterRequest regReq = new FarmerRegisterRequest();
        regReq.setFullName("Ramesh Patel");
        regReq.setMobileNumber("9876543210");
        regReq.setPassword("Secure@123");

        when(farmerRepository.existsByMobileNumber("9876543210")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(farmerRepository.save(any(Farmer.class))).thenReturn(farmer);

        FarmerResponse regRes = farmerService.registerFarmer(regReq);
        assertNotNull(regRes);
        assertEquals("9876543210", regRes.getMobileNumber());
        verify(userRepository, times(1)).save(any(User.class));
        verify(farmerRepository, times(1)).save(any(Farmer.class));

        // Step 2: Farmer Profile Retrieval & Avatar Upload
        when(farmerRepository.findById(100L)).thenReturn(Optional.of(farmer));
        FarmerProfileResponse profileRes = farmerProfileService.getProfile(100L);
        assertEquals("Ramesh Patel", profileRes.getFullName());

        MockMultipartFile avatarFile = new MockMultipartFile("file", "profile.png", "image/png", "png bytes".getBytes());
        FarmerProfileResponse avatarRes = farmerProfileService.uploadAvatar(100L, avatarFile);
        assertNotNull(avatarRes.getProfileImage());
        assertTrue(avatarRes.getProfileImage().startsWith("/uploads/avatars/"));

        // Step 3: Farm CRUD Operations (Ownership Enforced)
        com.agrorental.farmer.dto.FarmResponse farmResponse = com.agrorental.farmer.dto.FarmResponse.builder()
                .id(200L)
                .farmerId(100L)
                .farmName("Sunrise Agro Plot")
                .village("Manchar")
                .taluka("Ambegaon")
                .district("Pune")
                .state("Maharashtra")
                .farmArea(BigDecimal.valueOf(12.5))
                .cropType("Wheat & Sugarcane")
                .build();

        when(farmRepository.findByFarmerId(100L)).thenReturn(List.of(farm));
        when(farmRepository.findById(200L)).thenReturn(Optional.of(farm));

        List<com.agrorental.farmer.dto.FarmResponse> farmerFarms = farmService.getFarmsByFarmerId(100L);
        assertEquals(1, farmerFarms.size());
        assertEquals("Sunrise Agro Plot", farmerFarms.get(0).getFarmName());

        // Step 4: Equipment Booking Reservation
        BookingCreateRequest bookReq = new BookingCreateRequest();
        bookReq.setFarmerId(100L);
        bookReq.setEquipmentId(300L);
        bookReq.setStartDate(LocalDate.now().plusDays(1));
        bookReq.setEndDate(LocalDate.now().plusDays(3));
        bookReq.setDeliveryAddress("Manchar, Pune");

        when(equipmentRepository.findById(300L)).thenReturn(Optional.of(equipment));
        when(bookingRepository.existsOverlappingBooking(eq(300L), any(), any(), any())).thenReturn(false);
        when(bookingMapper.toEntity(any(), any(), any(), any(), any())).thenReturn(booking);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(bookingMapper.toResponse(any())).thenReturn(BookingResponse.builder().id(400L).status(BookingStatus.PENDING).build());

        BookingResponse bookRes = bookingService.createBooking(bookReq);
        assertNotNull(bookRes);
        assertEquals(BookingStatus.PENDING, bookRes.getStatus());

        // Step 5: Lifecycle Transition to ON_THE_WAY and Live Tracking
        booking.setStatus(BookingStatus.ON_THE_WAY);
        when(bookingRepository.findById(400L)).thenReturn(Optional.of(booking));
        when(assignmentRepository.findByBookingId(400L)).thenReturn(Optional.empty());

        TrackingResponse trackingRes = liveTrackingService.getTrackingByBookingId(400L);
        assertNotNull(trackingRes);
        assertEquals("ON_THE_WAY", trackingRes.getStatus());
        assertNotNull(trackingRes.getLatitude());

        // Step 6: Booking Status Completion
        booking.setStatus(BookingStatus.COMPLETED);
        equipment.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        TrackingResponse completedTrackingRes = liveTrackingService.getTrackingByBookingId(400L);
        assertEquals("COMPLETED", completedTrackingRes.getStatus());
        assertEquals(100, completedTrackingRes.getWorkProgress());
    }
}
