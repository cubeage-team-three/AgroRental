-- -----------------------------------------------------------------------------
-- Flyway Migration V1.1: Add Unique Constraint on payments.booking_id
-- AgroRent Priority 5.2.4B.3: Enforce One Payment per Booking
-- -----------------------------------------------------------------------------
-- Enforces strictly one payment per booking to prevent duplicate charges,
-- concurrent race conditions, and corrupted financial analytics.
-- If duplicate records exist for any booking_id, this migration will FAIL
-- immediately with ER_DUP_ENTRY (error 1062) to prevent silent data alteration.
-- -----------------------------------------------------------------------------

ALTER TABLE payments
    ADD CONSTRAINT uk_payments_booking_id UNIQUE (booking_id);
