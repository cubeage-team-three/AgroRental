package com.agrorental.booking.workflow;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.partner.entity.Partner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Item 3: booking creation rules. Dates (past, inverted, absurd ranges, the equipment's offered window), farm
 * ownership, equipment state, the server-side total (inclusive days x daily rate; the 18% GST is already
 * included in that total per BIZ-20) and that a client cannot inject totals, status, partner or farmer.
 */
@DisplayName("Booking workflow 3: booking creation rules")
class BookingWorkflowCreationTest extends AbstractBookingWorkflowTest {

    @Test
    @DisplayName("The total is computed server-side (inclusive days x rate); injected totals/status/ids are ignored")
    void serverSideTotalAndNoFieldInjection() throws Exception {
        Partner owner = newPartner();
        Partner otherPartner = newPartner();
        Equipment equipment = newEquipment(owner);
        Farmer farmer = newFarmer();
        String payload = """
                {"equipmentId": %d, "startDate": "%s", "endDate": "%s", "deliveryAddress": "Plot 4",
                 "totalCost": 1.00, "status": "CONFIRMED", "partnerId": %d, "operatorId": 1, "id": 1,
                 "paymentStatus": "SUCCESS", "rejectionReason": "x"}
                """.formatted(equipment.getId(), day(3), day(5), otherPartner.getId());

        MvcResult result = callJson(post("/api/bookings"), tokenFor(farmer), payload);

        assertThat(status(result)).as(body(result)).isEqualTo(201);
        assertThat(new BigDecimal(json(result, "$.data.totalCost").toString())).isEqualByComparingTo("7500.00");
        assertThat((String) json(result, "$.data.status")).isEqualTo("PENDING");
        assertThat((Integer) json(result, "$.data.partnerId")).isEqualTo(owner.getId().intValue());
        assertThat((Integer) json(result, "$.data.farmerId")).isEqualTo(farmer.getId().intValue());
        assertThat((Object) json(result, "$.data.operatorId")).isNull();
        assertThat((Object) json(result, "$.data.paymentStatus")).isNull();

        Booking saved = bookingRepository.findById(((Number) json(result, "$.data.id")).longValue()).orElseThrow();
        assertThat(saved.getTotalCost()).isEqualByComparingTo("7500.00");
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(saved.getPartner().getId()).isEqualTo(owner.getId());
        assertThat(saved.getFarmerId()).isEqualTo(farmer.getId());
        assertThat(saved.getRejectionReason()).isNull();
    }

    @Test
    @DisplayName("A same-day booking costs exactly one day and may start today")
    void sameDayBookingCostsOneDay() throws Exception {
        Equipment equipment = newEquipment(newPartner());

        MvcResult result = postBooking(newFarmer(), equipment.getId(), day(0), day(0));

        assertThat(status(result)).as(body(result)).isEqualTo(201);
        assertThat(new BigDecimal(json(result, "$.data.totalCost").toString())).isEqualByComparingTo(DAILY_RATE);
    }

    @Test
    @DisplayName("Past start or end dates and an end date before the start date are rejected")
    void pastAndInvertedDatesAreRejected() throws Exception {
        Equipment equipment = newEquipment(newPartner());
        Farmer farmer = newFarmer();

        MvcResult pastStart = postBooking(farmer, equipment.getId(), day(-1), day(2));
        assertThat(status(pastStart)).isEqualTo(400);
        assertThat((String) json(pastStart, "$.data.startDate")).contains("today or in the future");

        MvcResult pastEnd = postBooking(farmer, equipment.getId(), day(-3), day(-1));
        assertThat(status(pastEnd)).isEqualTo(400);

        MvcResult inverted = postBooking(farmer, equipment.getId(), day(6), day(4));
        assertThat(status(inverted)).isEqualTo(400);
        assertThat((String) json(inverted, "$.message")).isEqualTo("End date cannot be before start date");

        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).isEmpty();
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    @DisplayName("An absurdly long range (a decade) is rejected instead of blocking the machine for years")
    void absurdRangeIsRejected() throws Exception {
        Equipment equipment = newEquipment(newPartner());

        MvcResult decade = postBooking(newFarmer(), equipment.getId(), day(1), day(1).plusYears(10));

        assertThat(status(decade)).as(body(decade)).isEqualTo(400);
        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).isEmpty();
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    @DisplayName("A total that does not fit the booking amount column is a clear 400, not a database error")
    void unstorableTotalIsRejectedCleanly() throws Exception {
        Equipment equipment = newEquipment(newPartner(), b -> b.rentalPrice(new BigDecimal("99999999.99")));

        MvcResult result = postBooking(newFarmer(), equipment.getId(), day(1), day(200));

        assertThat(status(result)).as(body(result)).isEqualTo(400);
        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).isEmpty();
    }

    @Test
    @DisplayName("Dates outside the partner's offered window (availableFrom/To) cannot be booked")
    void bookingOutsideOfferedWindowIsRejected() throws Exception {
        Equipment equipment = newEquipment(newPartner(), b -> b.availableFromDate(day(10)).availableToDate(day(20)));
        Farmer farmer = newFarmer();

        assertThat(status(postBooking(farmer, equipment.getId(), day(5), day(12)))).isEqualTo(400);
        assertThat(status(postBooking(farmer, equipment.getId(), day(18), day(25)))).isEqualTo(400);
        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).isEmpty();

        MvcResult inside = postBooking(farmer, equipment.getId(), day(10), day(20));
        assertThat(status(inside)).as(body(inside)).isEqualTo(201);
    }

    @Test
    @DisplayName("Farm ownership: own farm fills the address; another farmer's farm is refused; unknown farm is 404")
    void farmOwnershipIsEnforced() throws Exception {
        Equipment equipment = newEquipment(newPartner());
        Farmer farmer = newFarmer();
        Farmer neighbour = newFarmer();
        Farm own = newFarm(farmer);
        Farm foreign = newFarm(neighbour);

        String foreignFarm = """
                {"equipmentId": %d, "farmId": %d, "startDate": "%s", "endDate": "%s"}
                """.formatted(equipment.getId(), foreign.getId(), day(3), day(4));
        MvcResult refused = callJson(post("/api/bookings"), tokenFor(farmer), foreignFarm);
        assertThat(status(refused)).isEqualTo(400);
        assertThat((String) json(refused, "$.message")).contains("not authorized to use this farm");

        String unknownFarm = """
                {"equipmentId": %d, "farmId": 999999999, "startDate": "%s", "endDate": "%s"}
                """.formatted(equipment.getId(), day(3), day(4));
        assertThat(status(callJson(post("/api/bookings"), tokenFor(farmer), unknownFarm))).isEqualTo(404);
        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).isEmpty();

        String ownFarm = """
                {"equipmentId": %d, "farmId": %d, "startDate": "%s", "endDate": "%s"}
                """.formatted(equipment.getId(), own.getId(), day(3), day(4));
        MvcResult created = callJson(post("/api/bookings"), tokenFor(farmer), ownFarm);
        assertThat(status(created)).as(body(created)).isEqualTo(201);
        assertThat((Integer) json(created, "$.data.farmId")).isEqualTo(own.getId().intValue());
        assertThat((String) json(created, "$.data.deliveryAddress")).startsWith(own.getFarmName());
    }

    @Test
    @DisplayName("A farm with maximum-length names still yields a storable delivery address")
    void longFarmNamesStillProduceAValidAddress() throws Exception {
        Equipment equipment = newEquipment(newPartner());
        Farmer farmer = newFarmer();
        Farm farm = farmRepository.save(Farm.builder().farmerId(farmer.getId()).farmName("F".repeat(150))
                .village("V".repeat(100)).taluka("T".repeat(100)).district("D".repeat(100)).state("Maharashtra")
                .farmArea(new BigDecimal("2.00")).build());
        String payload = """
                {"equipmentId": %d, "farmId": %d, "startDate": "%s", "endDate": "%s"}
                """.formatted(equipment.getId(), farm.getId(), day(3), day(4));

        MvcResult result = callJson(post("/api/bookings"), tokenFor(farmer), payload);

        assertThat(status(result)).as(body(result)).isEqualTo(201);
        assertThat(((String) json(result, "$.data.deliveryAddress")).length()).isLessThanOrEqualTo(255);
    }

    @Test
    @DisplayName("Over-long delivery address or notes are validation errors (400), not database conflicts")
    void overlongTextFieldsAreValidationErrors() throws Exception {
        Equipment equipment = newEquipment(newPartner());
        Farmer farmer = newFarmer();

        String longAddress = """
                {"equipmentId": %d, "startDate": "%s", "endDate": "%s", "deliveryAddress": "%s"}
                """.formatted(equipment.getId(), day(3), day(4), "A".repeat(256));
        MvcResult address = callJson(post("/api/bookings"), tokenFor(farmer), longAddress);
        assertThat(status(address)).as(body(address)).isEqualTo(400);
        assertThat((String) json(address, "$.data.deliveryAddress")).isNotBlank();

        String longNotes = """
                {"equipmentId": %d, "startDate": "%s", "endDate": "%s", "notes": "%s"}
                """.formatted(equipment.getId(), day(3), day(4), "N".repeat(5001));
        MvcResult notes = callJson(post("/api/bookings"), tokenFor(farmer), longNotes);
        assertThat(status(notes)).as(body(notes)).isEqualTo(400);

        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).isEmpty();
    }

    @Test
    @DisplayName("Unknown equipment is 404; booking for another farmer is 403; non-farmers cannot book")
    void identityAndExistenceRules() throws Exception {
        Equipment equipment = newEquipment(newPartner());
        Farmer farmer = newFarmer();
        Farmer victim = newFarmer();

        assertThat(status(postBooking(farmer, 999999999L, day(3), day(4)))).isEqualTo(404);

        String asVictim = """
                {"equipmentId": %d, "farmerId": %d, "startDate": "%s", "endDate": "%s"}
                """.formatted(equipment.getId(), victim.getId(), day(3), day(4));
        assertThat(status(callJson(post("/api/bookings"), tokenFor(farmer), asVictim))).isEqualTo(403);
        assertThat(status(callJson(post("/api/farmers/bookings"), tokenFor(farmer), asVictim))).isEqualTo(403);

        String body = bookingJson(equipment.getId(), day(3), day(4));
        assertThat(status(callJson(post("/api/bookings"), tokenFor(equipment.getPartner()), body))).isEqualTo(403);
        assertThat(status(callJson(post("/api/bookings"), tokenFor(newOperator()), body))).isEqualTo(403);
        assertThat(status(callJson(post("/api/bookings"), tokenFor(newAdmin()), body))).isEqualTo(403);
        assertThat(status(callJson(post("/api/bookings"), null, body))).isIn(401, 403);
        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BookingStatus.class)
    @DisplayName("BIZ-13: a new booking may not overlap any reserving booking; finished ones do not block")
    void overlapRuleCoversEveryReservingStatus(BookingStatus existing) throws Exception {
        Equipment equipment = newEquipment(newPartner());
        insertBooking(newFarmer(), equipment, existing, day(10), day(12));
        boolean reserving = switch (existing) {
            case PENDING, ACCEPTED, CONFIRMED, OPERATOR_ASSIGNED, ON_THE_WAY, WORK_STARTED -> true;
            case COMPLETED, CANCELLED, REJECTED -> false;
        };

        // Touches the existing range only on its last day
        MvcResult result = postBooking(newFarmer(), equipment.getId(), day(12), day(14));

        assertThat(status(result)).as(body(result)).isEqualTo(reserving ? 400 : 201);
        // Adjacent ranges never conflict
        assertThat(status(postBooking(newFarmer(), equipment.getId(), day(15), day(16)))).isEqualTo(201);
    }

    @Test
    @DisplayName("The farmer route /api/farmers/bookings applies the same server-side rules")
    void farmerRouteAppliesTheSameRules() throws Exception {
        Equipment equipment = newEquipment(newPartner());
        Farmer farmer = newFarmer();

        MvcResult created = callJson(post("/api/farmers/bookings"), tokenFor(farmer), bookingJson(equipment.getId(), day(2), day(3)));
        assertThat(status(created)).as(body(created)).isEqualTo(201);
        assertThat(new BigDecimal(json(created, "$.data.totalCost").toString())).isEqualByComparingTo("5000.00");

        MvcResult overlap = callJson(post("/api/farmers/bookings"), tokenFor(newFarmer()), bookingJson(equipment.getId(), day(3), day(3)));
        assertThat(status(overlap)).isEqualTo(400);
        MvcResult past = callJson(post("/api/farmers/bookings"), tokenFor(farmer), bookingJson(equipment.getId(), day(-2), day(1)));
        assertThat(status(past)).isEqualTo(400);
        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).hasSize(1);
    }
}
