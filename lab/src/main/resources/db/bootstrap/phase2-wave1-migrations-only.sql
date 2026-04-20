-- Phase 2 Wave 1 migrations-only SQL
-- Source: src/main/resources/db/migration/V1-V9
-- Regenerate with: powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-phase2-bootstrap.ps1

SET NAMES utf8mb4;

-- ==================================================
-- BEGIN V1__search_and_list_indexes.sql
-- ==================================================

CREATE TABLE IF NOT EXISTS t_college (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    college_code VARCHAR(64) NOT NULL,
    college_name VARCHAR(128) NOT NULL,
    admin_user_id BIGINT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_college_code (college_code),
    KEY idx_college_admin_deleted (admin_user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_lab (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_name VARCHAR(100) NOT NULL,
    lab_code VARCHAR(64) NULL,
    college_id BIGINT NULL,
    lab_desc TEXT NOT NULL,
    teacher_name VARCHAR(64) NULL,
    location VARCHAR(128) NULL,
    contact_email VARCHAR(128) NULL,
    require_skill VARCHAR(255) NOT NULL,
    recruit_num INT NOT NULL DEFAULT 0,
    current_num INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    founding_date VARCHAR(50) NULL,
    awards TEXT NULL,
    outstanding_seniors TEXT NULL,
    basic_info TEXT NULL,
    advisors VARCHAR(255) NULL,
    current_admins VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_lab_name (lab_name),
    UNIQUE KEY uk_lab_code (lab_code),
    KEY idx_lab_deleted_college_status_create (deleted, college_id, status, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'student',
    student_id VARCHAR(20) NULL,
    college VARCHAR(100) NULL,
    major VARCHAR(100) NULL,
    grade VARCHAR(20) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(100) NULL,
    avatar VARCHAR(255) NULL,
    resume VARCHAR(1024) NULL,
    lab_id BIGINT NULL,
    can_edit TINYINT NOT NULL DEFAULT 1,
    system_account_code VARCHAR(64) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_student_id (student_id, deleted),
    UNIQUE KEY uk_user_email (email, deleted),
    UNIQUE KEY uk_user_system_account_code (system_account_code, deleted),
    KEY idx_user_lab_deleted (lab_id, deleted),
    KEY idx_user_role_deleted_create (role, deleted, create_time, id),
    KEY idx_user_lab_role_status_create (lab_id, role, status, deleted, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_email_auth_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account VARCHAR(64) NOT NULL,
    email VARCHAR(100) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expire_time DATETIME NOT NULL,
    is_used TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_email_auth_code_lookup (purpose, email, account, is_used, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_delivery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    skill_tags VARCHAR(255) NOT NULL,
    study_progress TEXT NOT NULL,
    attachment_url VARCHAR(1024) NULL,
    delivery_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    audit_status TINYINT NOT NULL DEFAULT 0,
    audit_remark VARCHAR(255) NULL,
    audit_time DATETIME NULL,
    is_admitted TINYINT NOT NULL DEFAULT 0,
    admit_time DATETIME NULL,
    delivery_attempt_count INT NOT NULL DEFAULT 1,
    withdraw_count INT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_lab (user_id, lab_id),
    KEY idx_delivery_lab_deleted_user (lab_id, deleted, user_id),
    KEY idx_delivery_user_deleted_lab (user_id, deleted, lab_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_recruit_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    quota INT NOT NULL DEFAULT 0,
    requirement TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    created_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_recruit_plan_lab_status_time (lab_id, status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_lab_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    student_user_id BIGINT NOT NULL,
    recruit_plan_id BIGINT NULL,
    apply_reason TEXT NOT NULL,
    research_interest TEXT NULL,
    skill_summary TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'submitted',
    audit_by BIGINT NULL,
    audit_time DATETIME NULL,
    audit_comment VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_lab_apply_lab_status_time (lab_id, status, create_time),
    KEY idx_lab_apply_user_status_time (student_user_id, status, create_time),
    KEY idx_lab_apply_plan_status (recruit_plan_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_lab_create_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    applicant_user_id BIGINT NOT NULL,
    college_id BIGINT NOT NULL,
    lab_name VARCHAR(120) NOT NULL,
    teacher_name VARCHAR(64) NOT NULL,
    location VARCHAR(128) NULL,
    contact_email VARCHAR(128) NULL,
    research_direction TEXT NULL,
    apply_reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'submitted',
    college_audit_by BIGINT NULL,
    college_audit_time DATETIME NULL,
    college_audit_comment VARCHAR(255) NULL,
    school_audit_by BIGINT NULL,
    school_audit_time DATETIME NULL,
    school_audit_comment VARCHAR(255) NULL,
    generated_lab_id BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_lab_create_apply_applicant_status (applicant_user_id, status, create_time),
    KEY idx_lab_create_apply_college_status (college_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_teacher_register_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_no VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    college_id BIGINT NOT NULL,
    title VARCHAR(50) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(100) NOT NULL,
    apply_reason VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'submitted',
    college_audit_by BIGINT NULL,
    college_audit_time DATETIME NULL,
    college_audit_comment VARCHAR(255) NULL,
    school_audit_by BIGINT NULL,
    school_audit_time DATETIME NULL,
    school_audit_comment VARCHAR(255) NULL,
    generated_user_id BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_teacher_register_apply_lookup (teacher_no, email, status, deleted),
    KEY idx_teacher_register_apply_college (college_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_lab_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(32) NOT NULL DEFAULT 'member',
    join_date DATE NULL,
    quit_date DATE NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    appointed_by BIGINT NULL,
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_lab_member_lab_user_status (lab_id, user_id, status),
    KEY idx_lab_member_lab_status_deleted (lab_id, status, deleted, id),
    KEY idx_lab_member_user_status_deleted (user_id, status, deleted, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_lab_space_folder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    parent_id BIGINT NULL DEFAULT 0,
    folder_name VARCHAR(120) NOT NULL,
    category VARCHAR(50) NULL,
    sort_order INT NOT NULL DEFAULT 10,
    access_scope VARCHAR(32) NOT NULL DEFAULT 'lab',
    archived TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_lab_space_folder_lab_parent (lab_id, parent_id, deleted),
    KEY idx_lab_space_folder_category (lab_id, category, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_lab_space_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(1024) NOT NULL,
    file_size BIGINT NULL,
    file_type VARCHAR(100) NULL,
    archive_flag TINYINT NOT NULL DEFAULT 0,
    access_scope VARCHAR(32) NOT NULL DEFAULT 'lab',
    version_no INT NOT NULL DEFAULT 1,
    upload_user_id BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_lab_space_file_scope (lab_id, folder_id, archive_flag),
    KEY idx_lab_space_file_user (upload_user_id, deleted, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_lab_attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    attendance_date VARCHAR(20) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    reason VARCHAR(255) NULL,
    confirmed_by BIGINT NULL,
    confirm_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_lab_attendance (lab_id, user_id, attendance_date),
    KEY idx_lab_attendance_user_time (user_id, attendance_date, deleted),
    KEY idx_lab_attendance_lab_status (lab_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_lab_exit_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    audit_remark VARCHAR(255) NULL,
    audit_by BIGINT NULL,
    audit_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_lab_exit_application_lab_status (lab_id, status, create_time),
    KEY idx_lab_exit_application_user_status (user_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(128) NOT NULL,
    content LONGTEXT NOT NULL,
    publish_scope VARCHAR(32) NOT NULL DEFAULT 'school',
    college_id BIGINT NULL,
    lab_id BIGINT NULL,
    publisher_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_notice_deleted_scope_status_time (deleted, publish_scope, status, publish_time, id),
    KEY idx_notice_deleted_college_status_time (deleted, college_id, status, publish_time, id),
    KEY idx_notice_deleted_lab_status_time (deleted, lab_id, status, publish_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_notice');
SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_notice' AND index_name = 'ft_notice_title_content');
SET @ngram_exists = (SELECT COUNT(*) FROM information_schema.plugins WHERE plugin_name = 'ngram' AND plugin_status = 'ACTIVE');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    IF(@ngram_exists > 0,
        'CREATE FULLTEXT INDEX ft_notice_title_content ON t_notice (title, content) WITH PARSER ngram',
        'CREATE FULLTEXT INDEX ft_notice_title_content ON t_notice (title, content)'
    ),
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================================================
-- END V1__search_and_list_indexes.sql
-- ==================================================

-- ==================================================
-- BEGIN V2__student_profile_scope.sql
-- ==================================================

CREATE TABLE IF NOT EXISTS t_student_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    lab_id BIGINT NULL,
    student_no VARCHAR(64) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    gender VARCHAR(16) NULL,
    college_id BIGINT NULL,
    major VARCHAR(128) NULL,
    class_name VARCHAR(128) NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    direction VARCHAR(128) NULL,
    introduction TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    current_version INT NOT NULL DEFAULT 0,
    submitted_at DATETIME NULL,
    last_review_time DATETIME NULL,
    created_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_student_profile_user (user_id),
    KEY idx_student_profile_lab (lab_id, status, create_time),
    KEY idx_student_profile_college (college_id, status, create_time),
    KEY idx_student_profile_student_no (student_no),
    KEY idx_student_profile_status (status)
);

CREATE TABLE IF NOT EXISTS t_student_profile_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    reviewer_id BIGINT NULL,
    review_status VARCHAR(32) NOT NULL,
    review_comment VARCHAR(500) NULL,
    review_snapshot LONGTEXT NOT NULL,
    review_time DATETIME NULL,
    created_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_student_profile_review_version (profile_id, version_no),
    KEY idx_student_profile_review_status (review_status, create_time),
    KEY idx_student_profile_review_profile (profile_id, create_time)
);

CREATE TABLE IF NOT EXISTS t_student_profile_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    archive_snapshot LONGTEXT NOT NULL,
    archived_by BIGINT NULL,
    archived_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_student_profile_archive_version (profile_id, version_no),
    KEY idx_student_profile_archive_profile (profile_id, archived_at)
);

-- ==================================================
-- END V2__student_profile_scope.sql
-- ==================================================

-- ==================================================
-- BEGIN V3__equipment_management_upgrade.sql
-- ==================================================

CREATE TABLE IF NOT EXISTS t_equipment_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_equipment_category_lab (lab_id, deleted),
    KEY idx_equipment_category_name (name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_equipment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(100) NOT NULL DEFAULT 'General',
    device_code VARCHAR(64) NULL,
    serial_number VARCHAR(100) NULL,
    brand VARCHAR(64) NULL,
    model VARCHAR(64) NULL,
    purchase_date DATE NULL,
    location VARCHAR(128) NULL,
    image_url VARCHAR(255) NULL,
    description TEXT NULL,
    remark VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_equipment_lab_category_status (lab_id, category_id, status, deleted),
    KEY idx_equipment_device_code (device_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_equipment_borrow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    equipment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    borrow_time DATETIME NULL,
    return_time DATETIME NULL,
    expected_return_time DATETIME NULL,
    pickup_time DATETIME NULL,
    pickup_confirmed_by BIGINT NULL,
    return_apply_time DATETIME NULL,
    return_confirmed_by BIGINT NULL,
    return_confirm_time DATETIME NULL,
    acceptance_checklist TEXT NULL,
    reason VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_equipment_borrow_equipment_status_time (equipment_id, status, deleted, create_time, id),
    KEY idx_equipment_borrow_user_status_time (user_id, status, deleted, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_equipment_maintenance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    equipment_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    report_user_id BIGINT NULL,
    issue_desc VARCHAR(500) NOT NULL,
    maintenance_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    result_desc VARCHAR(500) NULL,
    handled_by BIGINT NULL,
    handled_at DATETIME NULL,
    created_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_equipment_maintenance_lab (lab_id, maintenance_status, create_time),
    KEY idx_equipment_maintenance_equipment (equipment_id, maintenance_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'category_id'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment ADD COLUMN category_id BIGINT NULL AFTER lab_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'device_code'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment ADD COLUMN device_code VARCHAR(64) NULL AFTER type',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'brand'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment ADD COLUMN brand VARCHAR(64) NULL AFTER serial_number',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'model'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment ADD COLUMN model VARCHAR(64) NULL AFTER brand',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'purchase_date'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment ADD COLUMN purchase_date DATE NULL AFTER model',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'location'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment ADD COLUMN location VARCHAR(128) NULL AFTER purchase_date',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'remark'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment ADD COLUMN remark VARCHAR(255) NULL AFTER description',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'created_by'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment ADD COLUMN created_by BIGINT NULL AFTER status',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'updated_by'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment ADD COLUMN updated_by BIGINT NULL AFTER created_by',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND column_name = 'expected_return_time'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment_borrow ADD COLUMN expected_return_time DATETIME NULL AFTER return_time',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND column_name = 'pickup_time'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment_borrow ADD COLUMN pickup_time DATETIME NULL AFTER expected_return_time',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND column_name = 'pickup_confirmed_by'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment_borrow ADD COLUMN pickup_confirmed_by BIGINT NULL AFTER pickup_time',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND column_name = 'return_apply_time'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment_borrow ADD COLUMN return_apply_time DATETIME NULL AFTER pickup_confirmed_by',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND column_name = 'return_confirmed_by'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment_borrow ADD COLUMN return_confirmed_by BIGINT NULL AFTER return_apply_time',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND column_name = 'return_confirm_time'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment_borrow ADD COLUMN return_confirm_time DATETIME NULL AFTER return_confirmed_by',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND column_name = 'acceptance_checklist'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_equipment_borrow ADD COLUMN acceptance_checklist TEXT NULL AFTER return_confirm_time',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE t_equipment
SET type = 'General'
WHERE type IS NULL OR type = '';

UPDATE t_equipment
SET device_code = COALESCE(NULLIF(serial_number, ''), CONCAT('EQ-', LPAD(id, 6, '0')))
WHERE device_code IS NULL OR device_code = '';

ALTER TABLE t_equipment
    MODIFY COLUMN type VARCHAR(100) NOT NULL,
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0;

-- ==================================================
-- END V3__equipment_management_upgrade.sql
-- ==================================================

-- ==================================================
-- BEGIN V4__audit_notification_center.sql
-- ==================================================

CREATE TABLE IF NOT EXISTS t_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    operator_role VARCHAR(64) NULL,
    college_id BIGINT NULL,
    lab_id BIGINT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id BIGINT NULL,
    detail TEXT NULL,
    request_path VARCHAR(255) NULL,
    request_method VARCHAR(16) NULL,
    request_ip VARCHAR(64) NULL,
    result VARCHAR(32) NULL,
    detail_json LONGTEXT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_audit_log_action_time (action, create_time),
    KEY idx_audit_log_target (target_type, target_id, create_time),
    KEY idx_audit_log_scope_time (college_id, lab_id, create_time),
    KEY idx_audit_log_actor_time (actor_user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_system_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    related_id BIGINT NULL,
    redirect_path VARCHAR(255) NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_system_notification_user_read_time (user_id, is_read, create_time),
    KEY idx_system_notification_type_time (notification_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_user_identity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    identity_type VARCHAR(32) NOT NULL,
    college_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_identity_user_type_deleted (user_id, identity_type, deleted),
    KEY idx_user_identity_user_status (user_id, status, deleted),
    KEY idx_user_identity_college_status (college_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_platform_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_code VARCHAR(64) NOT NULL,
    college_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time DATETIME NULL,
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    unique_scope VARCHAR(128) GENERATED ALWAYS AS (
        CASE
            WHEN deleted = 0 AND status = 'active' THEN
                CASE
                    WHEN post_code = 'SCHOOL_DIRECTOR' THEN 'SCHOOL_DIRECTOR'
                    ELSE CONCAT(post_code, '#', COALESCE(CAST(college_id AS CHAR), 'GLOBAL'))
                END
            ELSE NULL
        END
    ) STORED,
    UNIQUE KEY uk_platform_post_active_scope (unique_scope),
    KEY idx_platform_post_user_status (user_id, status, deleted),
    KEY idx_platform_post_code_college_status (post_code, college_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_lab_teacher_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    is_primary TINYINT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_lab_teacher_relation (lab_id, user_id, deleted),
    KEY idx_lab_teacher_relation_user (user_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_audit_log' AND column_name = 'operator_role'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_audit_log ADD COLUMN operator_role VARCHAR(64) NULL AFTER actor_user_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_audit_log' AND column_name = 'college_id'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_audit_log ADD COLUMN college_id BIGINT NULL AFTER operator_role',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_audit_log' AND column_name = 'lab_id'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_audit_log ADD COLUMN lab_id BIGINT NULL AFTER college_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_audit_log' AND column_name = 'request_path'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_audit_log ADD COLUMN request_path VARCHAR(255) NULL AFTER detail',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_audit_log' AND column_name = 'request_method'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_audit_log ADD COLUMN request_method VARCHAR(16) NULL AFTER request_path',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_audit_log' AND column_name = 'request_ip'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_audit_log ADD COLUMN request_ip VARCHAR(64) NULL AFTER request_method',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_audit_log' AND column_name = 'result'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_audit_log ADD COLUMN result VARCHAR(32) NULL AFTER request_ip',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_audit_log' AND column_name = 'detail_json'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_audit_log ADD COLUMN detail_json LONGTEXT NULL AFTER result',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_system_notification' AND column_name = 'redirect_path'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_system_notification ADD COLUMN redirect_path VARCHAR(255) NULL AFTER related_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================================================
-- END V4__audit_notification_center.sql
-- ==================================================

-- ==================================================
-- BEGIN V5__attendance_leave_and_change_log.sql
-- ==================================================

CREATE TABLE IF NOT EXISTS t_attendance_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    college_id BIGINT NOT NULL,
    semester_name VARCHAR(64) NOT NULL,
    task_name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    published_by BIGINT NULL,
    published_time DATETIME NULL,
    created_by BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    active_scope VARCHAR(128) GENERATED ALWAYS AS (
        CASE
            WHEN deleted = 0 AND status IN ('draft', 'published')
                THEN CONCAT(CAST(college_id AS CHAR), '#', semester_name)
            ELSE NULL
        END
    ) STORED,
    UNIQUE KEY uk_attendance_task_active_scope (active_scope),
    KEY idx_attendance_task_college_status (college_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_attendance_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    week_day TINYINT NOT NULL,
    sign_in_start TIME NOT NULL,
    sign_in_end TIME NOT NULL,
    late_threshold_minutes INT NOT NULL DEFAULT 15,
    sign_code_length INT NOT NULL DEFAULT 4,
    code_ttl_minutes INT NOT NULL DEFAULT 90,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_attendance_schedule_week_day (task_id, week_day, deleted),
    KEY idx_attendance_schedule_task_status (task_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_attendance_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    session_code VARCHAR(12) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    sign_start_time DATETIME NOT NULL,
    sign_end_time DATETIME NOT NULL,
    late_time DATETIME NOT NULL,
    code_expire_time DATETIME NOT NULL,
    generated_by BIGINT NULL,
    publish_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_attendance_session_lab_date (lab_id, session_date, deleted),
    KEY idx_attendance_session_task_status (task_id, session_date, status, deleted),
    KEY idx_attendance_session_lab_status_date (lab_id, status, session_date, deleted, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_attendance_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sign_status VARCHAR(32) NOT NULL DEFAULT 'normal',
    sign_code VARCHAR(12) NULL,
    sign_time DATETIME NULL,
    remark VARCHAR(255) NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'student',
    reviewed_by BIGINT NULL,
    review_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_attendance_record_session_user (session_id, user_id, deleted),
    KEY idx_attendance_record_lab_status (lab_id, sign_status, deleted),
    KEY idx_attendance_record_user_time (user_id, deleted, sign_time),
    KEY idx_attendance_record_session_status (session_id, deleted, sign_status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_attendance_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    photo_url VARCHAR(1024) NOT NULL,
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_attendance_photo_session (session_id, deleted),
    KEY idx_attendance_photo_lab (lab_id, deleted, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_attendance_duty (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    college_id BIGINT NOT NULL,
    duty_admin_user_id BIGINT NOT NULL,
    backup_admin_user_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_attendance_duty_session (session_id, deleted),
    KEY idx_attendance_duty_task (task_id, college_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_attendance_leave (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    leave_reason VARCHAR(255) NOT NULL,
    leave_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    review_comment VARCHAR(255) NULL,
    reviewed_by BIGINT NULL,
    review_time DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_attendance_leave_session_user_deleted (session_id, user_id, deleted),
    KEY idx_attendance_leave_lab_status_time (lab_id, leave_status, deleted, created_at),
    KEY idx_attendance_leave_user_status_time (user_id, leave_status, deleted, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_attendance_change_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    record_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    before_status VARCHAR(32) NULL,
    after_status VARCHAR(32) NOT NULL,
    changed_by BIGINT NOT NULL,
    changed_reason VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_attendance_change_log_record_time (record_id, deleted, created_at),
    KEY idx_attendance_change_log_lab_time (lab_id, deleted, created_at),
    KEY idx_attendance_change_log_user_time (user_id, deleted, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================================================
-- END V5__attendance_leave_and_change_log.sql
-- ==================================================

-- ==================================================
-- BEGIN V6__search_keyword_hot.sql
-- ==================================================

CREATE TABLE IF NOT EXISTS t_search_keyword_hot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    keyword VARCHAR(128) NOT NULL,
    scope_level VARCHAR(32) NOT NULL,
    college_id BIGINT NULL,
    lab_id BIGINT NULL,
    search_count BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_search_keyword_scope (keyword, scope_level, college_id, lab_id, deleted),
    KEY idx_search_keyword_hot_count (search_count, updated_at),
    KEY idx_search_keyword_hot_scope (scope_level, college_id, lab_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================================================
-- END V6__search_keyword_hot.sql
-- ==================================================

-- ==================================================
-- BEGIN V7__file_object_and_relation.sql
-- ==================================================

CREATE TABLE IF NOT EXISTS t_file_object (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    md5 VARCHAR(32) NULL,
    uploaded_by BIGINT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_file_object_uploaded_time (uploaded_at, deleted),
    KEY idx_file_object_uploader_time (uploaded_by, uploaded_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_business_file_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_business_file_relation (business_type, business_id, file_id, deleted),
    KEY idx_business_file_relation_scope (business_type, business_id, deleted),
    KEY idx_business_file_relation_file (file_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================================================
-- END V7__file_object_and_relation.sql
-- ==================================================

-- ==================================================
-- BEGIN V8__phase2_core_indexes.sql
-- ==================================================

SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_user');
SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_user' AND index_name = 'idx_user_lab_role_status_create');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE INDEX idx_user_lab_role_status_create ON t_user (lab_id, role, status, deleted, create_time, id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_lab_member');
SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_lab_member' AND index_name = 'idx_lab_member_lab_status_deleted');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE INDEX idx_lab_member_lab_status_deleted ON t_lab_member (lab_id, status, deleted, id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_attendance_session');
SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_attendance_session' AND index_name = 'idx_attendance_session_lab_status_date');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE INDEX idx_attendance_session_lab_status_date ON t_attendance_session (lab_id, status, session_date, deleted, id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_attendance_record');
SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_attendance_record' AND index_name = 'idx_attendance_record_session_status');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE INDEX idx_attendance_record_session_status ON t_attendance_record (session_id, deleted, sign_status, id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow');
SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND index_name = 'idx_equipment_borrow_user_status_time');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE INDEX idx_equipment_borrow_user_status_time ON t_equipment_borrow (user_id, status, deleted, create_time, id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow');
SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND index_name = 'idx_equipment_borrow_equipment_status_time');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'CREATE INDEX idx_equipment_borrow_equipment_status_time ON t_equipment_borrow (equipment_id, status, deleted, create_time, id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================================================
-- END V8__phase2_core_indexes.sql
-- ==================================================

-- ==================================================
-- BEGIN V9__restore_foreign_keys.sql
-- ==================================================

UPDATE t_lab l
LEFT JOIN t_college c ON c.id = l.college_id
SET l.college_id = NULL
WHERE l.college_id IS NOT NULL AND c.id IS NULL;

UPDATE t_user u
LEFT JOIN t_lab l ON l.id = u.lab_id
SET u.lab_id = NULL
WHERE u.lab_id IS NOT NULL AND l.id IS NULL;

DELETE d
FROM t_delivery d
LEFT JOIN t_user u ON u.id = d.user_id
WHERE u.id IS NULL;

DELETE d
FROM t_delivery d
LEFT JOIN t_lab l ON l.id = d.lab_id
WHERE l.id IS NULL;

DELETE rp
FROM t_recruit_plan rp
LEFT JOIN t_lab l ON l.id = rp.lab_id
WHERE l.id IS NULL;

UPDATE t_lab_apply la
LEFT JOIN t_recruit_plan rp ON rp.id = la.recruit_plan_id
SET la.recruit_plan_id = NULL
WHERE la.recruit_plan_id IS NOT NULL AND rp.id IS NULL;

DELETE la
FROM t_lab_apply la
LEFT JOIN t_lab l ON l.id = la.lab_id
WHERE l.id IS NULL;

DELETE la
FROM t_lab_apply la
LEFT JOIN t_user u ON u.id = la.student_user_id
WHERE u.id IS NULL;

DELETE lca
FROM t_lab_create_apply lca
LEFT JOIN t_user u ON u.id = lca.applicant_user_id
WHERE u.id IS NULL;

DELETE lca
FROM t_lab_create_apply lca
LEFT JOIN t_college c ON c.id = lca.college_id
WHERE c.id IS NULL;

DELETE lm
FROM t_lab_member lm
LEFT JOIN t_lab l ON l.id = lm.lab_id
WHERE l.id IS NULL;

DELETE lm
FROM t_lab_member lm
LEFT JOIN t_user u ON u.id = lm.user_id
WHERE u.id IS NULL;

DELETE lsf
FROM t_lab_space_folder lsf
LEFT JOIN t_lab l ON l.id = lsf.lab_id
WHERE l.id IS NULL;

UPDATE t_lab_space_file lsf
LEFT JOIN t_user u ON u.id = lsf.upload_user_id
SET lsf.upload_user_id = NULL
WHERE lsf.upload_user_id IS NOT NULL AND u.id IS NULL;

DELETE lsf
FROM t_lab_space_file lsf
LEFT JOIN t_lab l ON l.id = lsf.lab_id
WHERE l.id IS NULL;

DELETE lsf
FROM t_lab_space_file lsf
LEFT JOIN t_lab_space_folder folder ON folder.id = lsf.folder_id
WHERE folder.id IS NULL;

DELETE la
FROM t_lab_attendance la
LEFT JOIN t_lab l ON l.id = la.lab_id
WHERE l.id IS NULL;

DELETE la
FROM t_lab_attendance la
LEFT JOIN t_user u ON u.id = la.user_id
WHERE u.id IS NULL;

DELETE lea
FROM t_lab_exit_application lea
LEFT JOIN t_lab l ON l.id = lea.lab_id
WHERE l.id IS NULL;

DELETE lea
FROM t_lab_exit_application lea
LEFT JOIN t_user u ON u.id = lea.user_id
WHERE u.id IS NULL;

DELETE n
FROM t_notice n
LEFT JOIN t_user u ON u.id = n.publisher_id
WHERE u.id IS NULL;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab' AND column_name = 'college_id'
      AND referenced_table_name = 't_college' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab ADD CONSTRAINT fk_lab_college FOREIGN KEY (college_id) REFERENCES t_college(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_user' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_user ADD CONSTRAINT fk_user_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_delivery' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_delivery ADD CONSTRAINT fk_delivery_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_delivery' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_delivery ADD CONSTRAINT fk_delivery_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_recruit_plan' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_recruit_plan ADD CONSTRAINT fk_recruit_plan_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_apply' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_apply ADD CONSTRAINT fk_lab_apply_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_apply' AND column_name = 'student_user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_apply ADD CONSTRAINT fk_lab_apply_student_user FOREIGN KEY (student_user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_apply' AND column_name = 'recruit_plan_id'
      AND referenced_table_name = 't_recruit_plan' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_apply ADD CONSTRAINT fk_lab_apply_recruit_plan FOREIGN KEY (recruit_plan_id) REFERENCES t_recruit_plan(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_create_apply' AND column_name = 'applicant_user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_create_apply ADD CONSTRAINT fk_lab_create_apply_user FOREIGN KEY (applicant_user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_create_apply' AND column_name = 'college_id'
      AND referenced_table_name = 't_college' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_create_apply ADD CONSTRAINT fk_lab_create_apply_college FOREIGN KEY (college_id) REFERENCES t_college(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_member' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_member ADD CONSTRAINT fk_lab_member_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_member' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_member ADD CONSTRAINT fk_lab_member_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_space_folder' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_space_folder ADD CONSTRAINT fk_lab_space_folder_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_space_file' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_space_file ADD CONSTRAINT fk_lab_space_file_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_space_file' AND column_name = 'folder_id'
      AND referenced_table_name = 't_lab_space_folder' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_space_file ADD CONSTRAINT fk_lab_space_file_folder FOREIGN KEY (folder_id) REFERENCES t_lab_space_folder(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_space_file' AND column_name = 'upload_user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_space_file ADD CONSTRAINT fk_lab_space_file_upload_user FOREIGN KEY (upload_user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_attendance' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_attendance ADD CONSTRAINT fk_lab_attendance_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_attendance' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_attendance ADD CONSTRAINT fk_lab_attendance_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_exit_application' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_exit_application ADD CONSTRAINT fk_lab_exit_application_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_exit_application' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_exit_application ADD CONSTRAINT fk_lab_exit_application_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_notice' AND column_name = 'publisher_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_notice ADD CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

DELETE sp
FROM t_student_profile sp
LEFT JOIN t_user u ON u.id = sp.user_id
WHERE u.id IS NULL;

UPDATE t_student_profile sp
LEFT JOIN t_lab l ON l.id = sp.lab_id
SET sp.lab_id = NULL
WHERE sp.lab_id IS NOT NULL AND l.id IS NULL;

UPDATE t_student_profile sp
LEFT JOIN t_college c ON c.id = sp.college_id
SET sp.college_id = NULL
WHERE sp.college_id IS NOT NULL AND c.id IS NULL;

DELETE spr
FROM t_student_profile_review spr
LEFT JOIN t_student_profile sp ON sp.id = spr.profile_id
WHERE sp.id IS NULL;

UPDATE t_student_profile_review spr
LEFT JOIN t_user u ON u.id = spr.reviewer_id
SET spr.reviewer_id = NULL
WHERE spr.reviewer_id IS NOT NULL AND u.id IS NULL;

DELETE spa
FROM t_student_profile_archive spa
LEFT JOIN t_student_profile sp ON sp.id = spa.profile_id
WHERE sp.id IS NULL;

UPDATE t_student_profile_archive spa
LEFT JOIN t_user u ON u.id = spa.archived_by
SET spa.archived_by = NULL
WHERE spa.archived_by IS NOT NULL AND u.id IS NULL;

DELETE ec
FROM t_equipment_category ec
LEFT JOIN t_lab l ON l.id = ec.lab_id
WHERE l.id IS NULL;

UPDATE t_equipment e
LEFT JOIN t_equipment_category ec ON ec.id = e.category_id
SET e.category_id = NULL
WHERE e.category_id IS NOT NULL AND ec.id IS NULL;

DELETE e
FROM t_equipment e
LEFT JOIN t_lab l ON l.id = e.lab_id
WHERE l.id IS NULL;

DELETE eb
FROM t_equipment_borrow eb
LEFT JOIN t_equipment e ON e.id = eb.equipment_id
WHERE e.id IS NULL;

DELETE eb
FROM t_equipment_borrow eb
LEFT JOIN t_user u ON u.id = eb.user_id
WHERE u.id IS NULL;

DELETE em
FROM t_equipment_maintenance em
LEFT JOIN t_equipment e ON e.id = em.equipment_id
WHERE e.id IS NULL;

DELETE em
FROM t_equipment_maintenance em
LEFT JOIN t_lab l ON l.id = em.lab_id
WHERE l.id IS NULL;

DELETE sn
FROM t_system_notification sn
LEFT JOIN t_user u ON u.id = sn.user_id
WHERE u.id IS NULL;

DELETE ui
FROM t_user_identity ui
LEFT JOIN t_user u ON u.id = ui.user_id
WHERE u.id IS NULL;

DELETE pp
FROM t_platform_post pp
LEFT JOIN t_user u ON u.id = pp.user_id
WHERE u.id IS NULL;

DELETE ltr
FROM t_lab_teacher_relation ltr
LEFT JOIN t_lab l ON l.id = ltr.lab_id
WHERE l.id IS NULL;

DELETE ltr
FROM t_lab_teacher_relation ltr
LEFT JOIN t_user u ON u.id = ltr.user_id
WHERE u.id IS NULL;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile ADD CONSTRAINT fk_student_profile_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile ADD CONSTRAINT fk_student_profile_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile' AND column_name = 'college_id'
      AND referenced_table_name = 't_college' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile ADD CONSTRAINT fk_student_profile_college FOREIGN KEY (college_id) REFERENCES t_college(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile_review' AND column_name = 'profile_id'
      AND referenced_table_name = 't_student_profile' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile_review ADD CONSTRAINT fk_student_profile_review_profile FOREIGN KEY (profile_id) REFERENCES t_student_profile(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile_review' AND column_name = 'reviewer_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile_review ADD CONSTRAINT fk_student_profile_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile_archive' AND column_name = 'profile_id'
      AND referenced_table_name = 't_student_profile' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile_archive ADD CONSTRAINT fk_student_profile_archive_profile FOREIGN KEY (profile_id) REFERENCES t_student_profile(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile_archive' AND column_name = 'archived_by'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile_archive ADD CONSTRAINT fk_student_profile_archive_archived_by FOREIGN KEY (archived_by) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_category' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment_category ADD CONSTRAINT fk_equipment_category_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment ADD CONSTRAINT fk_equipment_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'category_id'
      AND referenced_table_name = 't_equipment_category' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment ADD CONSTRAINT fk_equipment_category FOREIGN KEY (category_id) REFERENCES t_equipment_category(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND column_name = 'equipment_id'
      AND referenced_table_name = 't_equipment' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment_borrow ADD CONSTRAINT fk_equipment_borrow_equipment FOREIGN KEY (equipment_id) REFERENCES t_equipment(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment_borrow ADD CONSTRAINT fk_equipment_borrow_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_maintenance' AND column_name = 'equipment_id'
      AND referenced_table_name = 't_equipment' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment_maintenance ADD CONSTRAINT fk_equipment_maintenance_equipment FOREIGN KEY (equipment_id) REFERENCES t_equipment(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_maintenance' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment_maintenance ADD CONSTRAINT fk_equipment_maintenance_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_system_notification' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_system_notification ADD CONSTRAINT fk_system_notification_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_user_identity' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_user_identity ADD CONSTRAINT fk_user_identity_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_platform_post' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_platform_post ADD CONSTRAINT fk_platform_post_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_teacher_relation' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_teacher_relation ADD CONSTRAINT fk_lab_teacher_relation_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_teacher_relation' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_teacher_relation ADD CONSTRAINT fk_lab_teacher_relation_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

DELETE att
FROM t_attendance_task att
LEFT JOIN t_college c ON c.id = att.college_id
WHERE c.id IS NULL;

DELETE att
FROM t_attendance_task att
LEFT JOIN t_user u ON u.id = att.created_by
WHERE u.id IS NULL;

DELETE ats
FROM t_attendance_schedule ats
LEFT JOIN t_attendance_task att ON att.id = ats.task_id
WHERE att.id IS NULL;

DELETE ase
FROM t_attendance_session ase
LEFT JOIN t_attendance_task att ON att.id = ase.task_id
WHERE att.id IS NULL;

DELETE ase
FROM t_attendance_session ase
LEFT JOIN t_attendance_schedule ats ON ats.id = ase.schedule_id
WHERE ats.id IS NULL;

DELETE ase
FROM t_attendance_session ase
LEFT JOIN t_lab l ON l.id = ase.lab_id
WHERE l.id IS NULL;

DELETE ar
FROM t_attendance_record ar
LEFT JOIN t_attendance_session ase ON ase.id = ar.session_id
WHERE ase.id IS NULL;

DELETE ar
FROM t_attendance_record ar
LEFT JOIN t_attendance_task att ON att.id = ar.task_id
WHERE att.id IS NULL;

DELETE ar
FROM t_attendance_record ar
LEFT JOIN t_lab l ON l.id = ar.lab_id
WHERE l.id IS NULL;

DELETE ar
FROM t_attendance_record ar
LEFT JOIN t_user u ON u.id = ar.user_id
WHERE u.id IS NULL;

DELETE ap
FROM t_attendance_photo ap
LEFT JOIN t_attendance_session ase ON ase.id = ap.session_id
WHERE ase.id IS NULL;

DELETE ap
FROM t_attendance_photo ap
LEFT JOIN t_lab l ON l.id = ap.lab_id
WHERE l.id IS NULL;

DELETE ap
FROM t_attendance_photo ap
LEFT JOIN t_user u ON u.id = ap.uploader_id
WHERE u.id IS NULL;

UPDATE t_attendance_duty ad
LEFT JOIN t_user backup_user ON backup_user.id = ad.backup_admin_user_id
SET ad.backup_admin_user_id = NULL
WHERE ad.backup_admin_user_id IS NOT NULL AND backup_user.id IS NULL;

DELETE ad
FROM t_attendance_duty ad
LEFT JOIN t_attendance_task att ON att.id = ad.task_id
WHERE att.id IS NULL;

DELETE ad
FROM t_attendance_duty ad
LEFT JOIN t_attendance_session ase ON ase.id = ad.session_id
WHERE ase.id IS NULL;

DELETE ad
FROM t_attendance_duty ad
LEFT JOIN t_college c ON c.id = ad.college_id
WHERE c.id IS NULL;

DELETE ad
FROM t_attendance_duty ad
LEFT JOIN t_user u ON u.id = ad.duty_admin_user_id
WHERE u.id IS NULL;

UPDATE t_attendance_leave al
LEFT JOIN t_user reviewer ON reviewer.id = al.reviewed_by
SET al.reviewed_by = NULL
WHERE al.reviewed_by IS NOT NULL AND reviewer.id IS NULL;

DELETE al
FROM t_attendance_leave al
LEFT JOIN t_attendance_session ase ON ase.id = al.session_id
WHERE ase.id IS NULL;

DELETE al
FROM t_attendance_leave al
LEFT JOIN t_attendance_task att ON att.id = al.task_id
WHERE att.id IS NULL;

DELETE al
FROM t_attendance_leave al
LEFT JOIN t_lab l ON l.id = al.lab_id
WHERE l.id IS NULL;

DELETE al
FROM t_attendance_leave al
LEFT JOIN t_user u ON u.id = al.user_id
WHERE u.id IS NULL;

DELETE acl
FROM t_attendance_change_log acl
LEFT JOIN t_attendance_session ase ON ase.id = acl.session_id
WHERE ase.id IS NULL;

DELETE acl
FROM t_attendance_change_log acl
LEFT JOIN t_attendance_record ar ON ar.id = acl.record_id
WHERE ar.id IS NULL;

DELETE acl
FROM t_attendance_change_log acl
LEFT JOIN t_lab l ON l.id = acl.lab_id
WHERE l.id IS NULL;

DELETE acl
FROM t_attendance_change_log acl
LEFT JOIN t_user u ON u.id = acl.user_id
WHERE u.id IS NULL;

DELETE bfr
FROM t_business_file_relation bfr
LEFT JOIN t_file_object fo ON fo.id = bfr.file_id
WHERE fo.id IS NULL;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_task' AND column_name = 'college_id'
      AND referenced_table_name = 't_college' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_task ADD CONSTRAINT fk_attendance_task_college FOREIGN KEY (college_id) REFERENCES t_college(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_task' AND column_name = 'created_by'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_task ADD CONSTRAINT fk_attendance_task_creator FOREIGN KEY (created_by) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_schedule' AND column_name = 'task_id'
      AND referenced_table_name = 't_attendance_task' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_schedule ADD CONSTRAINT fk_attendance_schedule_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_session' AND column_name = 'task_id'
      AND referenced_table_name = 't_attendance_task' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_session ADD CONSTRAINT fk_attendance_session_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_session' AND column_name = 'schedule_id'
      AND referenced_table_name = 't_attendance_schedule' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_session ADD CONSTRAINT fk_attendance_session_schedule FOREIGN KEY (schedule_id) REFERENCES t_attendance_schedule(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_session' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_session ADD CONSTRAINT fk_attendance_session_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_record' AND column_name = 'session_id'
      AND referenced_table_name = 't_attendance_session' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_record ADD CONSTRAINT fk_attendance_record_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_record' AND column_name = 'task_id'
      AND referenced_table_name = 't_attendance_task' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_record ADD CONSTRAINT fk_attendance_record_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_record' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_record ADD CONSTRAINT fk_attendance_record_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_record' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_record ADD CONSTRAINT fk_attendance_record_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_photo' AND column_name = 'session_id'
      AND referenced_table_name = 't_attendance_session' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_photo ADD CONSTRAINT fk_attendance_photo_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_photo' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_photo ADD CONSTRAINT fk_attendance_photo_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_photo' AND column_name = 'uploader_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_photo ADD CONSTRAINT fk_attendance_photo_uploader FOREIGN KEY (uploader_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_duty' AND column_name = 'task_id'
      AND referenced_table_name = 't_attendance_task' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_duty ADD CONSTRAINT fk_attendance_duty_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_duty' AND column_name = 'session_id'
      AND referenced_table_name = 't_attendance_session' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_duty ADD CONSTRAINT fk_attendance_duty_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_duty' AND column_name = 'college_id'
      AND referenced_table_name = 't_college' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_duty ADD CONSTRAINT fk_attendance_duty_college FOREIGN KEY (college_id) REFERENCES t_college(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_duty' AND column_name = 'duty_admin_user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_duty ADD CONSTRAINT fk_attendance_duty_admin FOREIGN KEY (duty_admin_user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_leave' AND column_name = 'session_id'
      AND referenced_table_name = 't_attendance_session' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_leave ADD CONSTRAINT fk_attendance_leave_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_leave' AND column_name = 'task_id'
      AND referenced_table_name = 't_attendance_task' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_leave ADD CONSTRAINT fk_attendance_leave_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_leave' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_leave ADD CONSTRAINT fk_attendance_leave_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_leave' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_leave ADD CONSTRAINT fk_attendance_leave_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_change_log' AND column_name = 'session_id'
      AND referenced_table_name = 't_attendance_session' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_change_log ADD CONSTRAINT fk_attendance_change_log_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_change_log' AND column_name = 'record_id'
      AND referenced_table_name = 't_attendance_record' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_change_log ADD CONSTRAINT fk_attendance_change_log_record FOREIGN KEY (record_id) REFERENCES t_attendance_record(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_change_log' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_change_log ADD CONSTRAINT fk_attendance_change_log_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_change_log' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_change_log ADD CONSTRAINT fk_attendance_change_log_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_business_file_relation' AND column_name = 'file_id'
      AND referenced_table_name = 't_file_object' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_business_file_relation ADD CONSTRAINT fk_business_file_relation_file FOREIGN KEY (file_id) REFERENCES t_file_object(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================================================
-- END V9__restore_foreign_keys.sql
-- ==================================================

