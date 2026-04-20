package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_ai_interview_module")
public class AiInterviewModule {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("module_name")
    private String moduleName;

    @TableField("module_code")
    private String moduleCode;

    @TableField("description")
    private String description;

    @TableField("prompt_template")
    private String promptTemplate;

    @TableField("score_rule")
    private String scoreRule;

    @TableField("icon")
    private String icon;

    @TableField("color")
    private String color;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
