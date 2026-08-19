package com.agrorental.payment.repository;

import com.agrorental.payment.entity.PaymentTransaction;
import com.agrorental.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for PaymentTransaction entities.
 */
@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByBookingId(Long bookingId);

    List<PaymentTransaction> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);

    List<PaymentTransaction> findByPartnerIdOrderByCreatedAtDesc(Long partnerId);

    boolean existsByBookingIdAndPaymentStatus(Long bookingId, PaymentStatus paymentStatus);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTransaction p WHERE p.partnerId = :partnerId AND p.paymentStatus = :status")
    BigDecimal sumAmountByPartnerIdAndStatus(@Param("partnerId") Long partnerId, @Param("status") PaymentStatus status);

    @Query("SELECT COUNT(p) FROM PaymentTransaction p WHERE p.partnerId = :partnerId AND p.paymentStatus = :status")
    long countByPartnerIdAndStatus(@Param("partnerId") Long partnerId, @Param("status") PaymentStatus status);
}
