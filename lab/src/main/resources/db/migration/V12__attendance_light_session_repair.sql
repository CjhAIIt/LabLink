SET @db_name = DATABASE();

-- Repair t_lab_attendance columns.
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

-- Relax legacy session columns so lightweight sessions can be inserted without task/schedule data.
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

-- Repair lightweight session columns.
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
        'ALTER TABLE t_attendance_session ADD COLUMN session_no VARCHAR(64) NULL COMMENT ''lightweight session no'' AFTER session_code'
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
        'ALTER TABLE t_attendance_session ADD COLUMN start_time DATETIME NULL COMMENT ''session start time'' AFTER code_expire_time'
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
        'ALTER TABLE t_attendance_session ADD COLUMN expire_time DATETIME NULL COMMENT ''session expire time'' AFTER start_time'
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
        'ALTER TABLE t_attendance_session ADD COLUMN duration_seconds INT NOT NULL DEFAULT 60 COMMENT ''session ttl seconds'' AFTER expire_time'
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
        'ALTER TABLE t_attendance_session ADD COLUMN created_by BIGINT NULL COMMENT ''session creator'' AFTER duration_seconds'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create record table when absent.
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

-- Repair record table columns when the table already exists with partial structure.
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session_record'
              AND COLUMN_NAME = 'sign_method'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session_record ADD COLUMN sign_method VARCHAR(20) NOT NULL DEFAULT ''code'' COMMENT ''code or qr'' AFTER sign_time'
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
              AND TABLE_NAME = 't_attendance_session_record'
              AND COLUMN_NAME = 'sign_code_snapshot'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session_record ADD COLUMN sign_code_snapshot VARCHAR(16) NULL COMMENT ''sign code snapshot'' AFTER sign_method'
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
              AND TABLE_NAME = 't_attendance_session_record'
              AND COLUMN_NAME = 'create_time'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session_record ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER sign_code_snapshot'
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
              AND TABLE_NAME = 't_attendance_session_record'
              AND COLUMN_NAME = 'deleted'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session_record ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 AFTER create_time'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Repair indexes.
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
              AND INDEX_NAME = 'uk_session_no'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session ADD UNIQUE KEY uk_session_no (session_no)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
              AND INDEX_NAME = 'idx_lab_status'
        ),
        'SELECT 1',
        'CREATE INDEX idx_lab_status ON t_attendance_session (lab_id, status)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
              AND INDEX_NAME = 'idx_expire_time'
        ),
        'SELECT 1',
        'CREATE INDEX idx_expire_time ON t_attendance_session (expire_time)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session'
              AND INDEX_NAME = 'idx_sign_code_status'
        ),
        'SELECT 1',
        'CREATE INDEX idx_sign_code_status ON t_attendance_session (sign_code, status, deleted)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session_record'
              AND INDEX_NAME = 'uk_session_user'
        ),
        'SELECT 1',
        'ALTER TABLE t_attendance_session_record ADD UNIQUE KEY uk_session_user (session_id, user_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 't_attendance_session_record'
              AND INDEX_NAME = 'idx_lab_user'
        ),
        'SELECT 1',
        'CREATE INDEX idx_lab_user ON t_attendance_session_record (lab_id, user_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
