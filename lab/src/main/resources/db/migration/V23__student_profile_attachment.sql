SET @student_profile_attachment_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_student_profile'
      AND COLUMN_NAME = 'attachment_url'
);

SET @ddl := IF(
    @student_profile_attachment_exists = 0,
    'ALTER TABLE t_student_profile ADD COLUMN attachment_url VARCHAR(1024) NULL AFTER introduction',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
