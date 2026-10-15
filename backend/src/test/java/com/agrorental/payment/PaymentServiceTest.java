package com.agrorental.payment;

import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.service.BookingService;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import com.agrorental.farmer.service.FarmerProfileService;
import com.agrorental.payment.dto.InvoiceResponse;
import com.agrorental.payment.dto.PaymentRequest;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.payment.service.PaymentService;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.security.principal.FarmerPrincipal;
import com.agrorental.booking.entity.BookingStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingService bookingService;

    @Mock
    private FarmerProfileService farmerProfileService;

    @InjectMocks
    private PaymentService paymentService;

    private BookingResponse mockBooking;
    private Payment mockPayment;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockBooking = BookingResponse.builder()
                .id(10L)
                .farmerId(1L)
                .equipmentId(5L)
                .equipmentName("Mahindra 575 DI Tractor")
                .equipmentCategory("TRACTOR")
                .partnerId(2L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .totalCost(BigDecimal.valueOf(4500))
                .status(BookingStatus.CONFIRMED)
                .build();

        mockPayment = Payment.builder()
                .bookingId(10L)
                .farmerId(1L)
                .partnerId(2L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .transactionId("TXN-12345")
                .paymentReference("TXN-12345")
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentDate(LocalDateTime.now())
                .invoiceReference("INV-2026-00010")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void processPayment_Success() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .farmerId(1L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(10L, response.getBookingId());
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void processPayment_PartnerOrOperatorCaller_DeniedEvenWithBookingFarmerId() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        Object[] nonFarmerPrincipals = {
                com.agrorental.security.principal.PartnerPrincipal.builder().id(1L).role("PARTNER").build(),
                com.agrorental.security.principal.OperatorPrincipal.builder().id(1L).role("OPERATOR").build()
        };
        String[] authorities = {"ROLE_PARTNER", "ROLE_OPERATOR"};

        for (int i = 0; i < nonFarmerPrincipals.length; i++) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    nonFarmerPrincipals[i], null, List.of(new SimpleGrantedAuthority(authorities[i]))));
            // The request names the booking's real farmer (1L), which used to be enough to pass
            PaymentRequest request = PaymentRequest.builder()
                    .bookingId(10L)
                    .farmerId(1L)
                    .amount(BigDecimal.valueOf(4500))
                    .paymentMethod(PaymentMethod.UPI)
                    .build();

            assertThrows(AccessDeniedException.class, () -> paymentService.processPayment(request));
        }
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_AuthenticatedFarmer_UsesPrincipalNotRequestFarmerId() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                FarmerPrincipal.builder().id(1L).build(), null, List.of(new SimpleGrantedAuthority("ROLE_FARMER"))));

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .farmerId(999L) // ignored: the authenticated owning farmer is used
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        paymentService.processPayment(request);

        assertEquals(1L, request.getFarmerId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void processPayment_Admin_RecordsPaymentForBookingsOwnFarmer() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "ADMIN_1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .farmerId(999L) // a caller-supplied farmer must never be recorded
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        paymentService.processPayment(request);

        assertEquals(1L, request.getFarmerId());
    }

    @Test
    void processPayment_InternalCallWithoutFarmerId_Denied() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        assertThrows(AccessDeniedException.class, () -> paymentService.processPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void getFarmerPayments_ReturnsList() {
        when(paymentRepository.findByFarmerIdOrderByPaymentDateDesc(1L)).thenReturn(List.of(mockPayment));

        List<PaymentResponse> list = paymentService.getFarmerPayments(1L);

        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals("TXN-12345", list.get(0).getTransactionId());
    }

    @Test
    void generateInvoice_Success() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.of(mockPayment));
        when(farmerProfileService.getProfile(1L)).thenReturn(
                FarmerProfileResponse.builder()
                        .farmerId(1L)
                        .fullName("Ramesh Kumar")
                        .mobileNumber("9876543210")
                        .build()
        );

        InvoiceResponse invoice = paymentService.generateInvoice(10L);

        assertNotNull(invoice);
        assertEquals("INV-2026-00010", invoice.getInvoiceReference());
        assertEquals("Ramesh Kumar", invoice.getFarmerName());
        assertEquals("Mahindra 575 DI Tractor", invoice.getEquipmentName());
        assertEquals("SUCCESS", invoice.getStatus());
        assertEquals("CONFIRMED", invoice.getBookingStatus());
    }

    @Test
    void generateInvoice_UnpaidBooking_ThrowsResourceNotFoundException() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                paymentService.generateInvoice(10L)
        );

        assertTrue(ex.getMessage().contains("No payment transaction found for booking ID: 10"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void generateInvoice_AnotherFarmer_ThrowsAccessDeniedException() {
        FarmerPrincipal otherFarmer = FarmerPrincipal.builder().id(99L).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(otherFarmer, null, List.of(new SimpleGrantedAuthority("ROLE_FARMER")))
        );

        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                paymentService.generateInvoice(10L)
        );

        assertTrue(ex.getMessage().contains("Access is denied"));
        verify(paymentRepository, never()).findByBookingId(anyLong());
    }

    @Test
    void generateInvoice_Admin_AllowedForAnyFarmer() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminUser", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.of(mockPayment));

        InvoiceResponse invoice = paymentService.generateInvoice(10L);

        assertNotNull(invoice);
        assertEquals("INV-2026-00010", invoice.getInvoiceReference());
    }

    @Test
    void generateInvoice_CancelledBookingWithPayment_PreservesActualStatuses() {
        mockBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.of(mockPayment));

        InvoiceResponse invoice = paymentService.generateInvoice(10L);

        assertNotNull(invoice);
        assertEquals("SUCCESS", invoice.getStatus());
        assertEquals("CANCELLED", invoice.getBookingStatus());
        assertNotEquals("REFUNDED", invoice.getStatus());
    }

    @Test
    void generateInvoice_RejectedBookingWithPayment_PreservesActualStatuses() {
        mockBooking.setStatus(BookingStatus.REJECTED);
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.of(mockPayment));

        InvoiceResponse invoice = paymentService.generateInvoice(10L);

        assertNotNull(invoice);
        assertEquals("SUCCESS", invoice.getStatus());
        assertEquals("REJECTED", invoice.getBookingStatus());
        assertNotEquals("REFUNDED", invoice.getStatus());
    }

    @Test
    void processPayment_Duplicate_ReturnsExistingPayment() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.of(mockPayment));

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .farmerId(1L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(10L, response.getBookingId());
        assertEquals("TXN-12345", response.getTransactionId());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_MismatchedFarmer_ThrowsAccessDeniedException() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .farmerId(999L) // Does not match mockBooking farmerId (1L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            paymentService.processPayment(request);
        });
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_CancelledBooking_ThrowsBadRequestException() {
        BookingResponse cancelledBooking = BookingResponse.builder()
                .id(10L)
                .farmerId(1L)
                .totalCost(BigDecimal.valueOf(4500))
                .status(com.agrorental.booking.entity.BookingStatus.CANCELLED)
                .build();
        when(bookingService.getBookingById(10L)).thenReturn(cancelledBooking);

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .farmerId(1L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        assertThrows(com.agrorental.common.exception.BadRequestException.class, () -> {
            paymentService.processPayment(request);
        });
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_AmountLessThanTotalCost_ThrowsBadRequestException() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .farmerId(1L)
                .amount(BigDecimal.valueOf(1000)) // Less than required 4500
                .paymentMethod(PaymentMethod.UPI)
                .build();

        assertThrows(com.agrorental.common.exception.BadRequestException.class, () -> {
            paymentService.processPayment(request);
        });
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_ConcurrencyCollision_RecoversWinningPayment() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        // Initial fast-path check returns empty
        // In-transaction check returns empty to simulate race reaching save
        // Recovery check in catch block returns winning payment
        when(paymentRepository.findByBookingId(10L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(mockPayment));

        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "Duplicate entry '10' for key 'payments.uk_payments_booking_id'"));

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .farmerId(1L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(10L, response.getBookingId());
        assertEquals("TXN-12345", response.getTransactionId());
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
    }

    @Test
    void processPayment_UnrelatedConstraintViolation_RethrowsException() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "Cannot add or update a child row: a foreign key constraint fails"));

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .farmerId(1L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            paymentService.processPayment(request);
        });
    }

    @Test
    void processPayment_LockAcquisitionFailure_RethrowsCannotAcquireLockException() {
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new org.springframework.dao.CannotAcquireLockException(
                        "Deadlock found when trying to get lock; try restarting transaction"));

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .farmerId(1L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        assertThrows(org.springframework.dao.CannotAcquireLockException.class, () -> {
            paymentService.processPayment(request);
        });
    }

    @Test
    void processPayment_ExistingFailedPayment_UpdatesInPlace() {
        Payment failedPayment = Payment.builder()
                .bookingId(10L)
                .farmerId(1L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .transactionId("TXN-FAILED-1")
                .paymentStatus(PaymentStatus.FAILED)
                .paymentDate(LocalDateTime.now().minusDays(1))
                .invoiceReference("INV-OLD")
                .build();

        when(bookingService.getBookingById(10L)).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.of(failedPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(failedPayment);

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(10L)
                .farmerId(1L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, failedPayment.getPaymentStatus());
        verify(paymentRepository, times(1)).save(failedPayment);
    }
}
