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
