-- =============================================================================
-- AgroRent Database Migration: V1__baseline_schema.sql
-- Initial Schema Baseline for AgroRent MySQL 8.0 Persistence Model
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Root Authentication & Administrative Entities (Tier 0)
-- -----------------------------------------------------------------------------

CREATE TABLE admins (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_admins_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM ('ADMIN','FARMER','OPERATOR','PARTNER') NOT NULL,
    enabled BIT(1) NOT NULL,
    verified BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE partners (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    business_name VARCHAR(255),
    email VARCHAR(255),
    mobile_number VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    address TEXT,
    gst_number VARCHAR(255),
    pan_number VARCHAR(255),
    aadhaar_number VARCHAR(255),
    profile_photo VARCHAR(255),
    verification_status ENUM ('APPROVED','PENDING','REJECTED') NOT NULL,
    otp_code VARCHAR(255),
    otp_expiry DATETIME(6),
    otp_verified BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_partners_mobile UNIQUE (mobile_number),
    CONSTRAINT uk_partners_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE farmer_otp (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    farmer_id BIGINT,
    mobile_number VARCHAR(15) NOT NULL,
    otp VARCHAR(10) NOT NULL,
    attempt_count INT NOT NULL,
    verified BIT(1) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operator_otps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    operator_id BIGINT,
    mobile_number VARCHAR(15) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    purpose ENUM ('MOBILE_VERIFICATION','PASSWORD_RESET') NOT NULL,
    attempt_count INT NOT NULL,
    max_attempts INT NOT NULL,
    verified BIT(1) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_operator_otp_mobile ON operator_otps (mobile_number);
CREATE INDEX idx_operator_otp_expires ON operator_otps (expires_at);

-- -----------------------------------------------------------------------------
-- 2. User-Dependent & Partner-Dependent Entities (Tier 1)
-- -----------------------------------------------------------------------------

CREATE TABLE email_verification_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    used BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_email_verification_tokens_token UNIQUE (token),
    CONSTRAINT uk_email_verification_tokens_user_id UNIQUE (user_id),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    used BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE farmers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    user_id BIGINT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120),
    mobile_number VARCHAR(15) NOT NULL,
    password VARCHAR(255),
    address VARCHAR(255),
    profile_image VARCHAR(255),
    account_status VARCHAR(30),
    preferred_language VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT uk_farmers_mobile UNIQUE (mobile_number),
    CONSTRAINT uk_farmers_email UNIQUE (email),
    CONSTRAINT uk_farmers_user_id UNIQUE (user_id),
    CONSTRAINT fk_farmers_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE farms (
    farm_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    farmer_id BIGINT NOT NULL,
    farm_name VARCHAR(150) NOT NULL,
    farm_area DECIMAL(10,2) NOT NULL,
    village VARCHAR(100) NOT NULL,
    taluka VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    crop_type VARCHAR(100),
    latitude FLOAT(53),
    longitude FLOAT(53),
    PRIMARY KEY (farm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE equipment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    manufacturing_year INT NOT NULL,
    capacity VARCHAR(255) NOT NULL,
    rental_price DECIMAL(12,2) NOT NULL,
    category ENUM ('HARVESTER','IRRIGATION','OTHER','SEEDER','SPRAYER','TILLER','TRACTOR') NOT NULL,
    fuel_type ENUM ('DIESEL','ELECTRIC','HYBRID','MANUAL_HUMAN_POWERED','OTHER','PETROL') NOT NULL,
    description TEXT NOT NULL,
    location_address VARCHAR(255) NOT NULL,
    latitude FLOAT(53) NOT NULL,
    longitude FLOAT(53) NOT NULL,
    availability_status ENUM ('AVAILABLE','BOOKED','INACTIVE','UNDER_MAINTENANCE') NOT NULL,
    available_from_date DATE,
    available_to_date DATE,
    is_disabled BIT(1) NOT NULL,
    maintenance_notes VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_equipment_partner FOREIGN KEY (partner_id) REFERENCES partners (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operators (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    partner_id BIGINT,
    full_name VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    experience INT NOT NULL,
    hourly_rate DECIMAL(38,2),
    skills VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    aadhaar_number VARCHAR(255) NOT NULL,
    driving_license_number VARCHAR(255) NOT NULL,
    profile_photo VARCHAR(255),
    rejection_reason VARCHAR(255),
    status ENUM ('APPROVED','PENDING','REJECTED') NOT NULL,
    mobile_verified BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_operators_mobile UNIQUE (mobile_number),
    CONSTRAINT fk_operators_partner FOREIGN KEY (partner_id) REFERENCES partners (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Dependent Equipment, Operator & Booking Entities (Tier 2)
-- -----------------------------------------------------------------------------

CREATE TABLE equipment_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    display_order INT,
    is_primary BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_equipment_images_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operator_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    document_type ENUM ('AADHAAR','DRIVING_LICENSE','EXPERIENCE_CERTIFICATE','PROFILE_PHOTO') NOT NULL,
    verification_status ENUM ('APPROVED','PENDING','REJECTED') NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    mime_type VARCHAR(255),
    file_size BIGINT,
    document_number VARCHAR(255),
    rejection_reason VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_operator_documents_operator FOREIGN KEY (operator_id) REFERENCES operators (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bookings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    farmer_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    operator_id BIGINT,
    farm_id BIGINT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_cost DECIMAL(12,2) NOT NULL,
    status ENUM ('ACCEPTED','CANCELLED','COMPLETED','CONFIRMED','ON_THE_WAY','OPERATOR_ASSIGNED','PENDING','REJECTED','WORK_STARTED') NOT NULL,
    delivery_address VARCHAR(255),
    notes TEXT,
    rejection_reason TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_bookings_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id),
    CONSTRAINT fk_bookings_partner FOREIGN KEY (partner_id) REFERENCES partners (id),
    CONSTRAINT fk_bookings_operator FOREIGN KEY (operator_id) REFERENCES operators (id),
    CONSTRAINT fk_bookings_farm FOREIGN KEY (farm_id) REFERENCES farms (farm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Booking & Assignment Lifecycle Entities (Tier 3)
-- -----------------------------------------------------------------------------

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    farmer_id BIGINT NOT NULL,
    partner_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    payment_method ENUM ('CREDIT_CARD','DEBIT_CARD','NET_BANKING','UPI','WALLET') NOT NULL,
    payment_status ENUM ('FAILED','PENDING','REFUNDED','SUCCESS') NOT NULL,
    payment_date DATETIME(6) NOT NULL,
    payment_reference VARCHAR(255),
    transaction_id VARCHAR(255) NOT NULL,
    invoice_reference VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_transaction_id UNIQUE (transaction_id),
    CONSTRAINT uk_payments_invoice_reference UNIQUE (invoice_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operator_job_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    assignment_status ENUM ('ACCEPTED','ASSIGNED','CANCELLED','COMPLETED','IN_PROGRESS','PAUSED','REACHED','REJECTED','TRAVELING') NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    accepted_at DATETIME(6),
    traveling_at DATETIME(6),
    reached_at DATETIME(6),
    work_started_at DATETIME(6),
    paused_at DATETIME(6),
    resumed_at DATETIME(6),
    completed_at DATETIME(6),
    rejected_at DATETIME(6),
    assigned_by VARCHAR(100),
    notes TEXT,
    rejection_reason TEXT,
    completion_notes TEXT,
    pause_reason TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_operator_job_assignments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_operator_job_assignments_operator FOREIGN KEY (operator_id) REFERENCES operators (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_assignment_operator ON operator_job_assignments (operator_id);
CREATE INDEX idx_assignment_booking ON operator_job_assignments (booking_id);
CREATE INDEX idx_assignment_status ON operator_job_assignments (assignment_status);

CREATE TABLE reviews (
    review_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    booking_id BIGINT NOT NULL,
    farmer_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000),
    PRIMARY KEY (review_id),
    CONSTRAINT uk_reviews_booking_id UNIQUE (booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    recipient_role VARCHAR(50) NOT NULL,
    recipient_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    booking_id BIGINT,
    is_read BIT(1) NOT NULL,
    PRIMARY KEY (notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE live_tracking (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    operator_id BIGINT,
    latitude FLOAT(53),
    longitude FLOAT(53),
    eta VARCHAR(255),
    route_information VARCHAR(255),
    work_progress INT,
    last_updated DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE complaints (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    farmer_id BIGINT NOT NULL,
    booking_id BIGINT,
    category ENUM ('EQUIPMENT_ISSUE','LATE_ARRIVAL','OPERATOR_ISSUE','OTHER','PAYMENT_ISSUE') NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status ENUM ('CLOSED','OPEN','RESOLVED','UNDER_REVIEW') NOT NULL,
    resolution_note VARCHAR(1000),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. Operator Assignment Tracking & Review Details (Tier 4)
-- -----------------------------------------------------------------------------

CREATE TABLE operator_job_pause_intervals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    paused_at DATETIME(6) NOT NULL,
    resumed_at DATETIME(6),
    duration_minutes BIGINT,
    pause_reason TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_operator_job_pause_intervals_assignment FOREIGN KEY (assignment_id) REFERENCES operator_job_assignments (id),
    CONSTRAINT fk_operator_job_pause_intervals_operator FOREIGN KEY (operator_id) REFERENCES operators (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pause_assignment ON operator_job_pause_intervals (assignment_id);
CREATE INDEX idx_pause_operator ON operator_job_pause_intervals (operator_id);
CREATE INDEX idx_pause_paused_at ON operator_job_pause_intervals (paused_at);

CREATE TABLE operator_locations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    latitude FLOAT(53) NOT NULL,
    longitude FLOAT(53) NOT NULL,
    accuracy FLOAT(53),
    speed FLOAT(53),
    heading FLOAT(53),
    recorded_at DATETIME(6) NOT NULL,
    tracking_active BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_operator_locations_assignment FOREIGN KEY (assignment_id) REFERENCES operator_job_assignments (id),
    CONSTRAINT fk_operator_locations_operator FOREIGN KEY (operator_id) REFERENCES operators (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_op_loc_assignment ON operator_locations (assignment_id);
CREATE INDEX idx_op_loc_operator ON operator_locations (operator_id);
CREATE INDEX idx_op_loc_recorded ON operator_locations (recorded_at);
CREATE INDEX idx_op_loc_active ON operator_locations (tracking_active);

CREATE TABLE operator_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    version BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    farmer_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT uk_operator_review_assignment UNIQUE (assignment_id),
    CONSTRAINT chk_operator_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT fk_operator_reviews_assignment FOREIGN KEY (assignment_id) REFERENCES operator_job_assignments (id),
    CONSTRAINT fk_operator_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_operator_reviews_operator FOREIGN KEY (operator_id) REFERENCES operators (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_op_review_assignment ON operator_reviews (assignment_id);
CREATE INDEX idx_op_review_operator ON operator_reviews (operator_id);
CREATE INDEX idx_op_review_farmer ON operator_reviews (farmer_id);
