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
@TableName("t_lab_exit_application")
public class LabExitApplication {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("lab_id")
    private Long labId;

    @TableField("reason")
    private String reason;

    @TableField("status")
    private Integer status;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("audit_by")
    private Long auditBy;

    @TableField("audit_time")
    private LocalDateTime auditTime;

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

    @TableField(exist = false)
    private String labName;
}
