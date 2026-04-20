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
