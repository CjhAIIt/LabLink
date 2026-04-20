package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_ai_interview_record")
public class AiInterviewRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("student_name")
    private String studentName;

    @TableField("module_id")
    private Long moduleId;

    @TableField("module_name")
    private String moduleName;

    @TableField("attempt_no")
    private Integer attemptNo;

    @TableField("score")
    private Integer score;

    @TableField("tags_json")
    private String tagsJson;

    @TableField("summary")
    private String summary;

    @TableField("strengths")
    private String strengths;

    @TableField("weaknesses")
    private String weaknesses;

    @TableField("suggestions")
    private String suggestions;

    @TableField("conversation_json")
    private String conversationJson;

    @TableField("status")
    private String status;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
