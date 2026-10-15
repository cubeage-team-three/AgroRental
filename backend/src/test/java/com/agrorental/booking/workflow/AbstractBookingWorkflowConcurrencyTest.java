package com.agrorental.booking.workflow;

import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.partner.entity.Partner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Item 4 (concurrency) and item 2 (availability under concurrency): duplicate and concurrent accept / reject /
 * cancel requests and concurrent transitions on different bookings of one machine, through the real HTTP stack
 * with real threads released together. Runs on H2 ({@link BookingWorkflowConcurrencyTest}) and, opt-in, on a
 * disposable MySQL 8 ({@link BookingWorkflowConcurrencyMySQLTest}) where InnoDB row locks are exercised.
 */
abstract class AbstractBookingWorkflowConcurrencyTest extends AbstractBookingWorkflowTest {

    private static final int DUPLICATES = 4;

    private static long count(List<Integer> statuses, int status) {
        return statuses.stream().filter(s -> s == status).count();
    }

    @Test
    void duplicateConcurrentAcceptsConfirmExactlyOnce() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Long id = createBooking(farmer, newEquipment(partner), day(3), day(4));

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < DUPLICATES; i++) {
            tasks.add(() -> accept(partner, id));
        }
        List<Integer> statuses = statuses(runConcurrently(tasks));

        assertThat(count(statuses, 200)).as("statuses %s", statuses).isEqualTo(1);
        assertThat(count(statuses, 400)).as("statuses %s", statuses).isEqualTo(DUPLICATES - 1);
        assertThat(statusOf(id)).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(notificationTypesFor("FARMER", farmer.getId(), id)).containsExactly("BOOKING_CONFIRMED");
    }

    @Test
    void duplicateConcurrentRejectsDeclineExactlyOnce() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Equipment equipment = newEquipment(partner);
        Long id = createBooking(farmer, equipment, day(3), day(4));

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < DUPLICATES; i++) {
            tasks.add(() -> reject(partner, id, "Machine unavailable"));
        }
        List<Integer> statuses = statuses(runConcurrently(tasks));

        assertThat(count(statuses, 200)).as("statuses %s", statuses).isEqualTo(1);
        assertThat(count(statuses, 400)).as("statuses %s", statuses).isEqualTo(DUPLICATES - 1);
        assertThat(statusOf(id)).isEqualTo(BookingStatus.REJECTED);
        assertThat(notificationTypesFor("FARMER", farmer.getId(), id)).containsExactly("BOOKING_REJECTED");
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    void duplicateConcurrentCancelsCancelExactlyOnce() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Equipment equipment = newEquipment(partner);
        Long id = createBooking(farmer, equipment, day(3), day(4));

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < DUPLICATES; i++) {
            tasks.add(() -> farmerCancel(farmer, id));
        }
        List<Integer> statuses = statuses(runConcurrently(tasks));

        assertThat(count(statuses, 200)).as("statuses %s", statuses).isEqualTo(1);
        assertThat(count(statuses, 400)).as("statuses %s", statuses).isEqualTo(DUPLICATES - 1);
        assertThat(statusOf(id)).isEqualTo(BookingStatus.CANCELLED);
        assertThat(notificationTypesFor("FARMER", farmer.getId(), id)).containsExactly("BOOKING_CANCELLED");
        assertThat(notificationTypesFor("PARTNER", partner.getId(), id)).containsExactlyInAnyOrder("BOOKING_CANCELLED", "BOOKING_CREATED");
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    void concurrentAcceptAndCancelOfOneBookingEndCancelledAndReleaseTheMachine() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Equipment equipment = newEquipment(partner);
        Long id = createBooking(farmer, equipment, day(3), day(4));

        List<Object> results = runConcurrently(List.of(() -> accept(partner, id), () -> farmerCancel(farmer, id)));
        List<Integer> statuses = statuses(results);

        assertThat(statuses.get(1)).as("the farmer's cancellation always wins or follows: %s", statuses).isEqualTo(200);
        assertThat(statuses.get(0)).as("accept: %s", statuses).isIn(200, 400);
        assertThat(statusOf(id)).isEqualTo(BookingStatus.CANCELLED);
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    void concurrentCancellationsOfDifferentBookingsOfOneMachineAllSucceedAndReleaseIt() throws Exception {
        for (int round = 0; round < 3; round++) {
            Partner partner = newPartner();
            Equipment equipment = newEquipment(partner);
            List<Farmer> farmers = new ArrayList<>();
            List<Long> bookings = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Farmer farmer = newFarmer();
                farmers.add(farmer);
                bookings.add(createBooking(farmer, equipment, day(10 + 5 * i), day(11 + 5 * i)));
            }

            List<Callable<Object>> tasks = new ArrayList<>();
            for (int i = 0; i < farmers.size(); i++) {
                Farmer farmer = farmers.get(i);
                Long booking = bookings.get(i);
                tasks.add(() -> farmerCancel(farmer, booking));
            }
            List<Integer> statuses = statuses(runConcurrently(tasks));

            assertThat(statuses).as("round %d: every farmer's own cancellation must succeed", round)
                    .containsOnly(200);
            assertThat(bookings).allSatisfy(b -> assertThat(statusOf(b)).isEqualTo(BookingStatus.CANCELLED));
            assertThat(availabilityOf(equipment)).as("round %d", round).isEqualTo(AvailabilityStatus.AVAILABLE);
        }
    }

    @Test
    void cancellationRacingANewBookingForOtherDatesLeavesTheMachineBooked() throws Exception {
        for (int round = 0; round < 5; round++) {
            Partner partner = newPartner();
            Equipment equipment = newEquipment(partner);
            Farmer leaving = newFarmer();
            Farmer arriving = newFarmer();
            Long existing = createBooking(leaving, equipment, day(10), day(11));

            List<Object> results = runConcurrently(List.of(
                    () -> farmerCancel(leaving, existing),
                    () -> postBooking(arriving, equipment.getId(), day(20), day(21))));
            List<Integer> statuses = statuses(results);

            assertThat(statuses).as("round %d", round).containsExactly(200, 201);
            assertThat(statusOf(existing)).isEqualTo(BookingStatus.CANCELLED);
            assertThat(availabilityOf(equipment)).as("round %d: the new pending booking holds the machine", round)
                    .isEqualTo(AvailabilityStatus.BOOKED);
        }
    }

    @Test
    void concurrentOverlappingCreationsThroughTheApiReserveOnce() throws Exception {
        Equipment equipment = newEquipment(newPartner());
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < DUPLICATES; i++) {
            Farmer farmer = newFarmer();
            tasks.add(() -> callJson(post("/api/bookings"), tokenFor(farmer), bookingJson(equipment.getId(), day(5), day(7))));
        }
        List<Integer> statuses = statuses(runConcurrently(tasks));

        assertThat(count(statuses, 201)).as("statuses %s", statuses).isEqualTo(1);
        assertThat(count(statuses, 400)).as("statuses %s", statuses).isEqualTo(DUPLICATES - 1);
        assertThat(bookingRepository.findByEquipmentId(equipment.getId())).hasSize(1);
        assertThat(availabilityOf(equipment)).isEqualTo(AvailabilityStatus.BOOKED);
    }
}
