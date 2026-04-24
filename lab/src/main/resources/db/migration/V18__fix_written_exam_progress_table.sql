CREATE TABLE IF NOT EXISTS t_written_exam_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    answers_json JSON NULL COMMENT '答案快照JSON',
    remaining_seconds INT NULL COMMENT '剩余秒数',
    current_index INT NOT NULL DEFAULT 0 COMMENT '当前题目索引',
    flagged_ids JSON NULL COMMENT '标记的题目ID列表',
    save_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exam_student (exam_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试进度暂存表';

SET @legacy_progress_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'written_exam_progress'
);

SET @new_progress_empty := (
    SELECT CASE WHEN COUNT(*) = 0 THEN 1 ELSE 0 END
    FROM t_written_exam_progress
);

SET @migrate_progress_sql := IF(
    @legacy_progress_exists > 0 AND @new_progress_empty = 1,
    'INSERT INTO t_written_exam_progress (id, exam_id, student_id, answers_json, remaining_seconds, current_index, flagged_ids, save_time)
     SELECT id, exam_id, student_id, answers_json, remaining_seconds, current_index, flagged_ids, save_time
     FROM written_exam_progress',
    'SELECT 1'
);

PREPARE stmt_progress_migrate FROM @migrate_progress_sql;
EXECUTE stmt_progress_migrate;
DEALLOCATE PREPARE stmt_progress_migrate;
