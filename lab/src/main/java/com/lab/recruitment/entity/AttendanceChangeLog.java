package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_attendance_change_log")
public class AttendanceChangeLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("record_id")
    private Long recordId;

    @TableField("lab_id")
    private Long labId;

    @TableField("user_id")
    private Long userId;

    @TableField("before_status")
    private String beforeStatus;

    @TableField("after_status")
    private String afterStatus;

    @TableField("changed_by")
    private Long changedBy;

    @TableField("changed_reason")
    private String changedReason;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
