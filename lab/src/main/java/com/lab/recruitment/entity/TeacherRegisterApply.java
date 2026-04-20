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
@TableName("t_teacher_register_apply")
public class TeacherRegisterApply {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("teacher_no")
    private String teacherNo;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("real_name")
    private String realName;

    @TableField("college_id")
    private Long collegeId;

    @TableField("title")
    private String title;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("apply_reason")
    private String applyReason;

    @TableField("status")
    private String status;

    @TableField("college_audit_by")
    private Long collegeAuditBy;

    @TableField("college_audit_time")
    private LocalDateTime collegeAuditTime;

    @TableField("college_audit_comment")
    private String collegeAuditComment;

    @TableField("school_audit_by")
    private Long schoolAuditBy;

    @TableField("school_audit_time")
    private LocalDateTime schoolAuditTime;

    @TableField("school_audit_comment")
    private String schoolAuditComment;

    @TableField("generated_user_id")
    private Long generatedUserId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
