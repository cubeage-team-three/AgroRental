package com.agrorental.farmer;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.farmer.dto.FarmerDashboardResponse;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.farmer.service.FarmerDashboardService;
import com.agrorental.farmer.service.FarmerProfileService;
import com.agrorental.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Farmer dashboard: active bookings follow the reserving-status rule")
class FarmerDashboardServiceTest {

    private static final long FARMER_ID = 7L;

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final FarmRepository farmRepository = mock(FarmRepository.class);
    private final FarmerDashboardService service = new FarmerDashboardService(
            mock(FarmerProfileService.class), farmRepository, bookingRepository, paymentRepository);

    private static Booking booking(long id, BookingStatus status) {
        Booking b = Booking.builder().farmerId(FARMER_ID).status(status).totalCost(new BigDecimal("2500.00"))
                .equipment(Equipment.builder().name("Tractor " + id).build()).build();
        b.setId(id);
        return b;
    }

    private FarmerDashboardResponse dashboardWith(Booking... bookings) {
        when(farmRepository.findByFarmerId(FARMER_ID)).thenReturn(List.of());
        when(paymentRepository.findByFarmerIdOrderByCreatedAtDesc(FARMER_ID)).thenReturn(List.of());
        when(bookingRepository.findByFarmerId(FARMER_ID)).thenReturn(List.of(bookings));
        return service.getDashboardData(FARMER_ID);
    }

    @Test
    @DisplayName("In-field bookings count as active; rejected, cancelled and completed do not")
    void activeCountUsesEveryReservingStatus() {
        FarmerDashboardResponse d = dashboardWith(
                booking(1, BookingStatus.REJECTED), booking(2, BookingStatus.ON_THE_WAY), booking(3, BookingStatus.PENDING),
                booking(4, BookingStatus.COMPLETED), booking(5, BookingStatus.CANCELLED), booking(6, BookingStatus.WORK_STARTED),
                booking(8, BookingStatus.OPERATOR_ASSIGNED));

        assertThat(d.getActiveBookingsCount()).isEqualTo(4);   // ON_THE_WAY, PENDING, WORK_STARTED, OPERATOR_ASSIGNED
        assertThat(d.getCompletedBookingsCount()).isEqualTo(1);
        // the status line describes the most recent active booking, never a rejected one
        assertThat(d.getActiveBookingStatusMessage()).isEqualTo("Tractor 8 Reservation #8 is currently OPERATOR_ASSIGNED");
    }

    @Test
    @DisplayName("Only rejected/cancelled/completed bookings: nothing is reported as in progress")
    void closedBookingsAreNeverReportedAsActive() {
        FarmerDashboardResponse d = dashboardWith(booking(11, BookingStatus.REJECTED), booking(12, BookingStatus.CANCELLED),
                booking(13, BookingStatus.COMPLETED));

        assertThat(d.getActiveBookingsCount()).isZero();
        assertThat(d.getActiveBookingStatusMessage()).isEqualTo("No active bookings currently in progress.");
    }
}
