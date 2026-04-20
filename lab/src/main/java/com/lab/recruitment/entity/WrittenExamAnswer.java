package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_written_exam_answer")
public class WrittenExamAnswer {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("attempt_id")
    private Long attemptId;

    @TableField("question_id")
    private Long questionId;

    @TableField("answer")
    private String answer;

    @TableField("code")
    private String code;

    @TableField("language")
    private String language;

    @TableField("score")
    private Integer score;

    @TableField("is_correct")
    private Boolean isCorrect;

    @TableField("judge_result")
    private String judgeResult;

    @TableField("grader_remark")
    private String graderRemark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
