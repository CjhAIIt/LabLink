package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.LabCreateApplyAuditDTO;
import com.lab.recruitment.dto.LabCreateApplyCreateDTO;
import com.lab.recruitment.entity.College;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.LabCreateApply;
import com.lab.recruitment.entity.LabTeacherRelation;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.entity.UserIdentity;
import com.lab.recruitment.mapper.CollegeMapper;
import com.lab.recruitment.mapper.LabCreateApplyMapper;
import com.lab.recruitment.mapper.LabTeacherRelationMapper;
import com.lab.recruitment.mapper.UserIdentityMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.LabCreateApplyService;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.support.CurrentUserAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class LabCreateApplyServiceImpl implements LabCreateApplyService {

    private static final String STATUS_SUBMITTED = "submitted";
    private static final String STATUS_COLLEGE_APPROVED = "college_approved";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";
    private static final String TEACHER_IDENTITY = "teacher";
    private static final String IDENTITY_STATUS_ACTIVE = "active";

    @Autowired
    private LabCreateApplyMapper labCreateApplyMapper;

    @Autowired
    private LabService labService;

    @Autowired
    private CollegeMapper collegeMapper;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserIdentityMapper userIdentityMapper;

    @Autowired
    private LabTeacherRelationMapper labTeacherRelationMapper;

    @Autowired
    private UserAccessService userAccessService;

    @Override
    @Transactional
    public boolean createApply(LabCreateApplyCreateDTO createDTO, User currentUser) {
        if (!currentUserAccessor.isTeacherIdentity(currentUser)) {
            throw new RuntimeException("只有教师身份可以提交实验室创建申请");
        }
        currentUserAccessor.assertTeacherOrAdmin(currentUser);
        Long managedCollegeId = resolveManagedCollegeId(currentUser);
        if (managedCollegeId == null && currentUserAccessor.isTeacherIdentity(currentUser)) {
            Long teacherCollegeId = resolveTeacherCollegeId(currentUser);
            if (teacherCollegeId == null) {
                throw new RuntimeException("当前教师账号未绑定所属学院");
            }
            if (!teacherCollegeId.equals(createDTO.getCollegeId())) {
                throw new RuntimeException("教师只能为自己所属学院提交实验室创建申请");
            }
        }
        if (managedCollegeId != null && !managedCollegeId.equals(createDTO.getCollegeId())) {
            throw new RuntimeException("仅可为自己管理的学院提交创建申请");
        }

        String labName = trimToNull(createDTO.getLabName());

        QueryWrapper<LabCreateApply> duplicateApplyQuery = new QueryWrapper<>();
        duplicateApplyQuery.eq("deleted", 0)
                .eq("lab_name", labName)
                .in("status", STATUS_SUBMITTED, STATUS_COLLEGE_APPROVED, STATUS_APPROVED);
        if (labCreateApplyMapper.selectCount(duplicateApplyQuery) > 0) {
            throw new RuntimeException("同名实验室创建申请已存在");
        }

        QueryWrapper<Lab> duplicateLabQuery = new QueryWrapper<>();
        duplicateLabQuery.eq("deleted", 0).eq("lab_name", labName);
        if (labService.count(duplicateLabQuery) > 0) {
            throw new RuntimeException("同名实验室已存在");
        }

        LabCreateApply apply = new LabCreateApply();
        apply.setApplicantUserId(currentUser.getId());
        apply.setCollegeId(createDTO.getCollegeId());
        apply.setLabName(labName);
        apply.setTeacherName(trimToNull(createDTO.getTeacherName()));
        apply.setLocation(trimToNull(createDTO.getLocation()));
        apply.setContactEmail(trimToNull(createDTO.getContactEmail()));
        apply.setResearchDirection(trimToNull(createDTO.getResearchDirection()));
        apply.setApplyReason(trimToNull(createDTO.getApplyReason()));
        apply.setStatus(STATUS_SUBMITTED);
        return labCreateApplyMapper.insert(apply) > 0;
    }

    @Override
    public Page<Map<String, Object>> getApplyPage(Integer pageNum, Integer pageSize, String status, String keyword, User currentUser) {
        currentUserAccessor.assertTeacherOrAdmin(currentUser);
        Long applicantUserId = null;
        Long collegeId = null;

        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            collegeId = resolveManagedCollegeId(currentUser);
            if (collegeId == null) {
                applicantUserId = currentUser.getId();
            }
        }

        return labCreateApplyMapper.selectApplyPage(
                new Page<>(pageNum, pageSize),
                applicantUserId,
                collegeId,
                trimToNull(status),
                trimToNull(keyword)
        );
    }

    @Override
    @Transactional
    public boolean auditApply(Long id, LabCreateApplyAuditDTO auditDTO, User currentUser) {
        LabCreateApply apply = labCreateApplyMapper.selectById(id);
        if (apply == null || (apply.getDeleted() != null && apply.getDeleted() == 1)) {
            throw new RuntimeException("实验室创建申请不存在");
        }

        String action = trimToNull(auditDTO.getAction());
        if (!StringUtils.hasText(action)) {
            throw new RuntimeException("审核动作不能为空");
        }

        if ("collegeApprove".equalsIgnoreCase(action)) {
            assertCollegeAuditor(currentUser, apply.getCollegeId());
            if (!STATUS_SUBMITTED.equalsIgnoreCase(apply.getStatus())) {
                throw new RuntimeException("仅待学院审核的申请可执行学院通过");
            }
            apply.setStatus(STATUS_COLLEGE_APPROVED);
            apply.setCollegeAuditBy(currentUser.getId());
            apply.setCollegeAuditTime(LocalDateTime.now());
            apply.setCollegeAuditComment(trimToNull(auditDTO.getAuditComment()));
            return labCreateApplyMapper.updateById(apply) > 0;
        }

        if ("schoolApprove".equalsIgnoreCase(action)) {
            currentUserAccessor.assertSuperAdmin(currentUser);
            if (!STATUS_COLLEGE_APPROVED.equalsIgnoreCase(apply.getStatus())) {
                throw new RuntimeException("仅学院初审通过的申请可执行学校通过");
            }

            QueryWrapper<Lab> duplicateLabQuery = new QueryWrapper<>();
            duplicateLabQuery.eq("deleted", 0).eq("lab_name", apply.getLabName());
            if (labService.count(duplicateLabQuery) > 0) {
                throw new RuntimeException("同名实验室已存在");
            }

            Lab lab = new Lab();
            lab.setLabName(apply.getLabName());
            lab.setCollegeId(apply.getCollegeId());
            lab.setTeacherName(apply.getTeacherName());
            lab.setLocation(apply.getLocation());
            lab.setContactEmail(apply.getContactEmail());
            lab.setLabDesc(StringUtils.hasText(apply.getResearchDirection()) ? apply.getResearchDirection() : apply.getApplyReason());
            lab.setRequireSkill("待补充");
            lab.setRecruitNum(20);
            lab.setStatus(1);
            lab.setBasicInfo(apply.getApplyReason());
            if (!labService.createLab(lab)) {
                throw new RuntimeException("根据申请创建实验室失败");
            }

            apply.setStatus(STATUS_APPROVED);
            apply.setSchoolAuditBy(currentUser.getId());
            apply.setSchoolAuditTime(LocalDateTime.now());
            apply.setSchoolAuditComment(trimToNull(auditDTO.getAuditComment()));
            apply.setGeneratedLabId(lab.getId());
            boolean updated = labCreateApplyMapper.updateById(apply) > 0;
            if (updated) {
                bindApprovedTeacherLab(apply, lab);
            }
            return updated;
        }

        if ("reject".equalsIgnoreCase(action)) {
            return rejectApply(apply, currentUser, trimToNull(auditDTO.getAuditComment()));
        }

        throw new RuntimeException("不支持的审核动作");
    }

    private boolean rejectApply(LabCreateApply apply, User currentUser, String auditComment) {
        if (STATUS_APPROVED.equalsIgnoreCase(apply.getStatus())) {
            throw new RuntimeException("已通过的申请不可驳回");
        }

        String currentStatus = trimToNull(apply.getStatus());
        if (STATUS_COLLEGE_APPROVED.equalsIgnoreCase(currentStatus)) {
            currentUserAccessor.assertSuperAdmin(currentUser);
            apply.setSchoolAuditBy(currentUser.getId());
            apply.setSchoolAuditTime(LocalDateTime.now());
            apply.setSchoolAuditComment(auditComment);
        } else {
            assertCollegeAuditor(currentUser, apply.getCollegeId());
            apply.setCollegeAuditBy(currentUser.getId());
            apply.setCollegeAuditTime(LocalDateTime.now());
            apply.setCollegeAuditComment(auditComment);
        }

        apply.setStatus(STATUS_REJECTED);
        return labCreateApplyMapper.updateById(apply) > 0;
    }

    private void assertCollegeAuditor(User currentUser, Long collegeId) {
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            return;
        }

        Long managedCollegeId = resolveManagedCollegeId(currentUser);
        if (managedCollegeId == null || !managedCollegeId.equals(collegeId)) {
            throw new RuntimeException("无权审核该学院的创建申请");
        }
    }

    private Long resolveManagedCollegeId(User currentUser) {
        if (currentUser == null || currentUser.getId() == null || currentUserAccessor.isSuperAdmin(currentUser)) {
            return null;
        }

        QueryWrapper<College> collegeQuery = new QueryWrapper<>();
        collegeQuery.eq("deleted", 0)
                .eq("admin_user_id", currentUser.getId())
                .last("LIMIT 1");
        College managedCollege = collegeMapper.selectOne(collegeQuery);
        return managedCollege == null ? null : managedCollege.getId();
    }

    private Long resolveTeacherCollegeId(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }

        QueryWrapper<UserIdentity> identityQuery = new QueryWrapper<>();
        identityQuery.eq("user_id", currentUser.getId())
                .eq("identity_type", TEACHER_IDENTITY)
                .eq("deleted", 0)
                .eq("status", IDENTITY_STATUS_ACTIVE)
                .orderByAsc("id")
                .last("LIMIT 1");
        UserIdentity teacherIdentity = userIdentityMapper.selectOne(identityQuery);
        if (teacherIdentity != null && teacherIdentity.getCollegeId() != null) {
            return teacherIdentity.getCollegeId();
        }

        String collegeName = trimToNull(currentUser.getCollege());
        if (!StringUtils.hasText(collegeName)) {
            return null;
        }

        QueryWrapper<College> collegeQuery = new QueryWrapper<>();
        collegeQuery.eq("deleted", 0)
                .eq("college_name", collegeName)
                .last("LIMIT 1");
        College teacherCollege = collegeMapper.selectOne(collegeQuery);
        return teacherCollege == null ? null : teacherCollege.getId();
    }

    private void bindApprovedTeacherLab(LabCreateApply apply, Lab lab) {
        if (apply == null || apply.getApplicantUserId() == null || lab == null || lab.getId() == null) {
            return;
        }

        User applicant = userMapper.selectById(apply.getApplicantUserId());
        if (applicant == null || !currentUserAccessor.isTeacherIdentity(applicant)) {
            return;
        }

        QueryWrapper<LabTeacherRelation> relationQuery = new QueryWrapper<>();
        relationQuery.eq("user_id", applicant.getId())
                .eq("lab_id", lab.getId())
                .eq("deleted", 0)
                .last("LIMIT 1");
        LabTeacherRelation relation = labTeacherRelationMapper.selectOne(relationQuery);
        if (relation == null) {
            relation = new LabTeacherRelation();
            relation.setUserId(applicant.getId());
            relation.setLabId(lab.getId());
            relation.setIsPrimary(1);
            relation.setStatus("active");
            relation.setRemark(trimToNull(apply.getResearchDirection()));
            labTeacherRelationMapper.insert(relation);
        } else {
            relation.setIsPrimary(1);
            relation.setStatus("active");
            relation.setRemark(trimToNull(apply.getResearchDirection()));
            labTeacherRelationMapper.updateById(relation);
        }

        userAccessService.refreshCompatibilityAccess(applicant.getId());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
