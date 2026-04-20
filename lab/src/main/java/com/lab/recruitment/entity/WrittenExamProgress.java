package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_written_exam_progress")
public class WrittenExamProgress {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("exam_id")
    private Long examId;

    @TableField("student_id")
    private Long studentId;

    @TableField("answers_json")
    private String answersJson;

    @TableField("remaining_seconds")
    private Integer remainingSeconds;

    @TableField("current_index")
    private Integer currentIndex;

    @TableField("flagged_ids")
    private String flaggedIds;

    @TableField("save_time")
    private LocalDateTime saveTime;
}
