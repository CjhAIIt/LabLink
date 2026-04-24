package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_outstanding_graduate")
public class OutstandingGraduate {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("lab_id")
    private Long labId;
    
    @TableField("name")
    private String name;
    
    @TableField("major")
    private String major;
    
    @TableField("graduation_year")
    private String graduationYear;
    
    @TableField("description")
    private String description;
    
    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("cover_image_url")
    private String coverImageUrl;
    
    @TableField("company")
    private String company;
    
    @TableField("position")
    private String position;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
