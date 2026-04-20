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
