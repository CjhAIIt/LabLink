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
@TableName("t_student_profile_review")
public class StudentProfileReview {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("profile_id")
    private Long profileId;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("review_comment")
    private String reviewComment;

    @TableField("review_snapshot")
    private String reviewSnapshot;

    @TableField("review_time")
    private LocalDateTime reviewTime;

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
