-- BIZ-20: Refund tracking for paid bookings that are cancelled or rejected.
--
-- Additive only: creates two new tables and does not alter or rewrite any existing table or row.
-- The original payment (payments.payment_status = 'SUCCESS', payments.amount) is preserved as the
-- historical record of money collected; refunds are tracked separately.
--
--   payment_refunds        - at most one refund obligation per payment (uk_payment_refunds_payment_id)
--   payment_refund_events  - append-only audit history of every refund state transition (incl. retries)
--
-- "No refund" (NONE) is represented by the absence of a payment_refunds row, so existing payments
-- need no backfill.

CREATE TABLE payment_refunds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    farmer_id BIGINT NOT NULL,
    partner_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM ('REFUND_FAILED','REFUND_PENDING','REFUNDED') NOT NULL,
    initiation_reason VARCHAR(500) NOT NULL,
    initiated_at DATETIME(6) NOT NULL,
    initiated_by VARCHAR(100) NOT NULL,
    attempt_count INT NOT NULL,
    transfer_reference VARCHAR(64),
    completed_at DATETIME(6),
    completed_by VARCHAR(100),
    last_failure_reason VARCHAR(500),
    last_failed_at DATETIME(6),
    last_failed_by VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_refunds_payment_id UNIQUE (payment_id),
    CONSTRAINT uk_payment_refunds_transfer_reference UNIQUE (transfer_reference),
    CONSTRAINT fk_payment_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payment_refunds_status ON payment_refunds (status);
CREATE INDEX idx_payment_refunds_partner_status ON payment_refunds (partner_id, status);
CREATE INDEX idx_payment_refunds_booking ON payment_refunds (booking_id);

CREATE TABLE payment_refund_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    refund_id BIGINT NOT NULL,
    from_status ENUM ('REFUND_FAILED','REFUND_PENDING','REFUNDED'),
    to_status ENUM ('REFUND_FAILED','REFUND_PENDING','REFUNDED') NOT NULL,
    actor VARCHAR(100) NOT NULL,
    note VARCHAR(500),
    transfer_reference VARCHAR(64),
    occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payment_refund_events_refund FOREIGN KEY (refund_id) REFERENCES payment_refunds (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payment_refund_events_refund ON payment_refund_events (refund_id, occurred_at);
