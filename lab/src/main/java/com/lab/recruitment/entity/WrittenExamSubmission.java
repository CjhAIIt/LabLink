package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_written_exam_submission")
public class WrittenExamSubmission {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("exam_id")
    private Long examId;

    @TableField("lab_id")
    private Long labId;

    @TableField("user_id")
    private Long userId;

    @TableField("answer_sheet_json")
    private String answerSheetJson;

    @TableField("total_score")
    private BigDecimal totalScore;

    @TableField("ai_remark")
    private String aiRemark;

    @TableField("admin_remark")
    private String adminRemark;

    @TableField("status")
    private Integer status;

    @TableField("submit_time")
    private LocalDateTime submitTime;

    @TableField("grade_time")
    private LocalDateTime gradeTime;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
