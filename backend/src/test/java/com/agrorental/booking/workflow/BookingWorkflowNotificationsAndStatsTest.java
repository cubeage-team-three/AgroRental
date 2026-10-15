package com.agrorental.booking.workflow;

import com.agrorental.admin.entity.Admin;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.notification.entity.Notification;
import com.agrorental.partner.entity.Partner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Items 5 and 6: notifications created by booking events (farmer, partner) and the admin booking statistics
 * after booking events. Notifications are read straight from the table; the admin stats through the API.
 */
@DisplayName("Booking workflow 5/6: notifications and admin booking stats")
class BookingWorkflowNotificationsAndStatsTest extends AbstractBookingWorkflowTest {

    @Test
    @DisplayName("Create -> partner is told; accept -> farmer is told (confirmed)")
    void creationAndAcceptanceNotify() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Equipment equipment = newEquipment(partner);
        Long id = createBooking(farmer, equipment, day(3), day(4));

        List<Notification> toPartner = notificationsFor("PARTNER", partner.getId(), id);
        assertThat(toPartner).extracting(Notification::getNotificationType).containsExactly("BOOKING_CREATED");
        assertThat(toPartner.get(0).getMessage()).contains("#" + id).contains(equipment.getName());

        assertThat(status(accept(partner, id))).isEqualTo(200);
        assertThat(notificationTypesFor("FARMER", farmer.getId(), id)).containsExactly("BOOKING_CONFIRMED");
    }

    @Test
    @DisplayName("Reject -> farmer is told with the reason, even when the reason is long")
    void rejectionNotifiesWithReason() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Long shortReason = createBooking(farmer, newEquipment(partner), day(3), day(4));
        Long longReason = createBooking(farmer, newEquipment(partner), day(3), day(4));

        assertThat(status(reject(partner, shortReason, "Tyre puncture"))).isEqualTo(200);
        List<Notification> declined = notificationsFor("FARMER", farmer.getId(), shortReason);
        assertThat(declined).extracting(Notification::getNotificationType).containsExactly("BOOKING_REJECTED");
        assertThat(declined.get(0).getMessage()).contains("Tyre puncture");

        String reason = "R".repeat(700);
        assertThat(status(reject(partner, longReason, reason))).isEqualTo(200);
        List<Notification> declinedLong = notificationsFor("FARMER", farmer.getId(), longReason);
        assertThat(declinedLong).extracting(Notification::getNotificationType).containsExactly("BOOKING_REJECTED");
        assertThat(declinedLong.get(0).getMessage().length()).isLessThanOrEqualTo(500);
    }

    @Test
    @DisplayName("Cancellation by the farmer, by the admin endpoint or by an admin status override tells farmer and partner")
    void everyCancellationPathNotifiesBothParties() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Admin admin = newAdmin();
        Long byFarmer = createBooking(farmer, newEquipment(partner), day(3), day(4));
        Long byAdminEndpoint = createBooking(farmer, newEquipment(partner), day(3), day(4));
        Long byAdminStatus = createBooking(farmer, newEquipment(partner), day(3), day(4));

        assertThat(status(farmerCancel(farmer, byFarmer))).isEqualTo(200);
        assertThat(status(adminCancel(admin, byAdminEndpoint))).isEqualTo(200);
        assertThat(status(adminStatus(admin, byAdminStatus, BookingStatus.CANCELLED))).isEqualTo(200);

        for (Long id : List.of(byFarmer, byAdminEndpoint, byAdminStatus)) {
            assertThat(notificationTypesFor("FARMER", farmer.getId(), id)).as("farmer, booking #%d", id)
                    .containsExactly("BOOKING_CANCELLED");
            assertThat(notificationTypesFor("PARTNER", partner.getId(), id)).as("partner, booking #%d", id)
                    .contains("BOOKING_CANCELLED");
        }
    }

    @Test
    @DisplayName("Admin completion tells farmer and partner; admin confirmation tells the farmer")
    void adminCompletionAndConfirmationNotify() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Admin admin = newAdmin();
        Long id = createBooking(farmer, newEquipment(partner), day(3), day(4));

        assertThat(status(adminStatus(admin, id, BookingStatus.CONFIRMED))).isEqualTo(200);
        assertThat(status(adminStatus(admin, id, BookingStatus.COMPLETED))).isEqualTo(200);

        assertThat(notificationTypesFor("FARMER", farmer.getId(), id))
                .containsExactlyInAnyOrder("BOOKING_CONFIRMED", "BOOKING_COMPLETED");
        assertThat(notificationTypesFor("PARTNER", partner.getId(), id))
                .containsExactlyInAnyOrder("BOOKING_CREATED", "BOOKING_COMPLETED");
    }

    @Test
    @DisplayName("Farmers and partners read their own booking notifications through the API")
    void notificationsAreReadableByTheirRecipients() throws Exception {
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        Long id = createBooking(farmer, newEquipment(partner), day(3), day(4));
        assertThat(status(accept(partner, id))).isEqualTo(200);

        MvcResult partnerInbox = call(get("/api/notifications"), tokenFor(partner));
        assertThat(status(partnerInbox)).isEqualTo(200);
        List<Integer> partnerBookingIds = json(partnerInbox, "$.data[*].bookingId");
        assertThat(partnerBookingIds).contains(id.intValue());

        MvcResult farmerInbox = call(get("/api/notifications"), tokenFor(farmer));
        assertThat(status(farmerInbox)).isEqualTo(200);
        List<String> farmerTypes = json(farmerInbox, "$.data[*].notificationType");
        assertThat(farmerTypes).contains("BOOKING_CONFIRMED");

        MvcResult strangerInbox = call(get("/api/notifications"), tokenFor(newFarmer()));
        List<Integer> strangerBookingIds = json(strangerInbox, "$.data[*].bookingId");
        assertThat(strangerBookingIds).doesNotContain(id.intValue());
    }

    @Test
    @DisplayName("Admin booking stats move exactly with booking events (deltas)")
    void adminStatsFollowBookingEvents() throws Exception {
        Admin admin = newAdmin();
        Partner partner = newPartner();
        Farmer farmer = newFarmer();
        MvcResult before = call(get("/api/admin/bookings/stats"), tokenFor(admin));
        assertThat(status(before)).isEqualTo(200);

        Long pending = createBooking(farmer, newEquipment(partner), day(3), day(4));          // 5000
        Long confirmed = createBooking(farmer, newEquipment(partner), day(3), day(5));        // 7500
        Long rejected = createBooking(farmer, newEquipment(partner), day(3), day(3));         // 2500
        Long cancelled = createBooking(farmer, newEquipment(partner), day(3), day(3));        // 2500
        Long completed = createBooking(farmer, newEquipment(partner), day(3), day(6));        // 10000
        assertThat(status(accept(partner, confirmed))).isEqualTo(200);
        assertThat(status(reject(partner, rejected, "No"))).isEqualTo(200);
        assertThat(status(farmerCancel(farmer, cancelled))).isEqualTo(200);
        assertThat(status(accept(partner, completed))).isEqualTo(200);
        assertThat(status(adminStatus(admin, completed, BookingStatus.COMPLETED))).isEqualTo(200);
        assertThat(statusOf(pending)).isEqualTo(BookingStatus.PENDING);

        MvcResult after = call(get("/api/admin/bookings/stats"), tokenFor(admin));
        assertThat(status(after)).isEqualTo(200);
        assertThat(delta(before, after, "$.data.totalBookings")).isEqualTo(5);
        assertThat(delta(before, after, "$.data.pendingBookings")).isEqualTo(1);
        assertThat(delta(before, after, "$.data.activeBookings")).isEqualTo(1);
        assertThat(delta(before, after, "$.data.completedBookings")).isEqualTo(1);
        assertThat(delta(before, after, "$.data.cancelledBookings")).isEqualTo(2);
        BigDecimal amountDelta = new BigDecimal(json(after, "$.data.totalAmount").toString())
                .subtract(new BigDecimal(json(before, "$.data.totalAmount").toString()));
        assertThat(amountDelta).isEqualByComparingTo("22500.00");

        assertThat(status(call(get("/api/admin/bookings/stats"), tokenFor(partner)))).isEqualTo(403);
    }

    private static long delta(MvcResult before, MvcResult after, String path) throws Exception {
        return ((Number) json(after, path)).longValue() - ((Number) json(before, path)).longValue();
    }
}
