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
@TableName("t_student_profile")
public class StudentProfile {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("lab_id")
    private Long labId;

    @TableField("student_no")
    private String studentNo;

    @TableField("real_name")
    private String realName;

    @TableField("gender")
    private String gender;

    @TableField("college_id")
    private Long collegeId;

    @TableField("major")
    private String major;

    @TableField("class_name")
    private String className;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("direction")
    private String direction;

    @TableField("introduction")
    private String introduction;

    @TableField("attachment_url")
    private String attachmentUrl;

    @TableField("status")
    private String status;

    @TableField("current_version")
    private Integer currentVersion;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("last_review_time")
    private LocalDateTime lastReviewTime;

    @TableField("created_by")
    private Long createdBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("version")
    private Integer version;
}
