-- =============================================
-- V14: 出勤管理增强 - 异常处理 + 统计缓存 + 批量审批
-- =============================================

-- 1. t_attendance_record 增加异常处理字段
ALTER TABLE t_attendance_record
  ADD COLUMN handle_remark VARCHAR(200) NULL COMMENT '异常处理备注' AFTER remark,
  ADD COLUMN handled_by    BIGINT       NULL COMMENT '处理人ID'     AFTER handle_remark,
  ADD COLUMN handled_at    DATETIME     NULL COMMENT '处理时间'     AFTER handled_by;

-- 2. t_attendance_leave 增加批量审批批次ID
ALTER TABLE t_attendance_leave
  ADD COLUMN batch_id VARCHAR(64) NULL COMMENT '批量审批批次ID' AFTER review_comment;

-- 3. 出勤统计缓存表
CREATE TABLE IF NOT EXISTS t_attendance_stats_cache (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  lab_id      BIGINT      NOT NULL,
  user_id     BIGINT      NOT NULL,
  stat_month  VARCHAR(7)  NOT NULL COMMENT '格式 2026-04',
  signed_days INT         NOT NULL DEFAULT 0,
  late_days   INT         NOT NULL DEFAULT 0,
  absent_days INT         NOT NULL DEFAULT 0,
  leave_days  INT         NOT NULL DEFAULT 0,
  total_days  INT         NOT NULL DEFAULT 0,
  updated_at  DATETIME    NOT NULL,
  UNIQUE KEY uk_lab_user_month (lab_id, user_id, stat_month),
  KEY idx_lab_month (lab_id, stat_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出勤统计月度缓存';
