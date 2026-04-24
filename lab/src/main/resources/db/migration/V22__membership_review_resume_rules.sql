SET @old_snapshot_column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_lab_info_change_review'
      AND COLUMN_NAME = 'old_snapshot'
);

SET @ddl := IF(
    @old_snapshot_column_exists = 0,
    'ALTER TABLE t_lab_info_change_review ADD COLUMN old_snapshot LONGTEXT NULL AFTER review_snapshot',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE t_user u
LEFT JOIN t_lab_member m
    ON m.user_id = u.id
   AND m.lab_id = u.lab_id
   AND m.deleted = 0
   AND m.status = 'active'
SET u.lab_id = NULL,
    u.update_time = NOW()
WHERE u.deleted = 0
  AND u.role = 'student'
  AND u.lab_id IS NOT NULL
  AND m.id IS NULL;

UPDATE t_lab l
LEFT JOIN (
    SELECT lab_id, COUNT(DISTINCT user_id) AS active_count
    FROM t_lab_member
    WHERE deleted = 0 AND status = 'active'
    GROUP BY lab_id
) x ON x.lab_id = l.id
SET l.current_num = COALESCE(x.active_count, 0),
    l.update_time = NOW()
WHERE l.deleted = 0;
