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
@TableName("t_written_exam")
public class WrittenExam {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("lab_id")
    private Long labId;

    @TableField("description")
    private String description;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("duration")
    private Integer duration;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("pass_score")
    private Integer passScore;

    @TableField("status")
    private Integer status;

    @TableField("enable_anti_cheat")
    private Boolean enableAntiCheat;

    @TableField("enable_signature")
    private Boolean enableSignature;

    @TableField("allow_retry")
    private Boolean allowRetry;

    @TableField("max_switch_count")
    private Integer maxSwitchCount;

    @TableField("created_by")
    private Long createdBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
