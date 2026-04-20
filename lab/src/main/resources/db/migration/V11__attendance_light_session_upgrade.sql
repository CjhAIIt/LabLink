SET @db_name = DATABASE();

-- Upgrade t_lab_attendance with lightweight attendance fields.
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_lab_attendance'
              AND COLUMN_NAME = 'session_id'
        ),
        'SELECT 1',
        'ALTER TABLE t_lab_attendance ADD COLUMN session_id BIGINT NULL COMMENT ''attendance session id'' AFTER user_id'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_lab_attendance'
              AND COLUMN_NAME = 'checkin_time'
        ),
        'SELECT 1',
        'ALTER TABLE t_lab_attendance ADD COLUMN checkin_time DATETIME NULL COMMENT ''check in time'' AFTER attendance_date'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_lab_attendance'
              AND COLUMN_NAME = 'tag_type'
        ),
        'SELECT 1',
        'ALTER TABLE t_lab_attendance ADD COLUMN tag_type VARCHAR(32) NULL COMMENT ''leave or forgot tag'' AFTER status'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_lab_attendance'
              AND COLUMN_NAME = 'export_flag'
        ),
        'SELECT 1',
        'ALTER TABLE t_lab_attendance ADD COLUMN export_flag TINYINT NOT NULL DEFAULT 0 COMMENT ''exported flag'' AFTER confirm_time'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure the legacy session table exists for both scheduled attendance and lightweight sessions.
CREATE TABLE IF NOT EXISTS t_attendance_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NULL,
    schedule_id BIGINT NULL,
    lab_id BIGINT NOT NULL,
    session_date DATE NULL,
    session_code VARCHAR(12) NULL,
    session_no VARCHAR(64) NULL COMMENT 'lightweight session number',
    sign_code VARCHAR(16) NULL COMMENT 'lightweight sign code',
    qr_code_content VARCHAR(255) NULL COMMENT 'qr payload',
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    sign_start_time DATETIME NULL,
    sign_end_time DATETIME NULL,
    late_time DATETIME NULL,
    code_expire_time DATETIME NULL,
    start_time DATETIME NULL COMMENT 'lightweight session start time',
    expire_time DATETIME NULL COMMENT 'lightweight session expire time',
    duration_seconds INT NOT NULL DEFAULT 60 COMMENT 'lightweight session ttl',
    created_by BIGINT NULL COMMENT 'lightweight session creator',
    generated_by BIGINT NULL,
    publish_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_attendance_session_lab_date (lab_id, session_date, deleted),
    KEY idx_attendance_session_task_status (task_id, session_date, status, deleted),
    KEY idx_attendance_session_lab_status_date (lab_id, status, session_date, deleted, id),
    UNIQUE KEY uk_session_no (session_no),
    KEY idx_lab_status (lab_id, status),
    KEY idx_expire_time (expire_time),
    KEY idx_sign_code_status (sign_code, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='attendance session';

-- Relax legacy columns so lightweight sessions can be stored without task metadata.
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
        ),
        'ALTER TABLE t_attendance_session MODIFY COLUMN task_id BIGINT NULL, MODIFY COLUMN schedule_id BIGINT NULL, MODIFY COLUMN session_date DATE NULL, MODIFY COLUMN session_code VARCHAR(12) NULL, MODIFY COLUMN sign_start_time DATETIME NULL, MODIFY COLUMN sign_end_time DATETIME NULL, MODIFY COLUMN late_time DATETIME NULL, MODIFY COLUMN code_expire_time DATETIME NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
              AND COLUMN_NAME = 'session_no'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session ADD COLUMN session_no VARCHAR(64) NULL COMMENT ''lightweight session number'' AFTER session_code'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
              AND COLUMN_NAME = 'sign_code'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session ADD COLUMN sign_code VARCHAR(16) NULL COMMENT ''lightweight sign code'' AFTER session_no'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
              AND COLUMN_NAME = 'qr_code_content'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session ADD COLUMN qr_code_content VARCHAR(255) NULL COMMENT ''qr payload'' AFTER sign_code'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
              AND COLUMN_NAME = 'start_time'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session ADD COLUMN start_time DATETIME NULL COMMENT ''lightweight session start time'' AFTER code_expire_time'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
              AND COLUMN_NAME = 'expire_time'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session ADD COLUMN expire_time DATETIME NULL COMMENT ''lightweight session expire time'' AFTER start_time'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
              AND COLUMN_NAME = 'duration_seconds'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session ADD COLUMN duration_seconds INT NOT NULL DEFAULT 60 COMMENT ''lightweight session ttl'' AFTER expire_time'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
              AND COLUMN_NAME = 'created_by'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session ADD COLUMN created_by BIGINT NULL COMMENT ''lightweight session creator'' AFTER duration_seconds'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Store per-session realtime sign records.
CREATE TABLE IF NOT EXISTS t_attendance_session_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL COMMENT 'attendance session id',
    lab_id BIGINT NOT NULL COMMENT 'lab id',
    user_id BIGINT NOT NULL COMMENT 'user id',
    sign_time DATETIME NOT NULL COMMENT 'sign time',
    sign_method VARCHAR(20) NOT NULL DEFAULT 'code' COMMENT 'code or qr',
    sign_code_snapshot VARCHAR(16) NULL COMMENT 'sign code snapshot',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_session_user (session_id, user_id),
    KEY idx_lab_user (lab_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='lightweight attendance session record';
