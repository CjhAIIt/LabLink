package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.LabExitApplication;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabExitApplicationMapper;
import com.lab.recruitment.service.LabExitApplicationService;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.UserService;
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

    @Override
    @Transactional
    public boolean submit(Long userId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new RuntimeException("Please provide a reason for leaving the lab");
        }

        User user = userService.getById(userId);
        if (user == null || !"student".equals(user.getRole())) {
            throw new RuntimeException("User not found");
        }
        if (user.getLabId() == null) {
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
        application.setLabId(user.getLabId());
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
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.eq("lab_id", labId)
                    .eq("role", "student")
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
        if (admin.getLabId() == null) {
            throw new RuntimeException("Current admin is not bound to any lab");
        }

        LabExitApplication application = this.getById(applicationId);
        if (application == null) {
            throw new RuntimeException("Exit application not found");
        }
        if (!admin.getLabId().equals(application.getLabId())) {
            throw new RuntimeException("You cannot review another lab's exit request");
        }
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
            if (member.getLabId() != null && !member.getLabId().equals(application.getLabId())) {
                throw new RuntimeException("The student has already joined another lab");
            }
            userService.update(
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                            .eq(User::getId, member.getId())
                            .set(User::getLabId, null)
            );
            labService.syncCurrentMemberCount(application.getLabId());
        }

        return true;
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
