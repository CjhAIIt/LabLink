package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.RecruitPlan;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.RecruitPlanMapper;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.RecruitPlanService;
import com.lab.recruitment.support.CurrentUserAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class RecruitPlanServiceImpl extends ServiceImpl<RecruitPlanMapper, RecruitPlan> implements RecruitPlanService {

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private LabService labService;

    @Override
    public Page<Map<String, Object>> getPlanPage(Integer pageNum, Integer pageSize, Long labId, String status,
                                                 String keyword, User currentUser) {
        Long scopedLabId = labId;
        if (!currentUserAccessor.isSuperAdmin(currentUser) && currentUserAccessor.isLabScopedManager(currentUser)) {
            scopedLabId = currentUserAccessor.resolveLabScope(currentUser, labId);
        }
        return baseMapper.selectPlanPage(new Page<>(pageNum, pageSize), scopedLabId,
                normalizeStatus(status, false), trimToNull(keyword));
    }

    @Override
    public List<Map<String, Object>> getActivePlans(Long labId) {
        return baseMapper.selectActivePlanList(labId);
    }

    @Override
    @Transactional
    public boolean createPlan(RecruitPlan recruitPlan, User currentUser) {
        preparePlan(recruitPlan, currentUser, true);
        recruitPlan.setCreatedBy(currentUser.getId());
        return this.save(recruitPlan);
    }

    @Override
    @Transactional
    public boolean updatePlan(RecruitPlan recruitPlan, User currentUser) {
        if (recruitPlan == null || recruitPlan.getId() == null) {
            throw new RuntimeException("招新计划ID不能为空");
        }
        RecruitPlan existing = this.getById(recruitPlan.getId());
        if (existing == null) {
            throw new RuntimeException("招新计划不存在");
        }
        assertCanManagePlan(existing, currentUser);
        preparePlan(recruitPlan, currentUser, false);
        recruitPlan.setCreatedBy(null);
        return this.updateById(recruitPlan);
    }

    @Override
    @Transactional
    public boolean deletePlan(Long id, User currentUser) {
        RecruitPlan existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("招新计划不存在");
        }
        assertCanManagePlan(existing, currentUser);
        return this.removeById(id);
    }

    private void preparePlan(RecruitPlan recruitPlan, User currentUser, boolean creating) {
        if (recruitPlan == null) {
            throw new RuntimeException("招新计划不能为空");
        }
        if (!StringUtils.hasText(recruitPlan.getTitle())) {
            throw new RuntimeException("招新计划标题不能为空");
        }
        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            recruitPlan.setLabId(currentUserAccessor.resolveLabScope(currentUser, recruitPlan.getLabId()));
        }
        if (recruitPlan.getLabId() == null) {
            throw new RuntimeException("实验室不能为空");
        }
        Lab lab = labService.getById(recruitPlan.getLabId());
        if (lab == null) {
            throw new RuntimeException("实验室不存在");
        }
        if (recruitPlan.getQuota() == null || recruitPlan.getQuota() <= 0) {
            throw new RuntimeException("招新名额必须大于0");
        }
        if (recruitPlan.getStartTime() != null && recruitPlan.getEndTime() != null
                && recruitPlan.getStartTime().isAfter(recruitPlan.getEndTime())) {
            throw new RuntimeException("开始时间不能晚于结束时间");
        }
        recruitPlan.setStatus(normalizeStatus(recruitPlan.getStatus(), true));
        recruitPlan.setRequirement(trimToNull(recruitPlan.getRequirement()));
        if (!creating) {
            recruitPlan.setCreatedBy(null);
        }
    }

    private void assertCanManagePlan(RecruitPlan recruitPlan, User currentUser) {
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            return;
        }
        currentUserAccessor.assertLabScope(currentUser, recruitPlan.getLabId());
    }

    private String normalizeStatus(String status, boolean applyDefault) {
        String value = trimToNull(status);
        if (!StringUtils.hasText(value)) {
            return applyDefault ? "draft" : null;
        }
        String normalized = value.toLowerCase();
        if (!"draft".equals(normalized) && !"open".equals(normalized) && !"closed".equals(normalized)) {
            throw new RuntimeException("招新计划状态不合法");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
