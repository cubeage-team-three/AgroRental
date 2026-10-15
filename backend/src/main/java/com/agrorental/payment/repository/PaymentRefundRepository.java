package com.agrorental.payment.repository;

import com.agrorental.payment.entity.PaymentRefund;
import com.agrorental.payment.entity.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {

    Optional<PaymentRefund> findByPaymentId(Long paymentId);

    List<PaymentRefund> findByPaymentIdIn(List<Long> paymentIds);

    List<PaymentRefund> findByPartnerId(Long partnerId);

    List<PaymentRefund> findByStatusOrderByInitiatedAtAsc(RefundStatus status);

    List<PaymentRefund> findAllByOrderByInitiatedAtDesc();

    boolean existsByTransferReferenceIgnoreCase(String transferReference);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM PaymentRefund r WHERE r.status = :status")
    java.math.BigDecimal sumAmountByStatus(@Param("status") RefundStatus status);

    long countByStatus(RefundStatus status);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM PaymentRefund r WHERE r.partnerId = :partnerId AND r.status = :status")
    java.math.BigDecimal sumAmountByPartnerIdAndStatus(@Param("partnerId") Long partnerId, @Param("status") RefundStatus status);

    long countByPartnerIdAndStatus(Long partnerId, RefundStatus status);

    /** Serializes admin actions on one refund (SELECT ... FOR UPDATE). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM PaymentRefund r WHERE r.id = :id")
    Optional<PaymentRefund> findByIdForUpdate(@Param("id") Long id);
}
