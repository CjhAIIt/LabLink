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

    @TableField("exam_id")
    private Long examId;

    @TableField("bank_question_id")
    private Long bankQuestionId;

    @TableField("question_type")
    private String questionType;

    @TableField("track_code")
    private String trackCode;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("difficulty")
    private String difficulty;

    @TableField("input_format")
    private String inputFormat;

    @TableField("output_format")
    private String outputFormat;

    @TableField("sample_case_json")
    private String sampleCaseJson;

    @TableField("options_json")
    private String optionsJson;

    @TableField("answer_config")
    private String answerConfig;

    @TableField("program_languages")
    private String programLanguages;

    @TableField("judge_case_json")
    private String judgeCaseJson;

    @TableField("tags_json")
    private String tagsJson;

    @TableField("analysis_hint")
    private String analysisHint;

    @TableField("score")
    private Integer score;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
