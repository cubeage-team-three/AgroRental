package com.agrorental.booking.workflow;

import com.agrorental.admin.entity.Admin;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.operator.entity.Operator;
import com.agrorental.partner.entity.Partner;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentRefund;
import com.agrorental.payment.entity.RefundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

/**
 * Item 4: partner visibility, accept/reject ownership, BIZ-14 transition rules, farmer cancellation (incl. the
 * BIZ-20 refund initiation) and admin overrides, all through the real HTTP stack with signed tokens.
 */
@DisplayName("Booking workflow 4: visibility, transitions, cancellation and admin overrides")
class BookingWorkflowTransitionsTest extends AbstractBookingWorkflowTest {

    @Test
    @DisplayName("GET /api/bookings/partner/{id} lists only that partner's bookings; other roles and partners are denied")
    void partnerBookingListIsScoped() throws Exception {
        Partner partnerA = newPartner();
        Partner partnerB = newPartner();
        Farmer farmer = newFarmer();
        Long bookingA = createBooking(farmer, newEquipment(partnerA), day(3), day(4));
        Long bookingB = createBooking(farmer, newEquipment(partnerB), day(3), day(4));

        MvcResult own = call(get("/api/bookings/partner/" + partnerA.getId()), tokenFor(partnerA));
        assertThat(status(own)).isEqualTo(200);
        List<Integer> ids = json(own, "$.data[*].id");
        List<Integer> partnerIds = json(own, "$.data[*].partnerId");
        assertThat(ids).contains(bookingA.intValue()).doesNotContain(bookingB.intValue());
        assertThat(partnerIds).containsOnly(partnerA.getId().intValue());

        String url = "/api/bookings/partner/" + partnerA.getId();
        MvcResult otherPartner = call(get(url), tokenFor(partnerB));
        assertThat(status(otherPartner)).isEqualTo(403);
        assertThat(body(otherPartner)).doesNotContain("\"id\":" + bookingA);
        assertThat(status(call(get(url), tokenFor(farmer)))).isEqualTo(403);
        assertThat(status(call(get(url), tokenFor(newOperator())))).isEqualTo(403);
        assertThat(status(call(get(url), null))).isIn(401, 403);
        assertThat(status(call(get(url), tokenFor(newAdmin())))).isEqualTo(200);
    }

    @Test
    @DisplayName("Booking detail is visible only to its farmer, its partner, its assigned operator and admins")
    void bookingDetailVisibility() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Operator assigned = newOperator();
        Long id = createBooking(farmer, newEquipment(partner), day(3), day(4));
        Booking booking = bookingRepository.findById(id).orElseThrow();
        booking.setOperator(assigned);
        bookingRepository.save(booking);
        String url = "/api/bookings/" + id;

        assertThat(status(call(get(url), tokenFor(farmer)))).isEqualTo(200);
        assertThat(status(call(get(url), tokenFor(partner)))).isEqualTo(200);
        assertThat(status(call(get(url), tokenFor(assigned)))).isEqualTo(200);
        assertThat(status(call(get(url), tokenFor(newAdmin())))).isEqualTo(200);

