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
@TableName("t_written_exam_attempt")
public class WrittenExamAttempt {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("exam_id")
    private Long examId;

    @TableField("student_id")
    private Long studentId;

    @TableField("status")
    private Integer status;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("submit_time")
    private LocalDateTime submitTime;

    @TableField("auto_score")
    private Integer autoScore;

    @TableField("manual_score")
    private Integer manualScore;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("passed")
    private Boolean passed;

    @TableField("switch_count")
    private Integer switchCount;

    @TableField("refresh_count")
    private Integer refreshCount;

    @TableField("graded_by")
    private Long gradedBy;

    @TableField("graded_time")
    private LocalDateTime gradedTime;

    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
