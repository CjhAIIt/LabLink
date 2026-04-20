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
@TableName("t_audit_log")
public class AuditLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("actor_user_id")
    private Long actorUserId;

    @TableField("operator_role")
    private String operatorRole;

    @TableField("college_id")
    private Long collegeId;

    @TableField("lab_id")
    private Long labId;

    @TableField("action")
    private String action;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("detail")
    private String detail;

    @TableField("request_path")
    private String requestPath;

    @TableField("request_method")
    private String requestMethod;

    @TableField("request_ip")
    private String requestIp;

    @TableField("result")
    private String result;

    @TableField("detail_json")
    private String detailJson;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
