package com.agrorental.booking.workflow;

import com.agrorental.admin.entity.Admin;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.partner.entity.Partner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Item 2: equipment availability semantics. Policy under test (see REPORT.md):
 * <ul>
 *   <li>UNDER_MAINTENANCE and INACTIVE are partner/admin-controlled and are never changed by booking events;</li>
 *   <li>otherwise the flag is derived from the bookings table: BOOKED while at least one reserving booking
 *       (PENDING, ACCEPTED, CONFIRMED, OPERATOR_ASSIGNED, ON_THE_WAY, WORK_STARTED) exists, AVAILABLE after the
 *       last one ends;</li>
 *   <li>BOOKED never means "unavailable on every date": date ranges decide (BIZ-13 overlap check), so BOOKED
 *       equipment stays discoverable and bookable for free dates.</li>
 * </ul>
 */
@DisplayName("Booking workflow 2: equipment availability semantics")
class BookingWorkflowAvailabilityTest extends AbstractBookingWorkflowTest {

    private List<Integer> discoverableIds() throws Exception {
        MvcResult page = call(get("/api/equipment/available/page").param("size", "100").param("sort", "id,desc"), null);
        assertThat(status(page)).isEqualTo(200);
        return json(page, "$.data.content[*].id");
    }

    private List<Integer> searchIds(Equipment equipment, LocalDate start, LocalDate end) throws Exception {
        String village = equipment.getLocationAddress().split(",")[0];
        MvcResult page = call(get("/api/equipment/search/page").param("locationAddress", village)
                .param("startDate", start.toString()).param("endDate", end.toString()).param("size", "100"), null);
        assertThat(status(page)).isEqualTo(200);
        return json(page, "$.data.content[*].id");
    }

    @Test
    @DisplayName("A booking for some dates keeps the machine discoverable and bookable for other dates")
    void bookedMachineStaysDiscoverableAndBookableForFreeDates() throws Exception {
        Partner partner = newPartner();
        Equipment equipment = newEquipment(partner);
        createBooking(newFarmer(), equipment, day(10), day(12));

        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.BOOKED);
        assertThat(discoverableIds()).as("discovery must not hide a machine that is free on other dates")
                .contains(equipment.getId().intValue());
        assertThat(searchIds(equipment, day(20), day(22))).contains(equipment.getId().intValue());
        assertThat(searchIds(equipment, day(11), day(11))).doesNotContain(equipment.getId().intValue());

