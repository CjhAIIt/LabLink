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
@TableName("t_growth_track_stage")
public class GrowthTrackStage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("track_id")
    private Long trackId;

    @TableField("stage_no")
    private Integer stageNo;

    @TableField("phase_code")
    private String phaseCode;

    @TableField("title")
    private String title;

    @TableField("duration")
    private String duration;

    @TableField("goal")
    private String goal;

    @TableField("resource_name")
    private String resourceName;

    @TableField("resource_url")
    private String resourceUrl;

    @TableField("practice_keyword")
    private String practiceKeyword;

    @TableField("action_hint")
    private String actionHint;

    @TableField("focus_skills_json")
    private String focusSkillsJson;

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
