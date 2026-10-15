package com.agrorental.admin;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.admin.service.AdminBookingService;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.jwt.AccountType;
import com.agrorental.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin booking search against a real JPA context. The unit tests mock the repository, so the search
 * Specification never ran there; it referenced a non-existent Equipment attribute ("modelNumber") and
 * every non-empty search failed with HTTP 500. These tests execute the real query.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:admin_booking_search_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=admin-booking-search-test-secret-32-chars-min",
    "server.port=0"
})
@DisplayName("Admin booking search (real query)")
class AdminBookingSearchIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JwtService jwtService;
    @Autowired private AdminBookingService adminBookingService;
    @Autowired private AdminRepository adminRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private BookingRepository bookingRepository;

    private MockMvc mockMvc;
    private Admin admin;
    private Booking sprayerBooking;
    private Booking tractorBooking;
    private Booking cancelledSprayerBooking;

    @BeforeAll
    void createFixtures() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        admin = adminRepository.save(Admin.builder().fullName("Search Admin").email("search-admin@test.local").password("x").build());

        Partner partner = partnerRepository.save(Partner.builder().fullName("Search Partner").businessName("Kisan Sprayers")
                .mobileNumber("9600077001").email("search-partner@test.local").password("x").otpVerified(true)
                .verificationStatus(Partner.VerificationStatus.APPROVED).build());
        Farmer farmer = farmerRepository.save(Farmer.builder().fullName("Search Farmer").mobileNumber("9800077001")
                .email("search-farmer@test.local").accountStatus("ACTIVE").build());

        Equipment sprayer = equipmentRepository.save(equipment("Field Sprayer", "Aspee", "BS-21X", partner));
        Equipment tractor = equipmentRepository.save(equipment("Field Tractor", "Mahindra", "575 DI", partner));

        sprayerBooking = bookingRepository.save(booking(farmer, partner, sprayer, BookingStatus.CONFIRMED, LocalDate.of(2031, 4, 1), "Plot 7, Shirur"));
        tractorBooking = bookingRepository.save(booking(farmer, partner, tractor, BookingStatus.PENDING, LocalDate.of(2031, 4, 5), "Plot 9, Baramati"));
        cancelledSprayerBooking = bookingRepository.save(booking(farmer, partner, sprayer, BookingStatus.CANCELLED, LocalDate.of(2031, 4, 9), "Plot 7, Shirur"));
    }

    private static Equipment equipment(String name, String brand, String model, Partner partner) {
        return Equipment.builder().name(name).category(EquipmentCategory.SPRAYER).brand(brand).model(model)
                .manufacturingYear(2022).capacity("16 L").rentalPrice(new BigDecimal("1180.50")).fuelType(FuelType.MANUAL_HUMAN_POWERED)
                .description("Search test equipment").locationAddress("Pune").latitude(18.52).longitude(73.85)
                .partner(partner).availabilityStatus(AvailabilityStatus.AVAILABLE).build();
    }

    private static Booking booking(Farmer farmer, Partner partner, Equipment equipment, BookingStatus status,
                                   LocalDate start, String address) {
        return Booking.builder().farmerId(farmer.getId()).partner(partner).equipment(equipment).status(status)
                .startDate(start).endDate(start.plusDays(1)).totalCost(new BigDecimal("2361.00")).deliveryAddress(address).build();
    }

    private List<Long> search(String term, BookingStatus status) {
        return adminBookingService.getBookings(term, status, null, null,
                        PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "id")))
                .getContent().stream().map(BookingResponse::getId).toList();
    }

    @Test
    @DisplayName("Search by equipment model matches only bookings of that equipment (any case, partial)")
    void searchByEquipmentModel() {
        assertThat(search("BS-21X", null)).containsExactly(sprayerBooking.getId(), cancelledSprayerBooking.getId());
        assertThat(search("bs-21x", null)).containsExactly(sprayerBooking.getId(), cancelledSprayerBooking.getId());
        assertThat(search("575 di", null)).containsExactly(tractorBooking.getId());
        assertThat(search("BS-21X", BookingStatus.CANCELLED)).containsExactly(cancelledSprayerBooking.getId());
    }

    @Test
    @DisplayName("The other search fields keep working alongside the model predicate")
    void otherSearchFieldsStillMatch() {
        assertThat(search("field tractor", null)).containsExactly(tractorBooking.getId());          // equipment name
        assertThat(search("kisan sprayers", null))                                                 // partner business name
                .containsExactly(sprayerBooking.getId(), tractorBooking.getId(), cancelledSprayerBooking.getId());
        assertThat(search("baramati", null)).containsExactly(tractorBooking.getId());              // delivery address
        assertThat(search(String.valueOf(tractorBooking.getId()), null)).contains(tractorBooking.getId()); // numeric booking ID
        assertThat(search("no-such-equipment-xyz", null)).isEmpty();
    }

    @Test
    @DisplayName("GET /api/admin/bookings?search=<model> returns 200 with the matching bookings (was HTTP 500)")
    void adminSearchEndpointByModel() throws Exception {
        String authorization = "Bearer " + jwtService.generateToken(admin.getId(), "ADMIN", "0000000000",
                JwtService.accountTypeClaims(AccountType.ADMIN));

        mockMvc.perform(get("/api/admin/bookings").param("search", "BS-21X").param("sort", "id,asc")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].id").value(contains(
                        sprayerBooking.getId().intValue(), cancelledSprayerBooking.getId().intValue())));
    }
}
