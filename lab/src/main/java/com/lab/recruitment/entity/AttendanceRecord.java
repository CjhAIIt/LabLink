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
@TableName("t_attendance_record")
public class AttendanceRecord {

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

    @TableField("sign_status")
    private String signStatus;

    @TableField("sign_code")
    private String signCode;

    @TableField("sign_time")
    private LocalDateTime signTime;

    @TableField("remark")
    private String remark;

    @TableField("source")
    private String source;

    @TableField("reviewed_by")
    private Long reviewedBy;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
