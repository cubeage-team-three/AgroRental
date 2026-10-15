package com.agrorental.payment.repository;

import com.agrorental.payment.entity.PaymentRefundEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRefundEventRepository extends JpaRepository<PaymentRefundEvent, Long> {

    List<PaymentRefundEvent> findByRefundIdOrderByOccurredAtAscIdAsc(Long refundId);
}
