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
