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
