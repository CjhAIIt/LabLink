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
@TableName("t_lab_create_apply")
public class LabCreateApply {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("applicant_user_id")
    private Long applicantUserId;

    @TableField("college_id")
    private Long collegeId;

    @TableField("lab_name")
    private String labName;

    @TableField("teacher_name")
    private String teacherName;

    @TableField("location")
    private String location;

    @TableField("contact_email")
    private String contactEmail;

    @TableField("research_direction")
    private String researchDirection;

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

    @TableField("generated_lab_id")
    private Long generatedLabId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
