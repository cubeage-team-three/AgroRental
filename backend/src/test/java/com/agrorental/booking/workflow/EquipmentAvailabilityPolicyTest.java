package com.agrorental.booking.workflow;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.equipment.service.EquipmentAvailabilityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The availability policy as a table, and the order of operations of the recount (lock the equipment row, then
 * count the other reserving bookings, then write).
 */
@DisplayName("Equipment availability policy (unit)")
class EquipmentAvailabilityPolicyTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(AvailabilityStatus.class)
    @DisplayName("UNDER_MAINTENANCE / INACTIVE are kept; otherwise BOOKED iff a reserving booking exists")
    void policyTable(AvailabilityStatus current) {
        boolean operatorControlled = current == AvailabilityStatus.UNDER_MAINTENANCE || current == AvailabilityStatus.INACTIVE;

        assertThat(EquipmentAvailabilityService.derive(current, true))
                .isEqualTo(operatorControlled ? current : AvailabilityStatus.BOOKED);
        assertThat(EquipmentAvailabilityService.derive(current, false))
                .isEqualTo(operatorControlled ? current : AvailabilityStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Reserving statuses are exactly the BIZ-13 set")
    void reservingStatuses() {
        assertThat(BookingStatus.RESERVING_STATUSES).containsExactly(BookingStatus.PENDING, BookingStatus.ACCEPTED,
                BookingStatus.CONFIRMED, BookingStatus.OPERATOR_ASSIGNED, BookingStatus.ON_THE_WAY, BookingStatus.WORK_STARTED);
        assertThat(BookingStatus.COMPLETED.isReserving()).isFalse();
        assertThat(BookingStatus.CANCELLED.isReserving()).isFalse();
        assertThat(BookingStatus.REJECTED.isReserving()).isFalse();
        assertThat(BookingStatus.ON_THE_WAY.isReserving()).isTrue();
    }

    @Test
    @DisplayName("Ending one booking locks the equipment, counts the OTHER reserving bookings, then writes")
    void endingABookingRecountsOthersUnderTheLock() {
        BookingRepository bookings = mock(BookingRepository.class);
        EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
        Equipment equipment = Equipment.builder().name("Tractor").availabilityStatus(AvailabilityStatus.BOOKED).build();
        equipment.setId(5L);
        Booking cancelled = Booking.builder().equipment(equipment).status(BookingStatus.CANCELLED).build();
        cancelled.setId(50L);
        when(equipmentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(equipment));
        when(bookings.existsOtherBookingInStatuses(eq(5L), eq(50L), anyList())).thenReturn(true);

        AvailabilityStatus result = new EquipmentAvailabilityService(bookings, equipmentRepository)
                .refreshAfterBookingChange(cancelled);

        assertThat(result).isEqualTo(AvailabilityStatus.BOOKED);
        InOrder order = inOrder(equipmentRepository, bookings);
        order.verify(equipmentRepository).findByIdForUpdate(5L);
        order.verify(bookings).existsOtherBookingInStatuses(5L, 50L, BookingStatus.RESERVING_STATUSES);
        order.verify(equipmentRepository).save(equipment);
    }

    @Test
    @DisplayName("A reserving booking needs no count; maintenance survives a new reservation")
    void reservingBookingShortCircuitsAndMaintenanceIsKept() {
        BookingRepository bookings = mock(BookingRepository.class);
        EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
        Equipment equipment = Equipment.builder().name("Tractor").availabilityStatus(AvailabilityStatus.UNDER_MAINTENANCE).build();
        equipment.setId(6L);
        Booking pending = Booking.builder().equipment(equipment).status(BookingStatus.PENDING).build();
        pending.setId(60L);
        when(equipmentRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(equipment));

        AvailabilityStatus result = new EquipmentAvailabilityService(bookings, equipmentRepository)
                .refreshAfterBookingChange(pending);

        assertThat(result).isEqualTo(AvailabilityStatus.UNDER_MAINTENANCE);
        assertThat(equipment.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.UNDER_MAINTENANCE);
        verify(bookings, never()).existsOtherBookingInStatuses(any(), any(), anyList());
    }
}
