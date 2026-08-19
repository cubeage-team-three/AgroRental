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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    }
}