        Long second = createBooking(newFarmer(), equipment, day(20), day(22));
        assertThat(statusOf(second)).isEqualTo(BookingStatus.PENDING);
        assertThat(status(postBooking(newFarmer(), equipment.getId(), day(12), day(13))))
                .as("BIZ-13: overlapping dates stay protected").isEqualTo(400);
    }

    @Test
    @DisplayName("Rejecting one of two bookings keeps the machine BOOKED; rejecting the last one frees it")
    void rejectingOneOfTwoBookingsKeepsTheMachineBooked() throws Exception {
        Partner partner = newPartner();
        Equipment equipment = newEquipment(partner);
        Long first = createBooking(newFarmer(), equipment, day(10), day(11));
        Long second = createBooking(newFarmer(), equipment, day(20), day(21));

        assertThat(status(reject(partner, first, "Machine needs servicing that week"))).isEqualTo(200);
        assertThat(availabilityOf(equipment)).as("booking #%d is still pending", second).isEqualTo(AvailabilityStatus.BOOKED);

        assertThat(status(reject(partner, second, "Not available"))).isEqualTo(200);
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    @DisplayName("A farmer cancelling one of two bookings keeps the machine BOOKED; cancelling the last frees it")
    void cancellingOneOfTwoBookingsKeepsTheMachineBooked() throws Exception {
        Partner partner = newPartner();
        Equipment equipment = newEquipment(partner);
        Farmer farmerA = newFarmer();
        Farmer farmerB = newFarmer();
        Long first = createBooking(farmerA, equipment, day(10), day(11));
        Long second = createBooking(farmerB, equipment, day(20), day(21));
        assertThat(status(accept(partner, second))).isEqualTo(200);

        assertThat(status(farmerCancel(farmerA, first))).isEqualTo(200);
        assertThat(availabilityOf(equipment)).as("confirmed booking #%d still holds the machine", second)
                .isEqualTo(AvailabilityStatus.BOOKED);

        assertThat(status(farmerCancel(farmerB, second))).isEqualTo(200);
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Admin completion or cancellation of one booking keeps the machine BOOKED while another is active")
    void adminOverridesRespectOtherActiveBookings() throws Exception {
        Partner partner = newPartner();
        Admin admin = newAdmin();
        Equipment equipment = newEquipment(partner);
        Long first = createBooking(newFarmer(), equipment, day(10), day(11));
        Long second = createBooking(newFarmer(), equipment, day(20), day(21));
        Long third = createBooking(newFarmer(), equipment, day(30), day(31));

        assertThat(status(adminStatus(admin, first, BookingStatus.COMPLETED))).isEqualTo(200);
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.BOOKED);
        assertThat(status(adminCancel(admin, second))).isEqualTo(200);
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.BOOKED);
        assertThat(status(adminStatus(admin, third, BookingStatus.CANCELLED))).isEqualTo(200);
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Booking events never override UNDER_MAINTENANCE or INACTIVE set by the partner")
    void bookingEventsNeverOverrideMaintenanceOrInactive() throws Exception {
        Partner partner = newPartner();
        Admin admin = newAdmin();
        Farmer farmer = newFarmer();

        Equipment maintained = newEquipment(partner);
        Long cancelled = createBooking(farmer, maintained, day(10), day(11));
        maintained = equipmentRepository.findById(maintained.getId()).orElseThrow();
        maintained.setAvailabilityStatus(AvailabilityStatus.UNDER_MAINTENANCE);
        equipmentRepository.save(maintained);
        assertThat(status(farmerCancel(farmer, cancelled))).isEqualTo(200);
        assertThat(availabilityOf(maintained)).as("a cancellation must not put a machine under maintenance back in service")
                .isEqualTo(AvailabilityStatus.UNDER_MAINTENANCE);

        Equipment retired = newEquipment(partner);
        Long confirmed = createBooking(farmer, retired, day(10), day(11));
        Long rejected = createBooking(newFarmer(), retired, day(20), day(21));
        retired = equipmentRepository.findById(retired.getId()).orElseThrow();
        retired.setAvailabilityStatus(AvailabilityStatus.INACTIVE);
        equipmentRepository.save(retired);
        assertThat(status(adminStatus(admin, confirmed, BookingStatus.CONFIRMED))).isEqualTo(200);
        assertThat(availabilityOf(retired)).isEqualTo(AvailabilityStatus.INACTIVE);
        assertThat(status(reject(partner, rejected, "Machine retired"))).isEqualTo(200);
        assertThat(availabilityOf(retired)).isEqualTo(AvailabilityStatus.INACTIVE);
        assertThat(status(adminStatus(admin, confirmed, BookingStatus.COMPLETED))).isEqualTo(200);
        assertThat(availabilityOf(retired)).as("completion must not reactivate an INACTIVE machine")
                .isEqualTo(AvailabilityStatus.INACTIVE);
    }

    @Test
    @DisplayName("The partner can edit a BOOKED machine's details while the UI echoes the BOOKED status back")
    void partnerCanEditBookedMachine() throws Exception {
        Partner partner = newPartner();
        Equipment equipment = newEquipment(partner);
        createBooking(newFarmer(), equipment, day(10), day(11));
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.BOOKED);

        MvcResult result = callJson(put("/api/equipment/" + equipment.getId()), tokenFor(partner),
                BookingWorkflowEquipmentTest.updateJson(equipment, "Tractor with new tyres", "2600.00",
                        AvailabilityStatus.BOOKED, false));

        assertThat(status(result)).as(body(result)).isEqualTo(200);
        Equipment saved = equipmentRepository.findById(equipment.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Tractor with new tyres");
        assertThat(saved.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.BOOKED);
    }

    @Test
    @DisplayName("A partner cannot manually mark a machine without bookings as BOOKED")
    void partnerCannotMarkMachineBooked() throws Exception {
        Partner partner = newPartner();
        Equipment equipment = newEquipment(partner);

        MvcResult result = callJson(put("/api/equipment/" + equipment.getId()), tokenFor(partner),
                BookingWorkflowEquipmentTest.updateJson(equipment, "Name", "2500.00", AvailabilityStatus.BOOKED, false));

        assertThat(status(result)).isEqualTo(400);
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Setting AVAILABLE while bookings are active keeps BOOKED; maintenance and back re-derives it")
    void partnerStatusChangesAreReconciledWithBookings() throws Exception {
        Partner partner = newPartner();
        Equipment equipment = newEquipment(partner);
        createBooking(newFarmer(), equipment, day(10), day(11));

        MvcResult maintenance = callJson(put("/api/equipment/" + equipment.getId()), tokenFor(partner),
                BookingWorkflowEquipmentTest.updateJson(equipment, "Name", "2500.00", AvailabilityStatus.UNDER_MAINTENANCE, false));
        assertThat(status(maintenance)).isEqualTo(200);
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.UNDER_MAINTENANCE);
        assertThat(status(postBooking(newFarmer(), equipment.getId(), day(30), day(31)))).isEqualTo(400);

        MvcResult available = callJson(put("/api/equipment/" + equipment.getId()), tokenFor(partner),
                BookingWorkflowEquipmentTest.updateJson(equipment, "Name", "2500.00", AvailabilityStatus.AVAILABLE, false));
        assertThat(status(available)).isEqualTo(200);
        assertThat(availabilityOf(equipment)).as("the pending booking still holds the machine")
                .isEqualTo(AvailabilityStatus.BOOKED);
        assertThat((String) json(available, "$.data.availabilityStatus")).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("An inverted search date range is a 400, not a silently unfiltered result")
    void invertedSearchRangeIsRejected() throws Exception {
        MvcResult result = call(get("/api/equipment/search/page").param("startDate", day(10).toString())
                .param("endDate", day(5).toString()), null);

        assertThat(status(result)).isEqualTo(400);
        assertThat((String) json(result, "$.message")).isEqualTo("End date cannot be before start date");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BookingStatus.class)
    @DisplayName("Date-range search excludes a machine exactly when a reserving booking overlaps the range")
    void dateRangeSearchMatchesTheReservationRule(BookingStatus existing) throws Exception {
        Partner partner = newPartner();
        Equipment equipment = newEquipment(partner);
        insertBooking(newFarmer(), equipment, existing, day(10), day(12));
        boolean reserving = switch (existing) {
            case PENDING, ACCEPTED, CONFIRMED, OPERATOR_ASSIGNED, ON_THE_WAY, WORK_STARTED -> true;
            case COMPLETED, CANCELLED, REJECTED -> false;
        };

        List<Integer> ids = searchIds(equipment, day(11), day(11));
        int bookingStatus = status(postBooking(newFarmer(), equipment.getId(), day(11), day(11)));

        if (reserving) {
            assertThat(ids).as("search must hide a machine that cannot be booked for these dates")
                    .doesNotContain(equipment.getId().intValue());
            assertThat(bookingStatus).isEqualTo(400);
        } else {
            assertThat(ids).contains(equipment.getId().intValue());
            assertThat(bookingStatus).isEqualTo(201);
        }
    }
}
