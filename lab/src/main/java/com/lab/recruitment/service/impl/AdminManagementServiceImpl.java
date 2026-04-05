package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.AdminManagementService;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员管理服务实现类
 */
@Service
public class AdminManagementServiceImpl implements AdminManagementService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    @Transactional
    public Result<Object> assignAdminToLab(Long labId, Long userId) {
        try {
            // 检查用户是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 检查用户角色是否为学生（只有学生可以提升为管理员）
            if (!"student".equals(user.getRole())) {
                return Result.error("只有学生可以设置为管理员");
            }
            
            // 检查实验室是否已有管理员
            QueryWrapper<User> adminQuery = new QueryWrapper<>();
            adminQuery.eq("role", "admin")
                     .eq("lab_id", labId)
                     .eq("deleted", 0);
            User existingAdmin = userMapper.selectOne(adminQuery);
            
            if (existingAdmin != null) {
                // 如果已有管理员，先将其降级为学生
                existingAdmin.setRole("student");
                existingAdmin.setLabId(null);
                userMapper.updateById(existingAdmin);
            }
            
            // 检查用户是否已经是其他实验室的管理员
            QueryWrapper<User> userAdminQuery = new QueryWrapper<>();
            userAdminQuery.eq("role", "admin")
                         .eq("id", userId)
                         .eq("deleted", 0);
            User userAsAdmin = userMapper.selectOne(userAdminQuery);
            
            if (userAsAdmin != null) {
                return Result.error("该用户已经是其他实验室的管理员");
            }
            
            // 将用户设置为管理员并关联实验室
            user.setRole("admin");
            user.setLabId(labId);
            userMapper.updateById(user);
            
            return Result.success("管理员指定成功");
        } catch (Exception e) {
            return Result.error("指定管理员失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Result<Object> removeAdminFromLab(Long labId) {
        try {
            // 查找实验室管理员
            QueryWrapper<User> adminQuery = new QueryWrapper<>();
            adminQuery.eq("role", "admin")
                     .eq("lab_id", labId)
                     .eq("deleted", 0);
            User admin = userMapper.selectOne(adminQuery);
            
            if (admin == null) {
                return Result.error("该实验室没有管理员");
            }
            
            // 将管理员降级为学生并移除实验室关联
            admin.setRole("student");
            admin.setLabId(null);
            userMapper.updateById(admin);
            
            return Result.success("管理员移除成功");
        } catch (Exception e) {
            return Result.error("移除管理员失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result<User> getLabAdmin(Long labId) {
        try {
            QueryWrapper<User> adminQuery = new QueryWrapper<>();
            adminQuery.eq("role", "admin")
                     .eq("lab_id", labId)
                     .eq("deleted", 0);
            User admin = userMapper.selectOne(adminQuery);
            
            if (admin == null) {
                return Result.error("该实验室没有管理员");
            }
            
            // 清除密码等敏感信息
            admin.setPassword(null);
            
            return Result.success(admin);
        } catch (Exception e) {
            return Result.error("获取管理员信息失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result<List<User>> getAllAdminsWithLabs() {
        try {
            QueryWrapper<User> adminQuery = new QueryWrapper<>();
            adminQuery.eq("role", "admin")
                     .eq("deleted", 0)
                     .orderByDesc("create_time");
            
            List<User> admins = userMapper.selectList(adminQuery);
            
            // 清除密码等敏感信息
            admins.forEach(admin -> admin.setPassword(null));
            
            return Result.success(admins);
        } catch (Exception e) {
            return Result.error("获取管理员列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result<Boolean> canUserBeAdmin(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 只有学生可以成为管理员
            if (!"student".equals(user.getRole())) {
                return Result.success(false);
            }
            
            // 检查是否已经是管理员
            if ("admin".equals(user.getRole()) && user.getLabId() != null) {
                return Result.success(false);
            }
            
            return Result.success(true);
        } catch (Exception e) {
            return Result.error("检查用户状态失败: " + e.getMessage());
        }
    }
}