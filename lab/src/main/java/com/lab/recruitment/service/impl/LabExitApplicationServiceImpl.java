package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.LabExitApplication;
import com.lab.recruitment.entity.LabMember;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.mapper.LabExitApplicationMapper;
import com.lab.recruitment.service.LabExitApplicationService;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.SystemNotificationService;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.support.CurrentUserAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabExitApplicationServiceImpl extends ServiceImpl<LabExitApplicationMapper, LabExitApplication> implements LabExitApplicationService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    @Autowired
    private UserService userService;

    @Autowired
    private LabService labService;

    @Autowired
    private LabMemberMapper labMemberMapper;

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private SystemNotificationService systemNotificationService;

    @Override
    @Transactional
    public boolean submit(Long userId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new RuntimeException("Please provide a reason for leaving the lab");
        }

        User user = userService.getById(userId);
        if (user == null || !userAccessService.isStudentIdentity(user)) {
            throw new RuntimeException("User not found");
        }
        Long activeLabId = userAccessService.resolveManagedLabId(user);
        if (activeLabId == null) {
            throw new RuntimeException("You have not joined any lab");
        }

        QueryWrapper<LabExitApplication> pendingQuery = new QueryWrapper<>();
        pendingQuery.eq("user_id", userId)
                .eq("status", STATUS_PENDING);
        if (this.count(pendingQuery) > 0) {
            throw new RuntimeException("You already have a pending exit request");
        }

        LabExitApplication application = new LabExitApplication();
        application.setUserId(userId);
        application.setLabId(activeLabId);
        application.setReason(reason.trim());
        application.setStatus(STATUS_PENDING);
        return this.save(application);
    }

    @Override
    public Page<LabExitApplication> getMyApplicationPage(Integer pageNum, Integer pageSize, Long userId) {
        Page<LabExitApplication> page = new Page<>(pageNum, pageSize);
        QueryWrapper<LabExitApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("create_time")
                .orderByDesc("id");
        Page<LabExitApplication> result = this.page(page, queryWrapper);
        enrichRecords(result.getRecords());
        return result;
    }

    @Override
    public Page<LabExitApplication> getLabApplicationPage(Integer pageNum, Integer pageSize, Long labId, Integer status, String realName) {
        Page<LabExitApplication> page = new Page<>(pageNum, pageSize);
        QueryWrapper<LabExitApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .orderByAsc("status")
                .orderByDesc("create_time")
                .orderByDesc("id");
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        if (StringUtils.hasText(realName)) {
            QueryWrapper<LabMember> memberQuery = new QueryWrapper<>();
            memberQuery.select("user_id")
                    .eq("lab_id", labId)
                    .eq("deleted", 0)
                    .eq("status", "active");
            List<Long> activeUserIds = labMemberMapper.selectList(memberQuery).stream()
                    .map(LabMember::getUserId)
                    .collect(Collectors.toList());
            if (activeUserIds.isEmpty()) {
                page.setRecords(java.util.Collections.emptyList());
                page.setTotal(0);
                return page;
            }
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.in("id", activeUserIds)
                    .like("real_name", realName.trim());
            List<Long> userIds = userService.list(userQuery).stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            if (userIds.isEmpty()) {
                page.setRecords(java.util.Collections.emptyList());
                page.setTotal(0);
                return page;
            }
            queryWrapper.in("user_id", userIds);
        }
        Page<LabExitApplication> result = this.page(page, queryWrapper);
        enrichRecords(result.getRecords());
        return result;
    }

    @Override
    @Transactional
    public boolean audit(Long applicationId, Integer status, String auditRemark, User admin) {
        if (status == null || (status != STATUS_APPROVED && status != STATUS_REJECTED)) {
            throw new RuntimeException("Audit status is invalid");
        }
        LabExitApplication application = this.getById(applicationId);
        if (application == null) {
            throw new RuntimeException("Exit application not found");
        }
        currentUserAccessor.assertLabScope(admin, application.getLabId());
        if (!Integer.valueOf(STATUS_PENDING).equals(application.getStatus())) {
            throw new RuntimeException("This exit request has already been processed");
        }

        application.setStatus(status);
        application.setAuditRemark(StringUtils.hasText(auditRemark) ? auditRemark.trim() : null);
        application.setAuditBy(admin.getId());
        application.setAuditTime(LocalDateTime.now());
        this.updateById(application);

        if (status == STATUS_APPROVED) {
            User member = userService.getById(application.getUserId());
            if (member == null) {
                throw new RuntimeException("Lab member not found");
            }
            deactivateActiveMemberships(application.getLabId(), application.getUserId());
            userService.update(
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                            .eq(User::getId, member.getId())
                            .set(User::getLabId, null)
            );
            userAccessService.refreshCompatibilityAccess(member.getId());
            labService.syncCurrentMemberCount(application.getLabId());
        }
        systemNotificationService.createNotification(
                application.getUserId(),
                status == STATUS_APPROVED ? "退组申请已通过" : "退组申请已驳回",
                StringUtils.hasText(auditRemark) ? auditRemark.trim() : "请查看退组申请处理结果。",
                "lab_exit_review",
                application.getId(),
                "/student/lab"
        );

        return true;
    }

    private void deactivateActiveMemberships(Long labId, Long userId) {
        QueryWrapper<LabMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("lab_id", labId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .eq("status", "active");
        List<LabMember> memberships = labMemberMapper.selectList(memberQuery);
        LocalDateTime now = LocalDateTime.now();
        for (LabMember membership : memberships) {
            membership.setStatus("inactive");
            membership.setQuitDate(now.toLocalDate());
            membership.setRemark("exit application approved");
            labMemberMapper.updateById(membership);
        }
    }

    private void enrichRecords(List<LabExitApplication> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        List<Long> userIds = records.stream().map(LabExitApplication::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left));

        List<Long> labIds = records.stream().map(LabExitApplication::getLabId).distinct().collect(Collectors.toList());
        Map<Long, Lab> labMap = new HashMap<>();
        if (!labIds.isEmpty()) {
            labMap = labService.listByIds(labIds).stream()
                    .collect(Collectors.toMap(Lab::getId, item -> item, (left, right) -> left));
        }

        for (LabExitApplication record : records) {
            User user = userMap.get(record.getUserId());
            if (user != null) {
                record.setRealName(user.getRealName());
                record.setStudentId(user.getStudentId());
                record.setMajor(user.getMajor());
            }

            Lab lab = labMap.get(record.getLabId());
            if (lab != null) {
                record.setLabName(lab.getLabName());
            }
        }
    }
}
