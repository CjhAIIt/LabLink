package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_lab")
public class Lab {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("lab_name")
    private String labName;

    @TableField("lab_code")
    private String labCode;

    @TableField("college_id")
    private Long collegeId;
    
    @TableField("lab_desc")
    private String labDesc;

    @TableField("teacher_name")
    private String teacherName;

    @TableField("location")
    private String location;

    @TableField("contact_email")
    private String contactEmail;
    
    @TableField("require_skill")
    private String requireSkill;
    
    @TableField("recruit_num")
    private Integer recruitNum;
    
    @TableField("current_num")
    private Integer currentNum;
    
    @TableField("status")
    private Integer status;
    
    // 新增字段
    @TableField("founding_date")
    private String foundingDate;
    
    @TableField("awards")
    private String awards;
    
    @TableField("outstanding_seniors")
    private String outstandingSeniors;
    
    @TableField("basic_info")
    private String basicInfo;
    
    @TableField("advisors")
    private String advisors;
    
    @TableField("current_admins")
    private String currentAdmins;

    @TableField("logo_url")
    private String logoUrl;

    @TableField("cover_image_url")
    private String coverImageUrl;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
