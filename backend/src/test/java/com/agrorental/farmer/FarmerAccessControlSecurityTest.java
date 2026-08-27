package com.agrorental.farmer;

import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.service.BookingService;
import com.agrorental.farmer.controller.FarmerBookingController;
import com.agrorental.payment.controller.PaymentController;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.service.PaymentService;
import com.agrorental.security.principal.FarmerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class FarmerAccessControlSecurityTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private FarmerBookingController farmerBookingController;

    @InjectMocks
    private PaymentController paymentController;

    private FarmerPrincipal farmer1; // ID: 100L
    private FarmerPrincipal farmer2; // ID: 200L

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
        farmer1 = new FarmerPrincipal(100L, "9876543210", "Farmer One", "FARMER");
        farmer2 = new FarmerPrincipal(200L, "9876543211", "Farmer Two", "FARMER");
    }

    @Test
    @DisplayName("Farmer A cannot view Farmer B's booking details")
    void testFarmerCannotViewOtherFarmerBooking() {
        BookingResponse farmer2Booking = BookingResponse.builder()
                .id(99L)
                .farmerId(200L) // Belongs to Farmer 2
                .equipmentId(10L)
                .build();

        when(bookingService.getBookingById(99L)).thenReturn(farmer2Booking);

        // Attempting to fetch as Farmer 1 (ID 100L)
        assertThrows(AccessDeniedException.class, () -> {
            farmerBookingController.getBookingById(farmer1, 99L);
        });
    }

    @Test
    @DisplayName("Farmer A cannot cancel Farmer B's booking")
    void testFarmerCannotCancelOtherFarmerBooking() {
        when(bookingService.cancelBooking(eq(99L), eq(100L)))
                .thenThrow(new AccessDeniedException("Access is denied. You do not have permission to cancel another farmer's booking."));

        assertThrows(AccessDeniedException.class, () -> {
            farmerBookingController.cancelBooking(farmer1, 99L);
        });
    }

    @Test
    @DisplayName("Farmer A cannot access Farmer B's payment history")
    void testFarmerCannotAccessOtherFarmerPayments() {
        // Farmer 1 (100L) trying to query payments for Farmer 2 (200L)
        assertThrows(AccessDeniedException.class, () -> {
            paymentController.getFarmerPayments(farmer1, 200L);
        });
    }

    @Test
    @DisplayName("Farmer A can access their own payments")
    void testFarmerCanAccessOwnPayments() {
        when(paymentService.getFarmerPayments(100L)).thenReturn(List.of(
                PaymentResponse.builder().id(1L).farmerId(100L).build()
        ));

        assertDoesNotThrow(() -> {
            var response = paymentController.getFarmerPayments(farmer1, 100L);
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getData().size());
        });
    }
}
