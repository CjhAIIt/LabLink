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
@TableName("t_student_profile_archive")
public class StudentProfileArchive {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("profile_id")
    private Long profileId;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("archive_snapshot")
    private String archiveSnapshot;

    @TableField("archived_by")
    private Long archivedBy;

    @TableField("archived_at")
    private LocalDateTime archivedAt;

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
