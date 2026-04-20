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
