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
@TableName("t_written_exam_question")
public class WrittenExamQuestion {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("question_type")
    private String questionType;

    @TableField("difficulty")
    private String difficulty;

    @TableField("score")
    private Integer score;

    @TableField("lab_id")
    private Long labId;

    @TableField("options")
    private String options;

    @TableField("correct_answer")
    private String correctAnswer;

    @TableField("analysis")
    private String analysis;

    @TableField("input_format")
    private String inputFormat;

    @TableField("output_format")
    private String outputFormat;

    @TableField("sample_case")
    private String sampleCase;

    @TableField("test_cases")
    private String testCases;

    @TableField("allowed_languages")
    private String allowedLanguages;

    @TableField("tags")
    private String tags;

    @TableField("sort_order")
    private Integer sortOrder;

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
