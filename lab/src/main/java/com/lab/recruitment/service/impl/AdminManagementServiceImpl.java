package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.recruitment.entity.LabMember;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.AdminManagementService;
import com.lab.recruitment.service.LabMemberService;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminManagementServiceImpl implements AdminManagementService {

    private static final String MEMBER_STATUS_ACTIVE = "active";
    private static final String MEMBER_ROLE_LAB_ADMIN = "lab_admin";
    private static final String MEMBER_ROLE_MEMBER = "member";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LabMemberMapper labMemberMapper;

    @Autowired
    private LabMemberService labMemberService;

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Override
    @Transactional
    public Result<Object> assignAdminToLab(Long labId, Long userId, User operator) {
        try {
            if (labId == null || userId == null) {
                return Result.error("实验室 ID 和用户 ID 不能为空");
            }

            User targetUser = userMapper.selectById(userId);
            if (targetUser == null || !isActiveUser(targetUser)) {
                return Result.error("用户不存在");
            }
            if (!userAccessService.isStudentIdentity(targetUser)) {
                return Result.error("仅学生身份可设置为实验室管理员");
            }

            LabMember targetMember = findActiveMember(labId, userId);
            if (targetMember == null) {
                return Result.error("实验室管理员只能从该实验室在组学生中选取");
            }

            boolean operatorCanArrange = currentUserAccessor.isSuperAdmin(operator)
                    || currentUserAccessor.isCollegeManager(operator);
            LabMember currentAdminMember = findCurrentLabAdminMember(labId);

            if (currentAdminMember != null) {
                if (currentAdminMember.getUserId() != null && currentAdminMember.getUserId().equals(userId)) {
                    refreshLabAdminUsers(labId, userId);
                    return Result.success("该学生已是当前实验室管理员");
                }
                if (operatorCanArrange) {
                    return Result.error("当前实验室已有管理员，请由现任管理员在实验室内发起权限移交");
                }
                if (operator == null || operator.getId() == null || !operator.getId().equals(currentAdminMember.getUserId())) {
                    return Result.error("仅当前实验室管理员可移交权限");
                }
                demoteAdminMember(currentAdminMember, operator == null ? null : operator.getId(), "管理员权限已移交");
            } else if (!operatorCanArrange) {
                return Result.error("当前实验室暂无管理员，请联系学校管理员或学院管理员安排学生管理员");
            }

            labMemberService.activateMember(labId, userId, MEMBER_ROLE_LAB_ADMIN,
                    operator == null ? null : operator.getId(), "已任命为实验室管理员");
            refreshLabAdminUsers(labId, userId);

            return Result.success(currentAdminMember != null ? "管理员权限移交成功" : "学生管理员安排成功");
        } catch (Exception e) {
            return Result.error("指定管理员失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Object> removeAdminFromLab(Long labId, User operator) {
        try {
            if (!currentUserAccessor.isSuperAdmin(operator) && !currentUserAccessor.isCollegeManager(operator)) {
                return Result.error("仅学校管理员或学院管理员可以撤销实验室管理员");
            }
            currentUserAccessor.assertLabScope(operator, labId);

            LabMember currentAdminMember = findCurrentLabAdminMember(labId);
            if (currentAdminMember == null) {
                refreshLabAdminUsers(labId, null);
                return Result.error("该实验室没有管理员");
            }

            demoteAdminMember(currentAdminMember, operator == null ? null : operator.getId(), "实验室管理员权限已撤销");
            refreshLabAdminUsers(labId, null);
            return Result.success("管理员移除成功");
        } catch (Exception e) {
            return Result.error("移除管理员失败: " + e.getMessage());
        }
    }

    @Override
    public Result<User> getLabAdmin(Long labId) {
        try {
            User admin = getCurrentLabAdminUser(labId);
            if (admin == null) {
                return Result.error("该实验室没有管理员");
            }
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
            if (user == null || !isActiveUser(user)) {
                return Result.error("用户不存在");
            }
            if (!userAccessService.isStudentIdentity(user)) {
                return Result.success(false);
            }

            QueryWrapper<LabMember> managerQuery = new QueryWrapper<>();
            managerQuery.eq("user_id", userId)
                    .eq("deleted", 0)
                    .eq("status", MEMBER_STATUS_ACTIVE)
                    .eq("member_role", MEMBER_ROLE_LAB_ADMIN)
                    .last("LIMIT 1");
            return Result.success(labMemberMapper.selectOne(managerQuery) == null);
        } catch (Exception e) {
            return Result.error("检查用户状态失败: " + e.getMessage());
        }
    }

    private LabMember findCurrentLabAdminMember(Long labId) {
        if (labId == null) {
            return null;
        }
        QueryWrapper<LabMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("deleted", 0)
                .eq("status", MEMBER_STATUS_ACTIVE)
                .eq("member_role", MEMBER_ROLE_LAB_ADMIN)
                .orderByDesc("id")
                .last("LIMIT 1");
        return labMemberMapper.selectOne(queryWrapper);
    }

    private LabMember findActiveMember(Long labId, Long userId) {
        if (labId == null || userId == null) {
            return null;
        }
        QueryWrapper<LabMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .eq("status", MEMBER_STATUS_ACTIVE)
                .orderByDesc("id")
                .last("LIMIT 1");
        return labMemberMapper.selectOne(queryWrapper);
    }

    private void demoteAdminMember(LabMember member, Long operatorId, String remark) {
        if (member == null || member.getId() == null) {
            return;
        }
        member.setMemberRole(MEMBER_ROLE_MEMBER);
        member.setAppointedBy(operatorId);
        member.setRemark(remark);
        labMemberMapper.updateById(member);
        userAccessService.refreshCompatibilityAccess(member.getUserId());
    }

    private void refreshLabAdminUsers(Long labId, Long currentAdminUserId) {
        Set<Long> refreshUserIds = new LinkedHashSet<>();
        if (currentAdminUserId != null) {
            refreshUserIds.add(currentAdminUserId);
        }

        QueryWrapper<LabMember> memberQuery = new QueryWrapper<>();
        memberQuery.select("user_id")
                .eq("lab_id", labId)
                .eq("deleted", 0)
                .eq("status", MEMBER_STATUS_ACTIVE);
        for (LabMember member : labMemberMapper.selectList(memberQuery)) {
            if (member != null && member.getUserId() != null) {
                refreshUserIds.add(member.getUserId());
            }
        }

        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.select("id")
                .eq("deleted", 0)
                .eq("lab_id", labId);
        for (User user : userMapper.selectList(userQuery)) {
            if (user != null && user.getId() != null) {
                refreshUserIds.add(user.getId());
            }
        }

        for (Long refreshUserId : refreshUserIds) {
            userAccessService.refreshCompatibilityAccess(refreshUserId);
        }
    }

    private User getCurrentLabAdminUser(Long labId) {
        LabMember adminMember = findCurrentLabAdminMember(labId);
        if (adminMember != null && adminMember.getUserId() != null) {
            User admin = userMapper.selectById(adminMember.getUserId());
            if (isActiveUser(admin)) {
                return admin;
            }
        }

        QueryWrapper<User> fallbackQuery = new QueryWrapper<>();
        fallbackQuery.eq("role", "admin")
                .eq("lab_id", labId)
                .eq("deleted", 0)
                .orderByDesc("id")
                .last("LIMIT 1");
        return userMapper.selectOne(fallbackQuery);
    }

    private boolean isActiveUser(User user) {
        return user != null
                && (user.getDeleted() == null || user.getDeleted() == 0)
                && (user.getStatus() == null || user.getStatus() == 1);
    }
}
