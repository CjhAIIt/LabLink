package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.dto.LabApplyAuditDTO;
import com.lab.recruitment.dto.LabApplyCreateDTO;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.LabApply;
import com.lab.recruitment.entity.RecruitPlan;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabApplyMapper;
import com.lab.recruitment.service.LabApplyService;
import com.lab.recruitment.service.LabMemberService;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.RecruitPlanService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.support.CurrentUserAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class LabApplyServiceImpl extends ServiceImpl<LabApplyMapper, LabApply> implements LabApplyService {

    private static final String STATUS_SUBMITTED = "submitted";
    private static final String STATUS_LEADER_APPROVED = "leader_approved";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private UserService userService;

    @Autowired
    private LabService labService;

    @Autowired
    private RecruitPlanService recruitPlanService;

    @Autowired
    private LabMemberService labMemberService;

    @Override
    @Transactional
    public boolean createApply(LabApplyCreateDTO createDTO, User currentUser) {
        if (!StringUtils.hasText(currentUser.getResume())) {
            throw new RuntimeException("Please upload your resume before applying to a lab");
        }
        if (currentUser.getLabId() != null) {
            throw new RuntimeException("You have already joined a lab");
        }

        Lab lab = labService.getById(createDTO.getLabId());
        if (lab == null) {
            throw new RuntimeException("Lab does not exist");
        }
        if (lab.getStatus() != null && lab.getStatus() == 0) {
            throw new RuntimeException("This lab is not open for applications");
        }

        if (createDTO.getRecruitPlanId() != null) {
            RecruitPlan recruitPlan = recruitPlanService.getById(createDTO.getRecruitPlanId());
            if (recruitPlan == null || !createDTO.getLabId().equals(recruitPlan.getLabId())) {
                throw new RuntimeException("Recruit plan does not match the selected lab");
            }
            if (!"open".equalsIgnoreCase(recruitPlan.getStatus())) {
                throw new RuntimeException("The recruit plan is not open");
            }
            if (recruitPlan.getStartTime() != null && recruitPlan.getStartTime().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("The recruit plan has not started");
            }
            if (recruitPlan.getEndTime() != null && recruitPlan.getEndTime().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("The recruit plan has ended");
            }
        }

        QueryWrapper<LabApply> duplicateWrapper = new QueryWrapper<>();
        duplicateWrapper.eq("lab_id", createDTO.getLabId())
                .eq("student_user_id", currentUser.getId())
                .in("status", STATUS_SUBMITTED, STATUS_LEADER_APPROVED, STATUS_APPROVED);
        if (this.count(duplicateWrapper) > 0) {
            throw new RuntimeException("You have already applied to this lab");
        }

        LabApply apply = new LabApply();
        apply.setLabId(createDTO.getLabId());
        apply.setStudentUserId(currentUser.getId());
        apply.setRecruitPlanId(createDTO.getRecruitPlanId());
        apply.setApplyReason(trimToNull(createDTO.getApplyReason()));
        apply.setResearchInterest(trimToNull(createDTO.getResearchInterest()));
        apply.setSkillSummary(trimToNull(createDTO.getSkillSummary()));
        apply.setStatus(STATUS_SUBMITTED);
        return this.save(apply);
    }

    @Override
    public Page<Map<String, Object>> getApplyPage(Integer pageNum, Integer pageSize, Long labId, String status,
                                                  String keyword, User currentUser) {
        Long scopedLabId = labId;
        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            scopedLabId = currentUserAccessor.resolveLabScope(currentUser, labId);
        }
        return baseMapper.selectApplyPage(new Page<>(pageNum, pageSize), scopedLabId,
                normalizeStatus(status, false), null, trimToNull(keyword));
    }

    @Override
    public Page<Map<String, Object>> getMyApplyPage(Integer pageNum, Integer pageSize, String status, User currentUser) {
        return baseMapper.selectMyApplyPage(new Page<>(pageNum, pageSize), currentUser.getId(),
                normalizeStatus(status, false));
    }

    @Override
    @Transactional
    public boolean auditApply(Long applyId, LabApplyAuditDTO auditDTO, User currentUser) {
        LabApply apply = this.getById(applyId);
        if (apply == null) {
            throw new RuntimeException("Application does not exist");
        }
        currentUserAccessor.assertLabScope(currentUser, apply.getLabId());

        String action = normalizeAction(auditDTO.getAction());
        if ("leaderApprove".equals(action)) {
            ensureCurrentStatus(apply, STATUS_SUBMITTED);
            apply.setStatus(STATUS_LEADER_APPROVED);
        } else if ("approve".equals(action)) {
            if (!STATUS_SUBMITTED.equalsIgnoreCase(apply.getStatus())
                    && !STATUS_LEADER_APPROVED.equalsIgnoreCase(apply.getStatus())) {
                throw new RuntimeException("The current application status cannot be approved");
            }
            User applicant = userService.getById(apply.getStudentUserId());
            if (applicant == null) {
                throw new RuntimeException("Applicant does not exist");
            }
            if (applicant.getLabId() != null && !apply.getLabId().equals(applicant.getLabId())) {
                throw new RuntimeException("The applicant has already joined another lab");
            }

            apply.setStatus(STATUS_APPROVED);
            applicant.setLabId(apply.getLabId());
            userService.updateById(applicant);
            labMemberService.activateMember(apply.getLabId(), apply.getStudentUserId(), "member",
                    currentUser.getId(), "Approved application");
            labService.syncCurrentMemberCount(apply.getLabId());
            rejectOtherApplies(apply, currentUser.getId());
        } else if ("reject".equals(action)) {
            if (STATUS_APPROVED.equalsIgnoreCase(apply.getStatus())) {
                throw new RuntimeException("Approved applications cannot be rejected");
            }
            apply.setStatus(STATUS_REJECTED);
        } else {
            throw new RuntimeException("Unsupported audit action");
        }

        apply.setAuditBy(currentUser.getId());
        apply.setAuditTime(LocalDateTime.now());
        apply.setAuditComment(trimToNull(auditDTO.getAuditComment()));
        return this.updateById(apply);
    }

    @Override
    public List<Map<String, Object>> getLatestApplies(Long labId, int limit) {
        return baseMapper.selectLatestApplies(labId, Math.max(limit, 1));
    }

    private void rejectOtherApplies(LabApply currentApply, Long auditBy) {
        QueryWrapper<LabApply> wrapper = new QueryWrapper<>();
        wrapper.eq("student_user_id", currentApply.getStudentUserId())
                .ne("id", currentApply.getId())
                .in("status", STATUS_SUBMITTED, STATUS_LEADER_APPROVED);
        List<LabApply> otherApplies = this.list(wrapper);
        for (LabApply otherApply : otherApplies) {
            otherApply.setStatus(STATUS_REJECTED);
            otherApply.setAuditBy(auditBy);
            otherApply.setAuditTime(LocalDateTime.now());
            otherApply.setAuditComment(appendAuditComment(otherApply.getAuditComment(),
                    "Closed automatically because the student was admitted by another lab"));
        }
        if (!otherApplies.isEmpty()) {
            this.updateBatchById(otherApplies);
        }
    }

    private void ensureCurrentStatus(LabApply apply, String expectedStatus) {
        if (!expectedStatus.equalsIgnoreCase(apply.getStatus())) {
            throw new RuntimeException("The current application status does not allow this action");
        }
    }

    private String normalizeAction(String action) {
        String value = trimToNull(action);
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException("Audit action is required");
        }
        return value;
    }

    private String normalizeStatus(String status, boolean required) {
        String value = trimToNull(status);
        if (!StringUtils.hasText(value)) {
            if (required) {
                throw new RuntimeException("Status is required");
            }
            return null;
        }
        String normalized = value.toLowerCase();
        if (!STATUS_SUBMITTED.equals(normalized)
                && !STATUS_LEADER_APPROVED.equals(normalized)
                && !STATUS_APPROVED.equals(normalized)
                && !STATUS_REJECTED.equals(normalized)) {
            throw new RuntimeException("Invalid status");
        }
        return normalized;
    }

    private String appendAuditComment(String original, String extra) {
        if (!StringUtils.hasText(original)) {
            return extra;
        }
        return original + System.lineSeparator() + extra;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
