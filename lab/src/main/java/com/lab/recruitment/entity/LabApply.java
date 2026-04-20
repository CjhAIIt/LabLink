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
@TableName("t_lab_apply")
public class LabApply {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("lab_id")
    private Long labId;

    @TableField("student_user_id")
    private Long studentUserId;

    @TableField("recruit_plan_id")
    private Long recruitPlanId;

    @TableField("apply_reason")
    private String applyReason;

    @TableField("research_interest")
    private String researchInterest;

    @TableField("skill_summary")
    private String skillSummary;

    @TableField("status")
    private String status;

    @TableField("audit_by")
    private Long auditBy;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_comment")
    private String auditComment;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
