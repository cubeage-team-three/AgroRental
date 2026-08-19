package com.agrorental.payment.repository;

import com.agrorental.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);
    List<Payment> findByFarmerIdOrderByPaymentDateDesc(Long farmerId);
    Optional<Payment> findByTransactionId(String transactionId);
}
