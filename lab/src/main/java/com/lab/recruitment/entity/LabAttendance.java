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
@TableName("t_lab_attendance")
public class LabAttendance {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("lab_id")
    private Long labId;

    @TableField("user_id")
    private Long userId;

    @TableField("attendance_date")
    private String attendanceDate;

    @TableField("status")
    private Integer status;

    @TableField("reason")
    private String reason;

    @TableField("confirmed_by")
    private Long confirmedBy;

    @TableField("confirm_time")
    private LocalDateTime confirmTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField(exist = false)
    private String realName;

    @TableField(exist = false)
    private String studentId;

    @TableField(exist = false)
    private String major;
}
