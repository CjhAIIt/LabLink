package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_attendance_stats_cache")
public class AttendanceStatsCache {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("lab_id")
    private Long labId;

    @TableField("user_id")
    private Long userId;

    @TableField("stat_month")
    private String statMonth;

    @TableField("signed_days")
    private Integer signedDays;

    @TableField("late_days")
    private Integer lateDays;

    @TableField("absent_days")
    private Integer absentDays;

    @TableField("leave_days")
    private Integer leaveDays;

    @TableField("total_days")
    private Integer totalDays;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
