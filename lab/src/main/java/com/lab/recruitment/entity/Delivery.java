package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_delivery")
public class Delivery {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("lab_id")
    private Long labId;
    
    @TableField("skill_tags")
    private String skillTags;
    
    @TableField("study_progress")
    private String studyProgress;
    
    @TableField("attachment_url")
    private String attachmentUrl;
    
    @TableField(value = "delivery_time", fill = FieldFill.INSERT)
    private LocalDateTime deliveryTime;

    @TableField("delivery_attempt_count")
    private Integer deliveryAttemptCount;

    @TableField("withdraw_count")
    private Integer withdrawCount;
    
    @TableField("audit_status")
    private Integer auditStatus;
    
    @TableField("audit_remark")
    private String auditRemark;
    
    @TableField("audit_time")
    private LocalDateTime auditTime;
    
    @TableField("is_admitted")
    private Integer isAdmitted;
    
    @TableField("admit_time")
    private LocalDateTime admitTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
