package com.agrorental.integration;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.payment.controller.PaymentController;
import com.agrorental.payment.dto.InvoiceResponse;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.service.PaymentService;
import com.agrorental.security.principal.FarmerPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment & Invoice Integration Controller Tests")
class PaymentControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private PaymentResponse mockPaymentResponse;
    private InvoiceResponse mockInvoiceResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        authenticateAsFarmer(1L);

        mockPaymentResponse = PaymentResponse.builder()
                .id(1L)
                .bookingId(10L)
                .farmerId(1L)
                .amount(BigDecimal.valueOf(4500))
                .paymentMethod(PaymentMethod.UPI)
                .transactionId("TXN-998877")
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentDate(LocalDateTime.now())
                .invoiceReference("INV-2026-00010")
                .build();

        mockInvoiceResponse = InvoiceResponse.builder()
                .invoiceReference("INV-2026-00010")
                .transactionId("TXN-998877")
                .bookingId(10L)
                .farmerId(1L)
                .farmerName("Ramesh Kumar")
                .farmerMobile("9876543210")
                .equipmentName("Mahindra 575 DI Tractor")
                .equipmentCategory("TRACTOR")
                .partnerName("GreenFields Partner")
                .bookingStartDate(LocalDate.now())
                .bookingEndDate(LocalDate.now().plusDays(2))
                .rentalRatePerDay(BigDecimal.valueOf(1228.81))
                .rentalDays(3L)
                .subtotal(BigDecimal.valueOf(3686.44))
                .gstAmount(BigDecimal.valueOf(813.56))
                .totalAmount(BigDecimal.valueOf(4500.00))
                .paymentMethod(PaymentMethod.UPI)
                .paymentDate(LocalDateTime.now())
                .status("SUCCESS")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsFarmer(Long farmerId) {
        FarmerPrincipal principal = FarmerPrincipal.builder().id(farmerId).role("FARMER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_FARMER")))
        );
    }

    @Test
    void shouldProcessPayment() throws Exception {
        when(paymentService.processPayment(any())).thenReturn(mockPaymentResponse);

        String payload = """
            {
              "bookingId": 10,
              "farmerId": 1,
              "amount": 4500,
              "paymentMethod": "UPI"
            }
            """;

        mockMvc.perform(post("/api/farmers/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value("TXN-998877"))
                .andExpect(jsonPath("$.data.invoiceReference").value("INV-2026-00010"));
    }

    @Test
    void shouldGetFarmerPayments() throws Exception {
        when(paymentService.getFarmerPayments(1L)).thenReturn(List.of(mockPaymentResponse));

        mockMvc.perform(get("/api/farmers/payments/farmer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].invoiceReference").value("INV-2026-00010"));
    }

    @Test
    void shouldGenerateInvoice() throws Exception {
        when(paymentService.generateInvoice(10L)).thenReturn(mockInvoiceResponse);

        mockMvc.perform(get("/api/farmers/bookings/10/invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.farmerName").value("Ramesh Kumar"))
                .andExpect(jsonPath("$.data.totalAmount").value(4500.00));
    }
}
