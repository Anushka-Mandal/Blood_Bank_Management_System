-- ============================================================
--  Blood Bank Management System (BBMS)
--  MySQL Database Script
--  Compatible with: MySQL 8.0+ / MariaDB 10.5+
--
--  Tables:
--    1. users          — base user entity (JOINED inheritance)
--    2. donors         — extends users for donor-specific data
--    3. recipients     — extends users for recipient-specific data
--    4. blood_units    — individual donated blood bags + embedded TestResult
--    5. blood_requests — blood requests submitted by recipients
--    6. inventory      — stock levels per blood group
--
--  Run:  mysql -u root -p < bbms_schema.sql
--        OR import via MySQL Workbench / DBeaver / phpMyAdmin
-- ============================================================

-- ── 0. Database setup ──────────────────────────────────────
DROP DATABASE IF EXISTS bbmsdb;
CREATE DATABASE bbmsdb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bbmsdb;


-- ============================================================
-- TABLE 1: users
-- Base entity for all actors: Donor, Recipient, Admin,
-- Maintainer, Hematology.
-- Uses JOINED inheritance — subclasses get their own tables
-- joined on user_id.
-- ============================================================
CREATE TABLE users (
    user_id        BIGINT          NOT NULL AUTO_INCREMENT,
    name           VARCHAR(120)    NOT NULL,
    age            INT             NOT NULL CHECK (age BETWEEN 18 AND 65),
    nid            VARCHAR(50)     NOT NULL UNIQUE,
    blood_group    VARCHAR(5)      NOT NULL,
    username       VARCHAR(50)     NOT NULL UNIQUE,
    password       VARCHAR(255)    NOT NULL,   -- BCrypt hash
    role           ENUM(
                       'DONOR',
                       'RECIPIENT',
                       'ADMIN',
                       'MAINTAINER',
                       'HEMATOLOGY'
                   ) NOT NULL,
    account_status ENUM(
                       'PENDING',
                       'APPROVED',
                       'REJECTED'
                   ) NOT NULL DEFAULT 'PENDING',

    PRIMARY KEY (user_id),
    INDEX idx_users_username       (username),
    INDEX idx_users_role           (role),
    INDEX idx_users_account_status (account_status),
    INDEX idx_users_blood_group    (blood_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- TABLE 2: donors
-- Extends users via JOINED inheritance.
-- PK = FK to users.user_id.
-- Holds donation-specific fields.
-- ============================================================
CREATE TABLE donors (
    user_id              BIGINT       NOT NULL,
    donation_date        DATE         NULL,
    donation_status      ENUM(
                             'NOT_SCHEDULED',
                             'SCHEDULED',
                             'PENDING_TEST',
                             'DONATED',
                             'REJECTED'
                         ) NOT NULL DEFAULT 'NOT_SCHEDULED',
    medical_report_notes TEXT         NULL,
    medically_approved   TINYINT(1)   NOT NULL DEFAULT 0,

    PRIMARY KEY (user_id),
    CONSTRAINT fk_donors_users
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- TABLE 3: recipients
-- Extends users via JOINED inheritance.
-- No extra columns beyond user_id — all behaviour is in
-- blood_requests (association).
-- ============================================================
CREATE TABLE recipients (
    user_id BIGINT NOT NULL,

    PRIMARY KEY (user_id),
    CONSTRAINT fk_recipients_users
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- TABLE 4: blood_units
-- Individual blood bags collected from donors.
-- Embeds TestResult columns directly (JPA @Embedded).
-- FK to donors for traceability.
-- ============================================================
CREATE TABLE blood_units (
    blood_id      BIGINT       NOT NULL AUTO_INCREMENT,
    blood_group   VARCHAR(5)   NOT NULL,
    test_status   ENUM(
                      'PENDING',
                      'SAFE',
                      'UNSAFE',
                      'EXPIRED'
                  ) NOT NULL DEFAULT 'PENDING',
    expiry_date   DATE         NOT NULL,
    collected_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    available     TINYINT(1)   NOT NULL DEFAULT 0,

    -- FK to donor
    donor_id      BIGINT       NULL,

    -- Embedded TestResult (Composition — lives inside blood_units)
    tested_by     VARCHAR(100) NULL,
    passed        TINYINT(1)   NULL,
    notes         TEXT         NULL,
    tested_at     DATETIME     NULL,

    PRIMARY KEY (blood_id),
    CONSTRAINT fk_blood_units_donor
        FOREIGN KEY (donor_id) REFERENCES donors(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    INDEX idx_bu_blood_group  (blood_group),
    INDEX idx_bu_test_status  (test_status),
    INDEX idx_bu_available    (available),
    INDEX idx_bu_donor        (donor_id),
    INDEX idx_bu_expiry       (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- TABLE 5: blood_requests
-- Submitted by recipients; flows through Maintainer → Admin.
-- Association: recipient_id → recipients
-- Association: allocated_blood_unit_id → blood_units
-- ============================================================
CREATE TABLE blood_requests (
    request_id              BIGINT       NOT NULL AUTO_INCREMENT,
    blood_group             VARCHAR(5)   NOT NULL,
    quantity                INT          NOT NULL CHECK (quantity BETWEEN 1 AND 10),
    required_date           DATE         NOT NULL,
    urgency                 VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    status                  ENUM(
                                'PENDING',
                                'FORWARDED',
                                'APPROVED',
                                'ALLOCATED',
                                'FULFILLED',
                                'REJECTED'
                            ) NOT NULL DEFAULT 'PENDING',
    admin_notes             TEXT         NULL,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,

    -- FK to recipient
    recipient_id            BIGINT       NOT NULL,

    -- FK to allocated blood unit (set when Admin allocates)
    allocated_blood_unit_id BIGINT       NULL,

    PRIMARY KEY (request_id),
    CONSTRAINT fk_br_recipient
        FOREIGN KEY (recipient_id) REFERENCES recipients(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_br_blood_unit
        FOREIGN KEY (allocated_blood_unit_id) REFERENCES blood_units(blood_id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    INDEX idx_br_recipient   (recipient_id),
    INDEX idx_br_status      (status),
    INDEX idx_br_blood_group (blood_group),
    INDEX idx_br_urgency     (urgency),
    INDEX idx_br_created_at  (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- TABLE 6: inventory
-- One row per blood group — tracks stock levels.
-- Aggregation: managed by Administrator.
-- ============================================================
CREATE TABLE inventory (
    inventory_id    BIGINT       NOT NULL AUTO_INCREMENT,
    blood_group     VARCHAR(5)   NOT NULL UNIQUE,
    total_units     INT          NOT NULL DEFAULT 0,
    available_units INT          NOT NULL DEFAULT 0,
    reserved_units  INT          NOT NULL DEFAULT 0,
    last_updated    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (inventory_id),
    INDEX idx_inv_blood_group (blood_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- SEED DATA
-- ============================================================

-- ── Staff accounts ───────────────────────────────────────
-- Passwords are BCrypt hashes of "password123"
-- You can verify at: https://bcrypt-generator.com/
INSERT INTO users (name, age, nid, blood_group, username, password, role, account_status) VALUES
('Admin User',        30, 'ADMIN001',  'O+', 'admin',      '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'ADMIN',      'APPROVED'),
('System Maintainer', 32, 'MAINT001',  'A+', 'maintainer', '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'MAINTAINER', 'APPROVED'),
('Hematology Dept',   28, 'HEMA001',   'B+', 'hematology', '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'HEMATOLOGY', 'APPROVED');


-- ── Sample Donors (all APPROVED so they can donate) ──────
INSERT INTO users (name, age, nid, blood_group, username, password, role, account_status) VALUES
('Aarav Sharma',    25, 'DON001', 'O+',  'aarav',    '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'DONOR', 'APPROVED'),
('Priya Patel',     30, 'DON002', 'A+',  'priya',    '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'DONOR', 'APPROVED'),
('Rohit Verma',     27, 'DON003', 'B+',  'rohit',    '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'DONOR', 'APPROVED'),
('Sneha Nair',      22, 'DON004', 'AB+', 'sneha',    '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'DONOR', 'APPROVED'),
('Karan Mehta',     35, 'DON005', 'O-',  'karan',    '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'DONOR', 'APPROVED'),
('Divya Reddy',     29, 'DON006', 'A-',  'divya',    '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'DONOR', 'APPROVED'),
('Amit Joshi',      31, 'DON007', 'B-',  'amit',     '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'DONOR', 'APPROVED'),
('Meera Iyer',      26, 'DON008', 'AB-', 'meera',    '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'DONOR', 'APPROVED'),
('Rahul Kumar',     24, 'DON009', 'O+',  'rahul',    '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'DONOR', 'PENDING');  -- pending to demo maintainer flow


-- ── Insert donor sub-rows (matched by SELECT on username) ─
INSERT INTO donors (user_id, donation_date, donation_status, medical_report_notes, medically_approved)
SELECT user_id, '2026-04-10', 'SCHEDULED', 'BP 118/78, Hb 14.5 g/dL, no medications', 0
FROM users WHERE username = 'aarav';

INSERT INTO donors (user_id, donation_date, donation_status, medical_report_notes, medically_approved)
SELECT user_id, '2026-04-12', 'DONATED', 'BP 120/80, Hb 13.8 g/dL, fit to donate', 1
FROM users WHERE username = 'priya';

INSERT INTO donors (user_id, donation_date, donation_status, medical_report_notes, medically_approved)
SELECT user_id, '2026-04-08', 'DONATED', 'Hb 15.0 g/dL, cholesterol normal', 1
FROM users WHERE username = 'rohit';

INSERT INTO donors (user_id, donation_date, donation_status, medical_report_notes, medically_approved)
SELECT user_id, '2026-04-15', 'SCHEDULED', 'Recent checkup clear, Hb 13.2 g/dL', 0
FROM users WHERE username = 'sneha';

INSERT INTO donors (user_id, donation_date, donation_status, medical_report_notes, medically_approved)
SELECT user_id, '2026-04-05', 'DONATED', 'Universal donor O-, Hb 14.8 g/dL', 1
FROM users WHERE username = 'karan';

INSERT INTO donors (user_id, donation_date, donation_status, medical_report_notes, medically_approved)
SELECT user_id, '2026-04-18', 'SCHEDULED', 'BP 115/75, no allergies', 0
FROM users WHERE username = 'divya';

INSERT INTO donors (user_id, donation_date, donation_status, medical_report_notes, medically_approved)
SELECT user_id, '2026-04-20', 'NOT_SCHEDULED', NULL, 0
FROM users WHERE username = 'amit';

INSERT INTO donors (user_id, donation_date, donation_status, medical_report_notes, medically_approved)
SELECT user_id, '2026-04-22', 'NOT_SCHEDULED', NULL, 0
FROM users WHERE username = 'meera';

INSERT INTO donors (user_id, donation_date, donation_status, medical_report_notes, medically_approved)
SELECT user_id, NULL, 'NOT_SCHEDULED', NULL, 0
FROM users WHERE username = 'rahul';


-- ── Sample Recipients ────────────────────────────────────
INSERT INTO users (name, age, nid, blood_group, username, password, role, account_status) VALUES
('Ananya Singh',   45, 'REC001', 'O+',  'ananya',  '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'RECIPIENT', 'APPROVED'),
('Vikram Bose',    55, 'REC002', 'A+',  'vikram',  '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'RECIPIENT', 'APPROVED'),
('Lakshmi Das',    38, 'REC003', 'B+',  'lakshmi', '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'RECIPIENT', 'APPROVED'),
('Suresh Pillai',  62, 'REC004', 'AB+', 'suresh',  '$2a$10$LO0SEc5BpIUf8UMtOscd3OY5zpzYOVWFVEJoeTu9HeuztjwPG4kMW', 'RECIPIENT', 'PENDING');

INSERT INTO recipients (user_id)
SELECT user_id FROM users WHERE username IN ('ananya','vikram','lakshmi','suresh');


-- ── Blood Units ─────────────────────────────────────────
-- Safe tested units (from priya - A+, rohit - B+, karan - O-)
INSERT INTO blood_units (blood_group, test_status, expiry_date, collected_at, available, donor_id, tested_by, passed, notes, tested_at)
SELECT 'A+', 'SAFE', DATE_ADD(CURDATE(), INTERVAL 30 DAY), NOW() - INTERVAL 2 DAY, 1,
       d.user_id, 'hematology', 1, 'All markers normal. HIV: negative, HBsAg: negative, HCV: negative.', NOW() - INTERVAL 1 DAY
FROM users u JOIN donors d ON u.user_id = d.user_id WHERE u.username = 'priya';

INSERT INTO blood_units (blood_group, test_status, expiry_date, collected_at, available, donor_id, tested_by, passed, notes, tested_at)
SELECT 'A+', 'SAFE', DATE_ADD(CURDATE(), INTERVAL 28 DAY), NOW() - INTERVAL 3 DAY, 1,
       d.user_id, 'hematology', 1, 'Blood group confirmed A+. Haemoglobin adequate. All screens clear.', NOW() - INTERVAL 2 DAY
FROM users u JOIN donors d ON u.user_id = d.user_id WHERE u.username = 'priya';

INSERT INTO blood_units (blood_group, test_status, expiry_date, collected_at, available, donor_id, tested_by, passed, notes, tested_at)
SELECT 'B+', 'SAFE', DATE_ADD(CURDATE(), INTERVAL 25 DAY), NOW() - INTERVAL 4 DAY, 1,
       d.user_id, 'hematology', 1, 'Cross-match compatible. No irregular antibodies detected.', NOW() - INTERVAL 3 DAY
FROM users u JOIN donors d ON u.user_id = d.user_id WHERE u.username = 'rohit';

INSERT INTO blood_units (blood_group, test_status, expiry_date, collected_at, available, donor_id, tested_by, passed, notes, tested_at)
SELECT 'O-', 'SAFE', DATE_ADD(CURDATE(), INTERVAL 32 DAY), NOW() - INTERVAL 1 DAY, 1,
       d.user_id, 'hematology', 1, 'Universal donor unit. All infectious disease markers negative.', NOW()
FROM users u JOIN donors d ON u.user_id = d.user_id WHERE u.username = 'karan';

INSERT INTO blood_units (blood_group, test_status, expiry_date, collected_at, available, donor_id, tested_by, passed, notes, tested_at)
SELECT 'O-', 'SAFE', DATE_ADD(CURDATE(), INTERVAL 20 DAY), NOW() - INTERVAL 5 DAY, 1,
       d.user_id, 'hematology', 1, 'Confirmed O-. Syphilis: negative. Malaria antigen: negative.', NOW() - INTERVAL 4 DAY
FROM users u JOIN donors d ON u.user_id = d.user_id WHERE u.username = 'karan';

-- Unsafe unit (rejected)
INSERT INTO blood_units (blood_group, test_status, expiry_date, collected_at, available, donor_id, tested_by, passed, notes, tested_at)
SELECT 'B+', 'UNSAFE', DATE_ADD(CURDATE(), INTERVAL 30 DAY), NOW() - INTERVAL 6 DAY, 0,
       d.user_id, 'hematology', 0, 'HCV antibody positive. Unit discarded per protocol.', NOW() - INTERVAL 5 DAY
FROM users u JOIN donors d ON u.user_id = d.user_id WHERE u.username = 'rohit';

-- Pending units (just collected, awaiting hematology)
INSERT INTO blood_units (blood_group, test_status, expiry_date, collected_at, available, donor_id, tested_by, passed, notes, tested_at)
SELECT 'O+', 'PENDING', DATE_ADD(CURDATE(), INTERVAL 35 DAY), NOW(), 0,
       d.user_id, NULL, NULL, NULL, NULL
FROM users u JOIN donors d ON u.user_id = d.user_id WHERE u.username = 'aarav';

INSERT INTO blood_units (blood_group, test_status, expiry_date, collected_at, available, donor_id, tested_by, passed, notes, tested_at)
SELECT 'AB+', 'PENDING', DATE_ADD(CURDATE(), INTERVAL 35 DAY), NOW(), 0,
       d.user_id, NULL, NULL, NULL, NULL
FROM users u JOIN donors d ON u.user_id = d.user_id WHERE u.username = 'sneha';


-- ── Inventory (one row per blood group) ─────────────────
INSERT INTO inventory (blood_group, total_units, available_units, reserved_units) VALUES
('A+',  12, 12, 0),
('A-',   5,  5, 0),
('B+',   8,  7, 1),
('B-',   3,  3, 0),
('O+',  20, 18, 2),
('O-',   6,  5, 1),
('AB+',  4,  4, 0),
('AB-',  2,  2, 0);


-- ── Blood Requests ───────────────────────────────────────
-- Fulfilled request (complete lifecycle demo)
INSERT INTO blood_requests (blood_group, quantity, required_date, urgency, status, admin_notes, created_at, updated_at, recipient_id, allocated_blood_unit_id)
SELECT 'A+', 2, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'URGENT', 'FULFILLED',
       'Request fulfilled successfully. Two units of A+ allocated from donor Priya Patel.',
       NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 1 DAY,
       r.user_id, NULL
FROM users u JOIN recipients r ON u.user_id = r.user_id WHERE u.username = 'ananya';

-- Allocated request (blood assigned, not yet delivered)
INSERT INTO blood_requests (blood_group, quantity, required_date, urgency, status, admin_notes, created_at, updated_at, recipient_id, allocated_blood_unit_id)
SELECT 'B+', 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'CRITICAL', 'ALLOCATED',
       'Approved for surgery prep. One unit of B+ allocated.',
       NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 12 HOUR,
       r.user_id,
       (SELECT blood_id FROM blood_units WHERE blood_group = 'B+' AND test_status = 'SAFE' LIMIT 1)
FROM users u JOIN recipients r ON u.user_id = r.user_id WHERE u.username = 'lakshmi';

-- Approved request (admin approved, awaiting allocation)
INSERT INTO blood_requests (blood_group, quantity, required_date, urgency, status, admin_notes, created_at, updated_at, recipient_id, allocated_blood_unit_id)
SELECT 'O-', 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'CRITICAL', 'APPROVED',
       'Emergency request approved. Locate O- units immediately.',
       NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 6 HOUR,
       r.user_id, NULL
FROM users u JOIN recipients r ON u.user_id = r.user_id WHERE u.username = 'vikram';

-- Forwarded request (maintainer forwarded, admin to review)
INSERT INTO blood_requests (blood_group, quantity, required_date, urgency, status, admin_notes, created_at, updated_at, recipient_id, allocated_blood_unit_id)
SELECT 'A+', 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'NORMAL', 'FORWARDED',
       'Reviewed by System Maintainer. Forwarded to administrator for approval.',
       NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 4 HOUR,
       r.user_id, NULL
FROM users u JOIN recipients r ON u.user_id = r.user_id WHERE u.username = 'ananya';

-- Pending request (just submitted, waiting for maintainer)
INSERT INTO blood_requests (blood_group, quantity, required_date, urgency, status, admin_notes, created_at, updated_at, recipient_id, allocated_blood_unit_id)
SELECT 'B+', 2, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'URGENT', 'PENDING',
       NULL, NOW() - INTERVAL 30 MINUTE, NULL,
       r.user_id, NULL
FROM users u JOIN recipients r ON u.user_id = r.user_id WHERE u.username = 'lakshmi';

-- Rejected request
INSERT INTO blood_requests (blood_group, quantity, required_date, urgency, status, admin_notes, created_at, updated_at, recipient_id, allocated_blood_unit_id)
SELECT 'AB-', 5, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'NORMAL', 'REJECTED',
       'Insufficient AB- stock. Recipient advised to seek alternative arrangements.',
       NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 9 DAY,
       r.user_id, NULL
FROM users u JOIN recipients r ON u.user_id = r.user_id WHERE u.username = 'vikram';


-- ============================================================
-- USEFUL VIEWS (for reports and debugging)
-- ============================================================

-- View: Full user summary across all roles
CREATE OR REPLACE VIEW v_user_summary AS
SELECT
    u.user_id,
    u.name,
    u.age,
    u.blood_group,
    u.username,
    u.role,
    u.account_status,
    -- donor fields (NULL for non-donors)
    d.donation_date,
    d.donation_status,
    d.medically_approved
FROM users u
LEFT JOIN donors    d ON u.user_id = d.user_id
LEFT JOIN recipients r ON u.user_id = r.user_id;


-- View: Blood inventory status with stock level label
CREATE OR REPLACE VIEW v_inventory_status AS
SELECT
    inventory_id,
    blood_group,
    total_units,
    available_units,
    reserved_units,
    CASE
        WHEN available_units = 0       THEN 'OUT_OF_STOCK'
        WHEN available_units <= 5      THEN 'CRITICAL'
        WHEN available_units <= 15     THEN 'LOW'
        ELSE                                'ADEQUATE'
    END AS stock_level,
    last_updated
FROM inventory
ORDER BY blood_group;


-- View: Blood request pipeline with recipient name
CREATE OR REPLACE VIEW v_request_pipeline AS
SELECT
    br.request_id,
    u.name            AS recipient_name,
    u.blood_group     AS recipient_blood_group,
    br.blood_group    AS requested_blood_group,
    br.quantity,
    br.urgency,
    br.status,
    br.required_date,
    br.created_at,
    br.updated_at,
    br.admin_notes,
    bu.blood_id       AS allocated_unit_id
FROM blood_requests br
JOIN recipients r ON br.recipient_id = r.user_id
JOIN users      u ON r.user_id       = u.user_id
LEFT JOIN blood_units bu ON br.allocated_blood_unit_id = bu.blood_id
ORDER BY br.created_at DESC;


-- View: Blood unit traceability (donor → unit → request)
CREATE OR REPLACE VIEW v_blood_unit_traceability AS
SELECT
    bu.blood_id,
    bu.blood_group,
    bu.test_status,
    bu.available,
    bu.expiry_date,
    bu.collected_at,
    bu.tested_by,
    bu.passed           AS test_passed,
    bu.notes            AS test_notes,
    bu.tested_at,
    du.name             AS donor_name,
    du.username         AS donor_username,
    -- which request this unit was allocated to (if any)
    br.request_id       AS allocated_to_request,
    ru.name             AS allocated_to_recipient
FROM blood_units bu
LEFT JOIN donors    d  ON bu.donor_id      = d.user_id
LEFT JOIN users     du ON d.user_id        = du.user_id
LEFT JOIN blood_requests br ON br.allocated_blood_unit_id = bu.blood_id
LEFT JOIN recipients rec ON br.recipient_id = rec.user_id
LEFT JOIN users      ru  ON rec.user_id     = ru.user_id;


-- View: Pending hematology test queue
CREATE OR REPLACE VIEW v_pending_tests AS
SELECT
    bu.blood_id,
    bu.blood_group,
    bu.collected_at,
    bu.expiry_date,
    u.name  AS donor_name
FROM blood_units bu
LEFT JOIN donors d ON bu.donor_id = d.user_id
LEFT JOIN users  u ON d.user_id   = u.user_id
WHERE bu.test_status = 'PENDING'
ORDER BY bu.collected_at ASC;


-- ============================================================
-- USEFUL STORED PROCEDURES (bonus — for viva demonstration)
-- ============================================================

DELIMITER $$

-- Proc: approve a user account (mimics UserService.approveUser)
CREATE PROCEDURE sp_approve_user(IN p_user_id BIGINT)
BEGIN
    UPDATE users
    SET account_status = 'APPROVED'
    WHERE user_id = p_user_id;

    SELECT CONCAT('User ', user_id, ' (', name, ') approved.') AS result
    FROM users WHERE user_id = p_user_id;
END$$


-- Proc: forward a blood request to admin (mimics BloodRequestService.forwardRequest)
CREATE PROCEDURE sp_forward_request(IN p_request_id BIGINT, IN p_notes TEXT)
BEGIN
    UPDATE blood_requests
    SET status      = 'FORWARDED',
        admin_notes = p_notes,
        updated_at  = NOW()
    WHERE request_id = p_request_id AND status = 'PENDING';

    SELECT CONCAT('Request #', request_id, ' forwarded.') AS result
    FROM blood_requests WHERE request_id = p_request_id;
END$$


-- Proc: generate inventory summary report (mimics AdminController.reports)
CREATE PROCEDURE sp_inventory_report()
BEGIN
    SELECT
        blood_group,
        total_units,
        available_units,
        reserved_units,
        CASE
            WHEN available_units = 0  THEN '⛔ OUT OF STOCK'
            WHEN available_units <= 5 THEN '🔴 CRITICAL'
            WHEN available_units <= 15 THEN '🟡 LOW'
            ELSE '🟢 ADEQUATE'
        END AS stock_level
    FROM inventory
    ORDER BY available_units ASC;
END$$


-- Proc: mark a blood unit as safe and update inventory
CREATE PROCEDURE sp_mark_blood_safe(
    IN p_blood_id  BIGINT,
    IN p_tested_by VARCHAR(100),
    IN p_notes     TEXT
)
BEGIN
    DECLARE v_blood_group VARCHAR(5);

    -- Update blood unit
    UPDATE blood_units
    SET test_status = 'SAFE',
        available   = 1,
        tested_by   = p_tested_by,
        passed      = 1,
        notes       = p_notes,
        tested_at   = NOW()
    WHERE blood_id = p_blood_id;

    -- Update donor status
    UPDATE donors d
    JOIN blood_units bu ON d.user_id = bu.donor_id
    SET d.donation_status = 'DONATED',
        d.medically_approved = 1
    WHERE bu.blood_id = p_blood_id;

    -- Increment inventory
    SELECT blood_group INTO v_blood_group FROM blood_units WHERE blood_id = p_blood_id;
    UPDATE inventory
    SET total_units     = total_units + 1,
        available_units = available_units + 1,
        last_updated    = NOW()
    WHERE blood_group = v_blood_group;

    SELECT CONCAT('Blood unit #', p_blood_id, ' marked SAFE. Inventory updated.') AS result;
END$$

DELIMITER ;


-- ============================================================
-- QUICK VERIFICATION QUERIES
-- (run these after import to confirm data loaded correctly)
-- ============================================================

-- Check user counts by role
-- SELECT role, COUNT(*) AS count, account_status FROM users GROUP BY role, account_status;

-- Check inventory
-- SELECT * FROM v_inventory_status;

-- Check request pipeline
-- SELECT request_id, recipient_name, blood_group, urgency, status FROM v_request_pipeline;

-- Check pending tests
-- SELECT * FROM v_pending_tests;

-- Check blood unit traceability
-- SELECT blood_id, blood_group, test_status, donor_name, allocated_to_recipient FROM v_blood_unit_traceability;
