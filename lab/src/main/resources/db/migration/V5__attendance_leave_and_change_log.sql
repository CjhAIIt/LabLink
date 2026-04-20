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
