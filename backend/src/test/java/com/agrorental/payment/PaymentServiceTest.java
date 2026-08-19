package com.agrorental.payment;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.partner.entity.Partner;
import com.agrorental.payment.dto.PartnerEarningsSummary;
import com.agrorental.payment.dto.PaymentCreateRequest;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.entity.PaymentTransaction;
import com.agrorental.payment.enums.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentService paymentService;

    private Booking testBooking;
    private Equipment testEquipment;
    private Partner testPartner;
    private PaymentTransaction testTransaction;

    @BeforeEach
    void setUp() {
        testPartner = Partner.builder().fullName("Patil Agro Fleet").build();
        testPartner.setId(10L);

        testEquipment = Equipment.builder().name("Mahindra 575 DI Tractor").partner(testPartner).build();
        testEquipment.setId(1L);

        testBooking = Booking.builder()
                .farmerId(50L)
                .partner(testPartner)
                .equipment(testEquipment)
                .status(BookingStatus.CONFIRMED)
                .totalCost(BigDecimal.valueOf(7500))
                .build();
        testBooking.setId(100L);

        testTransaction = PaymentTransaction.builder()
                .id(1L)
                .bookingId(100L)
                .farmerId(50L)
                .partnerId(10L)
                .amount(BigDecimal.valueOf(7500))
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentReference("TXN-TEST1234")
                .paymentMethod("SIMULATED_UPI")
                .build();
    }

    @Test
    @DisplayName("createPayment: Processes successful payment and dispatches Phase 7 notifications")
    void createPayment_Success() {
        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(100L)
                .farmerId(50L)
                .amount(BigDecimal.valueOf(7500))
                .paymentMethod("SIMULATED_UPI")
                .simulateFailure(false)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.existsByBookingIdAndPaymentStatus(100L, PaymentStatus.SUCCESS)).thenReturn(false);
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(testTransaction);

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
        assertEquals(BigDecimal.valueOf(7500), response.getAmount());

        verify(notificationService).sendNotification(eq("FARMER"), eq(50L), anyString(), anyString(), eq("PAYMENT_SUCCESS"), eq(100L));
        verify(notificationService).sendNotification(eq("PARTNER"), eq(10L), anyString(), anyString(), eq("PAYMENT_RECEIVED"), eq(100L));
    }

    @Test
    @DisplayName("createPayment: Throws BadRequestException on farmer ownership mismatch")
    void createPayment_UnauthorizedFarmer_ThrowsException() {
        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(100L)
                .farmerId(999L)
                .amount(BigDecimal.valueOf(7500))
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> paymentService.createPayment(request));

        assertTrue(exception.getMessage().contains("Farmer is not authorized"));
    }

    @Test
    @DisplayName("createPayment: Throws BadRequestException if booking is CANCELLED")
    void createPayment_CancelledBooking_ThrowsException() {
        testBooking.setStatus(BookingStatus.CANCELLED);
        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(100L)
                .farmerId(50L)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> paymentService.createPayment(request));

        assertTrue(exception.getMessage().contains("Cannot process payment for booking in state"));
    }

    @Test
    @DisplayName("createPayment: Throws BadRequestException on duplicate successful payment")
    void createPayment_DuplicatePayment_ThrowsException() {
        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(100L)
                .farmerId(50L)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.existsByBookingIdAndPaymentStatus(100L, PaymentStatus.SUCCESS)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> paymentService.createPayment(request));

        assertTrue(exception.getMessage().contains("Payment has already been completed"));
    }

    @Test
    @DisplayName("createPayment: Throws BadRequestException on payable amount mismatch")
    void createPayment_AmountMismatch_ThrowsException() {
        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(100L)
                .farmerId(50L)
                .amount(BigDecimal.valueOf(100))
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> paymentService.createPayment(request));

        assertTrue(exception.getMessage().contains("Payment amount mismatch"));
    }

    @Test
    @DisplayName("getPartnerEarningsSummary: Computes total realized revenue and transaction count")
    void getPartnerEarningsSummary_Success() {
        when(paymentRepository.sumAmountByPartnerIdAndStatus(10L, PaymentStatus.SUCCESS))
                .thenReturn(BigDecimal.valueOf(25000));
        when(paymentRepository.countByPartnerIdAndStatus(10L, PaymentStatus.SUCCESS))
                .thenReturn(4L);

        PartnerEarningsSummary summary = paymentService.getPartnerEarningsSummary(10L);

        assertNotNull(summary);
        assertEquals(10L, summary.getPartnerId());
        assertEquals(BigDecimal.valueOf(25000), summary.getTotalRealizedEarnings());
        assertEquals(4L, summary.getCompletedTransactionCount());
    }
}
