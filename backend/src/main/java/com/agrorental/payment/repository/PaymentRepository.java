package com.agrorental.payment.repository;

import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for Payment entities.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByFarmerIdOrderByPaymentDateDesc(Long farmerId);

    List<Payment> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);

    List<Payment> findByPartnerIdOrderByCreatedAtDesc(Long partnerId);

    Optional<Payment> findByTransactionId(String transactionId);

    boolean existsByBookingIdAndPaymentStatus(Long bookingId, PaymentStatus paymentStatus);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.partnerId = :partnerId AND p.paymentStatus = :status")
    BigDecimal sumAmountByPartnerIdAndStatus(@Param("partnerId") Long partnerId, @Param("status") PaymentStatus status);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.partnerId = :partnerId AND p.paymentStatus = :status")
    long countByPartnerIdAndStatus(@Param("partnerId") Long partnerId, @Param("status") PaymentStatus status);
}
