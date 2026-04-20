package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.LabInfoChangeReview;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabInfoChangeReviewMapper;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.AuditLogService;
import com.lab.recruitment.service.LabInfoChangeReviewService;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.SystemNotificationService;
import com.lab.recruitment.support.CurrentUserAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LabInfoChangeReviewServiceImpl implements LabInfoChangeReviewService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    @Autowired
    private LabInfoChangeReviewMapper labInfoChangeReviewMapper;

    @Autowired
    private LabMapper labMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LabService labService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SystemNotificationService systemNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Map<String, Object> submitChange(Lab requestedLab, User currentUser) {
        if (requestedLab == null || requestedLab.getId() == null) {
            throw new RuntimeException("实验室ID不能为空");
        }
        if (!currentUserAccessor.isAdmin(currentUser)) {
            throw new RuntimeException("当前账号无权提交实验室资料变更");
        }
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            throw new RuntimeException("学校管理员请直接修改正式实验室信息");
        }

        Long scopedLabId = resolveManageableLabId(currentUser, requestedLab.getId(), true);
        Lab officialLab = mustGetLab(scopedLabId);
        assertNoPendingReview(scopedLabId);

        Lab snapshotLab = buildRequestedSnapshot(officialLab, requestedLab);
        validateRequestedSnapshot(snapshotLab);

        LabInfoChangeReview review = new LabInfoChangeReview();
        review.setLabId(scopedLabId);
        review.setVersionNo(nextVersionNo(scopedLabId));
        review.setApplicantUserId(currentUser.getId());
        review.setReviewStatus(STATUS_PENDING);
        review.setReviewSnapshot(writeSnapshot(snapshotLab));
        review.setCreatedBy(currentUser.getId());
        review.setUpdatedBy(currentUser.getId());
        labInfoChangeReviewMapper.insert(review);

        auditLogService.record(currentUser.getId(), "lab_info_change_submit", "lab", scopedLabId,
                "reviewId=" + review.getId() + ",version=" + review.getVersionNo());
        notifySchoolAdmins(
                "实验室资料变更待审核",
                "实验室“" + officialLab.getLabName() + "”提交了新的资料变更申请，请及时审核。",
                review.getId()
        );

        return buildReviewMap(review, officialLab, loadUser(review.getApplicantUserId()));
    }

    @Override
    public Page<Map<String, Object>> getPendingReviewPage(Integer pageNum, Integer pageSize, String keyword,
                                                          Long collegeId, Long labId, User currentUser) {
        currentUserAccessor.assertSuperAdmin(currentUser);

        long current = Math.max(pageNum == null ? 1 : pageNum, 1);
        long size = Math.max(pageSize == null ? 10 : pageSize, 1);
        long offset = (current - 1) * size;
        String normalizedKeyword = trimToNull(keyword);

        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE r.deleted = 0 AND r.review_status = ?");
        args.add(STATUS_PENDING);
        if (collegeId != null) {
            where.append(" AND l.college_id = ?");
            args.add(collegeId);
        }
        if (labId != null) {
            where.append(" AND r.lab_id = ?");
            args.add(labId);
        }
        if (normalizedKeyword != null) {
            where.append(" AND (l.lab_name LIKE ? OR u.real_name LIKE ? OR u.username LIKE ?)");
            args.add("%" + normalizedKeyword + "%");
            args.add("%" + normalizedKeyword + "%");
            args.add("%" + normalizedKeyword + "%");
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_lab_info_change_review r " +
                        "LEFT JOIN t_lab l ON l.id = r.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_user u ON u.id = r.applicant_user_id AND u.deleted = 0" +
                        where,
                Long.class,
                args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(offset);
        queryArgs.add(size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT r.id AS reviewId, r.lab_id AS labId, r.version_no AS versionNo, " +
                        "r.applicant_user_id AS applicantUserId, r.review_status AS reviewStatus, " +
                        "r.review_comment AS reviewComment, r.review_snapshot AS reviewSnapshot, " +
                        "r.review_time AS reviewTime, r.create_time AS submitTime, r.update_time AS updateTime, " +
                        "l.lab_name AS labName, l.lab_code AS labCode, l.college_id AS collegeId, " +
                        "c.college_name AS collegeName, u.real_name AS applicantName, " +
                        "u.username AS applicantUsername, u.student_id AS applicantStudentId " +
                        "FROM t_lab_info_change_review r " +
                        "LEFT JOIN t_lab l ON l.id = r.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_college c ON c.id = l.college_id AND c.deleted = 0 " +
                        "LEFT JOIN t_user u ON u.id = r.applicant_user_id AND u.deleted = 0" +
                        where +
                        " ORDER BY r.create_time ASC, r.id ASC LIMIT ?, ?",
                queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>(row);
            Map<String, Object> snapshot = parseSnapshot(asString(row.get("reviewSnapshot")));
            item.remove("reviewSnapshot");
            item.put("snapshot", snapshot);
            Object labIdValue = row.get("labId");
            if (labIdValue != null) {
                Lab officialLab = labMapper.selectById(Long.parseLong(String.valueOf(labIdValue)));
                item.put("official", officialLab == null ? null : buildOfficialSnapshot(officialLab));
            } else {
                item.put("official", null);
            }
            item.put("proposedLabName", snapshot.get("labName"));
            records.add(item);
        }

        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total == null ? 0L : total);
        return page;
    }

    @Override
    public List<Map<String, Object>> getReviewHistory(Long labId, User currentUser) {
        Long scopedLabId = resolveManageableLabId(currentUser, labId, false);
        if (scopedLabId == null) {
            throw new RuntimeException("请选择实验室");
        }

        QueryWrapper<LabInfoChangeReview> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", scopedLabId)
                .eq("deleted", 0)
                .orderByDesc("version_no")
                .orderByDesc("id");
        List<LabInfoChangeReview> reviewList = labInfoChangeReviewMapper.selectList(queryWrapper);

        Lab officialLab = mustGetLab(scopedLabId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LabInfoChangeReview review : reviewList) {
            result.add(buildReviewMap(review, officialLab, loadUser(review.getApplicantUserId())));
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> approveReview(Long reviewId, String reviewComment, User currentUser) {
        currentUserAccessor.assertSuperAdmin(currentUser);

        LabInfoChangeReview review = mustGetReview(reviewId);
        assertPending(review);

        Lab approvedLab = readSnapshotAsLab(review);
        approvedLab.setId(review.getLabId());
        labService.updateLab(approvedLab);

        LocalDateTime now = LocalDateTime.now();
        review.setReviewerId(currentUser.getId());
        review.setReviewStatus(STATUS_APPROVED);
        review.setReviewComment(requireReviewComment(reviewComment));
        review.setReviewTime(now);
        review.setUpdatedBy(currentUser.getId());
        labInfoChangeReviewMapper.updateById(review);

        auditLogService.record(currentUser.getId(), "lab_info_change_approve", "lab", review.getLabId(),
                "reviewId=" + review.getId() + ",version=" + review.getVersionNo());
        systemNotificationService.createNotificationAsync(
                review.getApplicantUserId(),
                "实验室资料变更已通过",
                "你提交的实验室资料变更申请已审核通过，最新信息现已生效。",
                "LAB_INFO_REVIEWED",
                review.getId(),
                "/admin/labs"
        );

        return buildReviewMap(review, mustGetLab(review.getLabId()), loadUser(review.getApplicantUserId()));
    }

    @Override
    @Transactional
    public Map<String, Object> rejectReview(Long reviewId, String reviewComment, User currentUser) {
        currentUserAccessor.assertSuperAdmin(currentUser);

        LabInfoChangeReview review = mustGetReview(reviewId);
        assertPending(review);

        LocalDateTime now = LocalDateTime.now();
        review.setReviewerId(currentUser.getId());
        review.setReviewStatus(STATUS_REJECTED);
        review.setReviewComment(requireReviewComment(reviewComment));
        review.setReviewTime(now);
        review.setUpdatedBy(currentUser.getId());
        labInfoChangeReviewMapper.updateById(review);

        auditLogService.record(currentUser.getId(), "lab_info_change_reject", "lab", review.getLabId(),
                "reviewId=" + review.getId() + ",version=" + review.getVersionNo());
        systemNotificationService.createNotificationAsync(
                review.getApplicantUserId(),
                "实验室资料变更被驳回",
                "你提交的实验室资料变更申请被驳回，请根据审核意见修改后重新提交。",
                "LAB_INFO_REVIEWED",
                review.getId(),
                "/admin/labs"
        );

        return buildReviewMap(review, mustGetLab(review.getLabId()), loadUser(review.getApplicantUserId()));
    }

    private Long resolveManageableLabId(User currentUser, Long requestedLabId, boolean requireRequestedForBroadScope) {
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            if (requestedLabId == null) {
                throw new RuntimeException("请选择实验室");
            }
            return requestedLabId;
        }
        if (!currentUserAccessor.isAdmin(currentUser)) {
            throw new RuntimeException("当前账号无权访问实验室资料审核");
        }
        if (currentUserAccessor.isLabManager(currentUser)) {
            return currentUserAccessor.resolveLabScope(currentUser, requestedLabId);
        }
        if (requestedLabId == null && requireRequestedForBroadScope) {
            throw new RuntimeException("请选择实验室");
        }
        return currentUserAccessor.resolveManagementScope(currentUser, null, requestedLabId).getLabId();
    }

    private void validateRequestedSnapshot(Lab requestedLab) {
        if (!StringUtils.hasText(requestedLab.getLabName())) {
            throw new RuntimeException("实验室名称不能为空");
        }
        if (!StringUtils.hasText(requestedLab.getLabCode())) {
            throw new RuntimeException("实验室编码不能为空");
        }
        if (requestedLab.getRecruitNum() == null || requestedLab.getRecruitNum() < 1) {
            throw new RuntimeException("计划容量必须大于 0");
        }
        int currentMembers = labService.countCurrentMembers(requestedLab.getId());
        if (requestedLab.getRecruitNum() < currentMembers) {
            throw new RuntimeException("计划容量不能低于当前正式成员人数");
        }
        if (!StringUtils.hasText(requestedLab.getLabDesc())) {
            throw new RuntimeException("实验室简介不能为空");
        }
    }

    private void assertNoPendingReview(Long labId) {
        QueryWrapper<LabInfoChangeReview> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("deleted", 0)
                .eq("review_status", STATUS_PENDING)
                .last("LIMIT 1");
        LabInfoChangeReview existing = labInfoChangeReviewMapper.selectOne(queryWrapper);
        if (existing != null) {
            throw new RuntimeException("该实验室已有待审核的资料变更申请，请等待学校管理员处理后再提交");
        }
    }

    private int nextVersionNo(Long labId) {
        QueryWrapper<LabInfoChangeReview> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("deleted", 0)
                .orderByDesc("version_no")
                .orderByDesc("id")
                .last("LIMIT 1");
        LabInfoChangeReview latest = labInfoChangeReviewMapper.selectOne(queryWrapper);
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    private LabInfoChangeReview mustGetReview(Long reviewId) {
        LabInfoChangeReview review = labInfoChangeReviewMapper.selectById(reviewId);
        if (review == null || Objects.equals(review.getDeleted(), 1)) {
            throw new RuntimeException("实验室资料审核记录不存在");
        }
        return review;
    }

    private void assertPending(LabInfoChangeReview review) {
        if (!STATUS_PENDING.equals(review.getReviewStatus())) {
            throw new RuntimeException("该审核记录已处理");
        }
    }

    private Lab mustGetLab(Long labId) {
        Lab lab = labMapper.selectById(labId);
        if (lab == null || Objects.equals(lab.getDeleted(), 1)) {
            throw new RuntimeException("实验室不存在");
        }
        return lab;
    }

    private Lab buildRequestedSnapshot(Lab officialLab, Lab requestedLab) {
        Lab snapshot = new Lab();
        snapshot.setId(officialLab.getId());
        snapshot.setCollegeId(officialLab.getCollegeId());
        snapshot.setCurrentNum(labService.countCurrentMembers(officialLab.getId()));
        snapshot.setLabName(normalizeText(requestedLab.getLabName(), officialLab.getLabName()));
        snapshot.setLabCode(normalizeText(requestedLab.getLabCode(), officialLab.getLabCode()));
        snapshot.setLabDesc(normalizeText(requestedLab.getLabDesc(), officialLab.getLabDesc()));
        snapshot.setTeacherName(normalizeText(requestedLab.getTeacherName(), officialLab.getTeacherName()));
        snapshot.setLocation(normalizeText(requestedLab.getLocation(), officialLab.getLocation()));
        snapshot.setContactEmail(normalizeText(requestedLab.getContactEmail(), officialLab.getContactEmail()));
        snapshot.setRequireSkill(normalizeText(requestedLab.getRequireSkill(), officialLab.getRequireSkill()));
        snapshot.setRecruitNum(requestedLab.getRecruitNum() == null ? officialLab.getRecruitNum() : requestedLab.getRecruitNum());
        snapshot.setStatus(requestedLab.getStatus() == null ? officialLab.getStatus() : requestedLab.getStatus());
        snapshot.setFoundingDate(normalizeText(requestedLab.getFoundingDate(), officialLab.getFoundingDate()));
        snapshot.setAwards(normalizeText(requestedLab.getAwards(), officialLab.getAwards()));
        snapshot.setBasicInfo(normalizeText(requestedLab.getBasicInfo(), officialLab.getBasicInfo()));
        snapshot.setAdvisors(normalizeText(requestedLab.getAdvisors(), officialLab.getAdvisors()));
        snapshot.setCurrentAdmins(normalizeText(requestedLab.getCurrentAdmins(), officialLab.getCurrentAdmins()));
        return snapshot;
    }

    private String writeSnapshot(Lab lab) {
        try {
            return objectMapper.writeValueAsString(buildSnapshotModel(lab));
        } catch (Exception e) {
            throw new RuntimeException("实验室资料快照序列化失败");
        }
    }

    private Map<String, Object> buildSnapshotModel(Lab lab) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", lab.getId());
        snapshot.put("labName", lab.getLabName());
        snapshot.put("labCode", lab.getLabCode());
        snapshot.put("collegeId", lab.getCollegeId());
        snapshot.put("teacherName", lab.getTeacherName());
        snapshot.put("location", lab.getLocation());
        snapshot.put("contactEmail", lab.getContactEmail());
        snapshot.put("labDesc", lab.getLabDesc());
        snapshot.put("requireSkill", lab.getRequireSkill());
        snapshot.put("recruitNum", lab.getRecruitNum());
        snapshot.put("currentNum", lab.getCurrentNum());
        snapshot.put("status", lab.getStatus());
        snapshot.put("foundingDate", lab.getFoundingDate());
        snapshot.put("awards", lab.getAwards());
        snapshot.put("basicInfo", lab.getBasicInfo());
        snapshot.put("advisors", lab.getAdvisors());
        snapshot.put("currentAdmins", lab.getCurrentAdmins());
        return snapshot;
    }

    private Map<String, Object> buildOfficialSnapshot(Lab lab) {
        Map<String, Object> snapshot = buildSnapshotModel(lab);
        snapshot.put("collegeName", loadCollegeName(lab.getCollegeId()));
        return snapshot;
    }

    private Map<String, Object> parseSnapshot(String rawSnapshot) {
        if (!StringUtils.hasText(rawSnapshot)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> snapshot = objectMapper.readValue(
                    rawSnapshot,
                    new TypeReference<LinkedHashMap<String, Object>>() { }
            );
            Object collegeIdValue = snapshot.get("collegeId");
            if (collegeIdValue != null) {
                snapshot.put("collegeName", loadCollegeName(Long.parseLong(String.valueOf(collegeIdValue))));
            }
            return snapshot;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private Lab readSnapshotAsLab(LabInfoChangeReview review) {
        try {
            Lab lab = objectMapper.readValue(review.getReviewSnapshot(), Lab.class);
            lab.setId(review.getLabId());
            lab.setCollegeId(mustGetLab(review.getLabId()).getCollegeId());
            return lab;
        } catch (Exception e) {
            throw new RuntimeException("实验室资料快照解析失败");
        }
    }

    private Map<String, Object> buildReviewMap(LabInfoChangeReview review, Lab officialLab, User applicant) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", review.getId());
        result.put("reviewId", review.getId());
        result.put("labId", review.getLabId());
        result.put("versionNo", review.getVersionNo());
        result.put("applicantUserId", review.getApplicantUserId());
        result.put("applicantName", applicant == null ? null : applicant.getRealName());
        result.put("applicantUsername", applicant == null ? null : applicant.getUsername());
        result.put("applicantStudentId", applicant == null ? null : applicant.getStudentId());
        result.put("reviewerId", review.getReviewerId());
        result.put("reviewStatus", review.getReviewStatus());
        result.put("reviewComment", review.getReviewComment());
        result.put("reviewTime", review.getReviewTime());
        result.put("submitTime", review.getCreateTime());
        result.put("createTime", review.getCreateTime());
        result.put("updateTime", review.getUpdateTime());
        result.put("labName", officialLab == null ? null : officialLab.getLabName());
        result.put("collegeId", officialLab == null ? null : officialLab.getCollegeId());
        result.put("collegeName", officialLab == null ? null : loadCollegeName(officialLab.getCollegeId()));
        Map<String, Object> snapshot = parseSnapshot(review.getReviewSnapshot());
        result.put("snapshot", snapshot);
        result.put("official", officialLab == null ? null : buildOfficialSnapshot(officialLab));
        result.put("proposedLabName", snapshot.get("labName"));
        return result;
    }

    private String requireReviewComment(String reviewComment) {
        String normalized = trimToNull(reviewComment);
        if (normalized == null) {
            throw new RuntimeException("审核意见不能为空");
        }
        return normalized;
    }

    private User loadUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    private String loadCollegeName(Long collegeId) {
        if (collegeId == null) {
            return null;
        }
        return jdbcTemplate.query(
                "SELECT college_name FROM t_college WHERE id = ? AND deleted = 0 LIMIT 1",
                ps -> ps.setLong(1, collegeId),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    private void notifySchoolAdmins(String title, String content, Long relatedId) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role", "super_admin")
                .eq("deleted", 0);
        List<User> schoolAdmins = userMapper.selectList(queryWrapper);
        for (User schoolAdmin : schoolAdmins) {
            if (schoolAdmin == null || schoolAdmin.getId() == null) {
                continue;
            }
            systemNotificationService.createNotificationAsync(
                    schoolAdmin.getId(),
                    title,
                    content,
                    "LAB_INFO_REVIEW",
                    relatedId,
                    "/admin/labs"
            );
        }
    }

    private String normalizeText(String requestedValue, String fallbackValue) {
        if (requestedValue == null) {
            return fallbackValue;
        }
        String trimmed = requestedValue.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
