package com.lab.recruitment.dto;

import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实验室及其管理员信息DTO
 */
@Data
public class LabWithAdminDTO {
    
    /**
     * 实验室信息
     */
    private Lab lab;
    
    /**
     * 管理员信息
     */
    private User admin;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}