package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_attendance_session")
public class AttendanceSession {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("schedule_id")
    private Long scheduleId;

    @TableField("lab_id")
    private Long labId;

    @TableField("session_date")
    private LocalDate sessionDate;

    @TableField("session_code")
    private String sessionCode;

    @TableField("session_no")
    private String sessionNo;

    @TableField("sign_code")
    private String signCode;

    @TableField("qr_code_content")
    private String qrCodeContent;

    @TableField("status")
    private String status;

    @TableField("sign_start_time")
    private LocalDateTime signStartTime;

    @TableField("sign_end_time")
    private LocalDateTime signEndTime;

    @TableField("late_time")
    private LocalDateTime lateTime;

    @TableField("code_expire_time")
    private LocalDateTime codeExpireTime;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("duration_seconds")
    private Integer durationSeconds;

    @TableField("created_by")
    private Long createdBy;

    @TableField("generated_by")
    private Long generatedBy;

    @TableField("publish_time")
    private LocalDateTime publishTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
