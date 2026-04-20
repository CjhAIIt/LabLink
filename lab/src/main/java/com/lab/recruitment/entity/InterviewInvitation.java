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
@TableName("t_interview_invitation")
public class InterviewInvitation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("exam_id")
    private Long examId;

    @TableField("lab_id")
    private Long labId;

    @TableField("student_id")
    private Long studentId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("interview_time")
    private LocalDateTime interviewTime;

    @TableField("location")
    private String location;

    @TableField("status")
    private Integer status;

    @TableField("student_confirmed")
    private Boolean studentConfirmed;

    @TableField("confirm_time")
    private LocalDateTime confirmTime;

    @TableField("sent_by")
    private Long sentBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
