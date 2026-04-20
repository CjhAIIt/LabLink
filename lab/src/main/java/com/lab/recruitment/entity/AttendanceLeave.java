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
@TableName("t_attendance_leave")
public class AttendanceLeave {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("task_id")
    private Long taskId;

    @TableField("lab_id")
    private Long labId;

    @TableField("user_id")
    private Long userId;

    @TableField("leave_reason")
    private String leaveReason;

    @TableField("leave_status")
    private String leaveStatus;

    @TableField("review_comment")
    private String reviewComment;

    @TableField("batch_id")
    private String batchId;

    @TableField("reviewed_by")
    private Long reviewedBy;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
