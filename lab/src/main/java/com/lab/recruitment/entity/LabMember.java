package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_lab_member")
public class LabMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("lab_id")
    private Long labId;

    @TableField("user_id")
    private Long userId;

    @TableField("member_role")
    private String memberRole;

    @TableField("join_date")
    private LocalDate joinDate;

    @TableField("quit_date")
    private LocalDate quitDate;

    @TableField("status")
    private String status;

    @TableField("appointed_by")
    private Long appointedBy;

    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
