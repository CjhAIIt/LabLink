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
@TableName("t_growth_assessment_question")
public class GrowthAssessmentQuestion {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("question_no")
    private Integer questionNo;

    @TableField("dimension")
    private String dimension;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("question_type")
    private String questionType;

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
