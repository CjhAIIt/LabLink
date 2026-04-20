package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 统一管理学生、管理员、总负责人三种角色的用户信息
 * 使用MyBatis Plus注解实现ORM映射
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_user")
public class User {
    
    /**
     * 用户ID（主键，自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户名（学生为学号，管理员为管理员账号）
     */
    @TableField("username")
    private String username;
    
    /**
     * 密码（BCrypt加密存储）
     */
    @TableField("password")
    private String password;
    
    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;
    
    /**
     * 用户角色（student-学生，admin-管理员，super_admin-总负责人）
     */
    @TableField("role")
    private String role;
    
    /**
     * 学号（学生角色必填）
     */
    @TableField("student_id")
    private String studentId;
    
    /**
     * 学院（学生角色必填）
     */
    @TableField("college")
    private String college;
    
    /**
     * 专业（学生角色必填）
     */
    @TableField("major")
    private String major;
    
    /**
     * 年级（学生角色必填）
     */
    @TableField("grade")
    private String grade;
    
    /**
     * 联系电话
     */
    @TableField("phone")
    private String phone;
    
    /**
     * 邮箱地址
     */
    @TableField("email")
    private String email;
    
    /**
     * 头像URL
     */
    @TableField("avatar")
    private String avatar;
    
    /**
     * 简历URL
     */
    @TableField("resume")
    private String resume;
    
    /**
     * 关联的实验室ID（管理员角色必填）
     */
    @TableField("lab_id")
    private Long labId;
    
    /**
     * 是否可编辑账号信息（0-不可编辑，1-可编辑）
     */
    @TableField("can_edit")
    private Integer canEdit;

    @TableField("system_account_code")
    private String systemAccountCode;
    
    /**
     * 创建时间（插入时自动填充）
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间（插入和更新时自动填充）
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 账号状态（0-禁用，1-正常）
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 逻辑删除标记（0-未删除，1-已删除）
     * 使用@TableLogic注解实现逻辑删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField(exist = false)
    private Long pendingCount;

    @TableField(exist = false)
    private Long approvedCount;

    @TableField(exist = false)
    private Long rejectedCount;

}