        MvcResult otherFarmer = call(get(url), tokenFor(newFarmer()));
        assertThat(status(otherFarmer)).isEqualTo(403);
        assertThat(body(otherFarmer)).doesNotContain(farmer.getMobileNumber());
        assertThat(status(call(get(url), tokenFor(newPartner())))).isEqualTo(403);
        assertThat(status(call(get(url), tokenFor(newOperator())))).isEqualTo(403);
        assertThat(status(call(get("/api/farmers/bookings/" + id), tokenFor(newFarmer())))).isEqualTo(403);
    }

    @Test
    @DisplayName("Only the owning partner can accept or reject; another partner changes nothing")
    void acceptAndRejectAreOwnerOnly() throws Exception {
        Partner owner = newPartner();
        Partner intruder = newPartner();
        Long id = createBooking(newFarmer(), newEquipment(owner), day(3), day(4));

        assertThat(status(accept(intruder, id))).isIn(400, 403);
        assertThat(status(reject(intruder, id, "Not mine"))).isIn(400, 403);
        assertThat(statusOf(id)).isEqualTo(BookingStatus.PENDING);
        assertThat(bookingRepository.findById(id).orElseThrow().getRejectionReason()).isNull();

        // Farmers and operators cannot use the partner actions at all
        assertThat(status(call(patch("/api/bookings/" + id + "/accept"), tokenFor(newFarmer())))).isEqualTo(403);
        assertThat(status(call(patch("/api/bookings/" + id + "/accept"), tokenFor(newOperator())))).isEqualTo(403);
        assertThat(statusOf(id)).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    @DisplayName("Accept moves PENDING to CONFIRMED; a repeated accept is refused and changes nothing")
    void acceptConfirmsOnce() throws Exception {
        Partner partner = newPartner();
        Long id = createBooking(newFarmer(), newEquipment(partner), day(3), day(4));

        MvcResult first = accept(partner, id);
        assertThat(status(first)).isEqualTo(200);
        assertThat((String) json(first, "$.data.status")).isEqualTo("CONFIRMED");

        MvcResult repeat = accept(partner, id);
        assertThat(status(repeat)).isEqualTo(400);
        assertThat(statusOf(id)).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("BIZ-14: a booking an admin moved to ACCEPTED can still be accepted (confirmed) by its partner")
    void acceptedBookingCanBeConfirmed() throws Exception {
        Partner partner = newPartner();
        Long id = createBooking(newFarmer(), newEquipment(partner), day(3), day(4));
        assertThat(status(adminStatus(newAdmin(), id, BookingStatus.ACCEPTED))).isEqualTo(200);

        MvcResult result = accept(partner, id);

        assertThat(status(result)).as(body(result)).isEqualTo(200);
        assertThat(statusOf(id)).isEqualTo(BookingStatus.CONFIRMED);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = BookingStatus.class, names = {"CONFIRMED", "OPERATOR_ASSIGNED", "ON_THE_WAY", "WORK_STARTED",
            "COMPLETED", "CANCELLED", "REJECTED"})
    @DisplayName("BIZ-14: accept is refused from every later or terminal status and nothing changes")
    void acceptRefusedFromLaterStatuses(BookingStatus current) throws Exception {
        Partner partner = newPartner();
        Booking booking = insertBooking(newFarmer(), newEquipment(partner), current, day(3), day(4));

        assertThat(status(accept(partner, booking.getId()))).isEqualTo(400);
        assertThat(statusOf(booking.getId())).isEqualTo(current);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = BookingStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("BIZ-14: reject is refused from every non-PENDING status and nothing changes")
    void rejectRefusedFromNonPendingStatuses(BookingStatus current) throws Exception {
        Partner partner = newPartner();
        Booking booking = insertBooking(newFarmer(), newEquipment(partner), current, day(3), day(4));

        assertThat(status(reject(partner, booking.getId(), "Too late"))).isEqualTo(400);
        Booking after = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(current);
        assertThat(after.getRejectionReason()).isNull();
    }

    @Test
    @DisplayName("Reject requires a reason; a long reason is accepted and stored in full")
    void rejectionReasonRules() throws Exception {
        Partner partner = newPartner();
        Long id = createBooking(newFarmer(), newEquipment(partner), day(3), day(4));

        assertThat(status(reject(partner, id, null))).isEqualTo(400);
        assertThat(status(reject(partner, id, "   "))).isEqualTo(400);
        assertThat(statusOf(id)).isEqualTo(BookingStatus.PENDING);

        String longReason = "The machine is booked for a government drainage project in that week. ".repeat(9).trim();
        assertThat(longReason.length()).isGreaterThan(500);
        MvcResult result = reject(partner, id, longReason);

        assertThat(status(result)).as(body(result)).isEqualTo(200);
        Booking after = bookingRepository.findById(id).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThat(after.getRejectionReason()).isEqualTo(longReason);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = BookingStatus.class, names = {"PENDING", "ACCEPTED", "CONFIRMED", "OPERATOR_ASSIGNED",
            "ON_THE_WAY", "WORK_STARTED"})
    @DisplayName("The farmer can cancel an own booking until it is finished")
    void farmerCancelsActiveBooking(BookingStatus current) throws Exception {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, newEquipment(newPartner()), current, day(3), day(4));

        MvcResult result = farmerCancel(farmer, booking.getId());

        assertThat(status(result)).as(body(result)).isEqualTo(200);
        assertThat(statusOf(booking.getId())).isEqualTo(BookingStatus.CANCELLED);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = BookingStatus.class, names = {"CANCELLED", "COMPLETED", "REJECTED"})
    @DisplayName("Cancellation is refused for CANCELLED, COMPLETED and REJECTED bookings")
    void farmerCannotCancelFinishedBooking(BookingStatus current) throws Exception {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, newEquipment(newPartner()), current, day(3), day(4));

        assertThat(status(farmerCancel(farmer, booking.getId()))).isEqualTo(400);
        assertThat(statusOf(booking.getId())).isEqualTo(current);
    }

    @Test
    @DisplayName("Another farmer, the partner and operators cannot cancel a farmer's booking")
    void cancellationIsOwnerOnly() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Long id = createBooking(farmer, newEquipment(partner), day(3), day(4));

        assertThat(status(farmerCancel(newFarmer(), id))).isEqualTo(403);
        assertThat(status(call(patch("/api/farmers/bookings/" + id + "/cancel"), tokenFor(newFarmer())))).isEqualTo(403);
        assertThat(status(call(patch("/api/bookings/" + id + "/cancel"), tokenFor(partner)))).isEqualTo(403);
        assertThat(status(call(patch("/api/bookings/" + id + "/cancel"), tokenFor(newOperator())))).isEqualTo(403);
        assertThat(statusOf(id)).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    @DisplayName("BIZ-20: cancelling a paid booking initiates exactly one full REFUND_PENDING refund")
    void cancellingAPaidBookingInitiatesTheRefund() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Long id = createBooking(farmer, newEquipment(partner), day(3), day(5));
        assertThat(status(accept(partner, id))).isEqualTo(200);
        Payment payment = insertSuccessfulPayment(bookingRepository.findById(id).orElseThrow());

        assertThat(status(farmerCancel(farmer, id))).isEqualTo(200);
        assertThat(status(farmerCancel(farmer, id))).isEqualTo(400);

        PaymentRefund refund = paymentRefundRepository.findByPaymentId(payment.getId()).orElseThrow();
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
        assertThat(refund.getAmount()).isEqualByComparingTo(payment.getAmount()).isEqualByComparingTo("7500.00");
        assertThat(paymentRefundRepository.findByPartnerId(partner.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Admin overrides: reject only from PENDING, no reactivation onto reserved dates, admin cancel works")
    void adminOverrides() throws Exception {
        Partner partner = newPartner();
        Admin admin = newAdmin();
        Equipment equipment = newEquipment(partner);
        Farmer farmer = newFarmer();
        Long confirmed = createBooking(farmer, equipment, day(3), day(4));
        assertThat(status(accept(partner, confirmed))).isEqualTo(200);

        assertThat(status(adminStatus(admin, confirmed, BookingStatus.REJECTED))).isEqualTo(400);
        assertThat(statusOf(confirmed)).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(status(adminCancel(admin, confirmed))).isEqualTo(200);
        assertThat(statusOf(confirmed)).isEqualTo(BookingStatus.CANCELLED);
        Long replacement = createBooking(newFarmer(), equipment, day(4), day(5));
        assertThat(status(adminStatus(admin, confirmed, BookingStatus.CONFIRMED)))
                .as("BIZ-13: reactivation must not double-book booking #%d's dates", replacement).isEqualTo(400);
        assertThat(statusOf(confirmed)).isEqualTo(BookingStatus.CANCELLED);

        // The admin-only endpoints refuse partners and farmers
        assertThat(status(callJson(patch("/api/admin/bookings/" + replacement + "/status"), tokenFor(partner),
                "{\"status\": \"CONFIRMED\"}"))).isEqualTo(403);
        assertThat(status(callJson(patch("/api/bookings/" + replacement + "/status"), tokenFor(partner),
                "{\"status\": \"CONFIRMED\"}"))).isEqualTo(403);
        assertThat(status(call(patch("/api/admin/bookings/" + replacement + "/cancel"), tokenFor(farmer)))).isEqualTo(403);
        assertThat(statusOf(replacement)).isEqualTo(BookingStatus.PENDING);
    }
}
