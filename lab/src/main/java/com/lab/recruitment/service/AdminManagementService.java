package com.lab.recruitment.service;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.utils.Result;

import java.util.List;

/**
 * 管理员管理服务接口
 */
public interface AdminManagementService {
    
    /**
     * 为实验室指定管理员
     * @param labId 实验室ID
     * @param userId 用户ID
     * @return 操作结果
     */
    Result<Object> assignAdminToLab(Long labId, Long userId);
    
    /**
     * 移除实验室管理员
     * @param labId 实验室ID
     * @return 操作结果
     */
    Result<Object> removeAdminFromLab(Long labId);
    
    /**
     * 获取实验室管理员信息
     * @param labId 实验室ID
     * @return 管理员信息
     */
    Result<User> getLabAdmin(Long labId);
    
    /**
     * 获取所有管理员及其关联的实验室
     * @return 管理员列表
     */
    Result<List<User>> getAllAdminsWithLabs();
    
    /**
     * 检查用户是否可以设置为管理员
     * @param userId 用户ID
     * @return 检查结果
     */
    Result<Boolean> canUserBeAdmin(Long userId);
}