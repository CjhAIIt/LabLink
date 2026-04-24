package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.recruitment.dto.StudentProfileSaveDTO;
import com.lab.recruitment.entity.StudentProfile;
import com.lab.recruitment.entity.StudentProfileArchive;
import com.lab.recruitment.entity.StudentProfileReview;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.CollegeMapper;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.mapper.StudentProfileArchiveMapper;
import com.lab.recruitment.mapper.StudentProfileMapper;
import com.lab.recruitment.mapper.StudentProfileReviewMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.AuditLogService;
import com.lab.recruitment.service.StatisticsRefreshService;
import com.lab.recruitment.service.StudentProfileService;
import com.lab.recruitment.service.SystemNotificationService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.DataScope;
import com.lab.recruitment.support.DataScopeLevel;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class StudentProfileServiceImpl implements StudentProfileService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    @Autowired
    private StudentProfileMapper studentProfileMapper;

    @Autowired
    private StudentProfileReviewMapper studentProfileReviewMapper;

    @Autowired
    private StudentProfileArchiveMapper studentProfileArchiveMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LabMapper labMapper;

    @Autowired
    private CollegeMapper collegeMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SystemNotificationService systemNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StatisticsRefreshService statisticsRefreshService;

    @Override
    public Map<String, Object> getMyProfile(User currentUser) {
        assertStudentOwner(currentUser);
        StudentProfile profile = findActiveProfileByUserId(currentUser.getId());
        if (profile == null) {
            return buildTransientProfile(currentUser);
        }
        return buildProfileDetailMap(profile, currentUser);
    }

    @Override
    @Transactional
    public Map<String, Object> saveMyProfile(StudentProfileSaveDTO saveDTO, User currentUser) {
        assertStudentOwner(currentUser);
        StudentProfile profile = findActiveProfileByUserId(currentUser.getId());
        if (profile != null && STATUS_PENDING.equals(profile.getStatus())) {
            throw new RuntimeException("当前资料正在审核中，暂不可编辑");
        }

        DataScope currentScope = currentUserAccessor.getCurrentDataScope();
        Long scopedLabId = currentScope.getLabId();
        Long scopedCollegeId = resolveProfileCollegeId(saveDTO.getCollegeId(), scopedLabId, currentUser, false);

        boolean isNew = profile == null;
        if (isNew) {
            profile = new StudentProfile();
            profile.setUserId(currentUser.getId());
            profile.setStatus(STATUS_DRAFT);
            profile.setCurrentVersion(0);
            profile.setCreatedBy(currentUser.getId());
        } else if (!STATUS_DRAFT.equals(profile.getStatus())) {
            profile.setStatus(STATUS_DRAFT);
        }

        profile.setLabId(scopedLabId);
        profile.setCollegeId(scopedCollegeId);
        profile.setStudentNo(defaultValue(trimToNull(saveDTO.getStudentNo()), trimToNull(currentUser.getStudentId())));
        profile.setRealName(defaultValue(trimToNull(saveDTO.getRealName()), trimToNull(currentUser.getRealName())));
        profile.setGender(trimToNull(saveDTO.getGender()));
        profile.setMajor(defaultValue(trimToNull(saveDTO.getMajor()), trimToNull(currentUser.getMajor())));
        profile.setClassName(trimToNull(saveDTO.getClassName()));
        profile.setPhone(defaultValue(trimToNull(saveDTO.getPhone()), trimToNull(currentUser.getPhone())));
        profile.setEmail(defaultValue(normalizeEmail(saveDTO.getEmail()), normalizeEmail(currentUser.getEmail())));
        profile.setDirection(trimToNull(saveDTO.getDirection()));
        profile.setIntroduction(trimToNull(saveDTO.getIntroduction()));
        profile.setAttachmentUrl(trimToNull(saveDTO.getAttachmentUrl()));
        profile.setUpdatedBy(currentUser.getId());

        if (isNew) {
            studentProfileMapper.insert(profile);
        } else {
            studentProfileMapper.updateById(profile);
        }

        syncUserBasicFields(profile, currentUser.getId());
        publishProfileStatisticsRefresh(profile, currentUser.getId(), isNew ? "profile_save_create" : "profile_save_update");
        return buildProfileDetailMap(profile, currentUser);
    }

    @Override
    @Transactional
    public Map<String, Object> submitMyProfile(User currentUser) {
        assertStudentOwner(currentUser);
        if (!requiresProfileReview(currentUser)) {
            throw new RuntimeException("当前账号尚未加入实验室，资料保存后会直接生效，无需提交审核");
        }
        StudentProfile profile = findActiveProfileByUserId(currentUser.getId());
        if (profile == null) {
            throw new RuntimeException("请先保存资料后再提交审核");
        }
        if (STATUS_PENDING.equals(profile.getStatus())) {
            throw new RuntimeException("当前资料已在审核中");
        }

        validateProfileBeforeSubmit(profile);
        int nextVersion = profile.getCurrentVersion() == null ? 1 : profile.getCurrentVersion() + 1;
        LocalDateTime now = LocalDateTime.now();

        StudentProfileReview review = new StudentProfileReview();
        review.setProfileId(profile.getId());
        review.setVersionNo(nextVersion);
        review.setReviewStatus(STATUS_PENDING);
        review.setReviewSnapshot(writeSnapshot(profile));
        review.setCreatedBy(currentUser.getId());
        review.setUpdatedBy(currentUser.getId());
        studentProfileReviewMapper.insert(review);

        profile.setCurrentVersion(nextVersion);
        profile.setStatus(STATUS_PENDING);
        profile.setSubmittedAt(now);
        profile.setUpdatedBy(currentUser.getId());
        studentProfileMapper.updateById(profile);

        auditLogService.record(currentUser.getId(), "student_profile_submit", "student_profile", profile.getId(),
                "version=" + nextVersion);
        publishProfileStatisticsRefresh(profile, currentUser.getId(), "profile_submit");
        return buildProfileDetailMap(profile, currentUser);
    }

    @Override
    public Page<Map<String, Object>> getProfilePage(Integer pageNum, Integer pageSize, String keyword, String status,
                                                    Long collegeId, Long labId, User currentUser) {
        DataScope queryScope = resolveReadableScope(currentUser, collegeId, labId);
        long current = Math.max(pageNum == null ? 1 : pageNum, 1);
        long size = Math.max(pageSize == null ? 10 : pageSize, 1);
        long offset = (current - 1) * size;

        String normalizedKeyword = trimToNull(keyword);
        String normalizedStatus = normalizeStatus(status, false);

        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE p.deleted = 0");
        appendScopeCondition(where, args, queryScope);
        if (normalizedStatus != null) {
            where.append(" AND p.status = ?");
            args.add(normalizedStatus);
        }
        if (normalizedKeyword != null) {
            where.append(" AND (p.real_name LIKE ? OR p.student_no LIKE ? OR p.major LIKE ? OR p.class_name LIKE ?)");
            args.add("%" + normalizedKeyword + "%");
            args.add("%" + normalizedKeyword + "%");
            args.add("%" + normalizedKeyword + "%");
            args.add("%" + normalizedKeyword + "%");
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_student_profile p" + where,
                Long.class,
                args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(offset);
        queryArgs.add(size);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT p.id, p.user_id AS userId, p.lab_id AS labId, l.lab_name AS labName, " +
                        "p.student_no AS studentNo, p.real_name AS realName, p.gender, " +
                        "p.college_id AS collegeId, c.college_name AS collegeName, p.major, " +
                        "p.class_name AS className, p.phone, p.email, p.direction, p.introduction, p.attachment_url AS attachmentUrl, " +
                        "p.status, p.current_version AS currentVersion, p.submitted_at AS submittedAt, " +
                        "p.last_review_time AS lastReviewTime, p.create_time AS createTime, p.update_time AS updateTime " +
                        "FROM t_student_profile p " +
                        "LEFT JOIN t_lab l ON l.id = p.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_college c ON c.id = p.college_id AND c.deleted = 0" +
                        where +
                        " ORDER BY p.update_time DESC, p.id DESC LIMIT ?, ?",
                queryArgs.toArray()
        );

        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total == null ? 0L : total);
        return page;
    }

    @Override
    public Map<String, Object> getProfileDetail(Long profileId, User currentUser) {
        StudentProfile profile = mustGetProfile(profileId);
        assertCanAccessProfile(profile, currentUser);
        return buildProfileDetailMap(profile, currentUser);
    }

    @Override
    public List<Map<String, Object>> getProfileReviewHistory(Long profileId, User currentUser) {
        StudentProfile profile = mustGetProfile(profileId);
        assertCanAccessProfile(profile, currentUser);

        QueryWrapper<StudentProfileReview> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("profile_id", profileId)
                .eq("deleted", 0)
                .orderByDesc("version_no")
                .orderByDesc("id");
        List<StudentProfileReview> reviewList = studentProfileReviewMapper.selectList(queryWrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentProfileReview review : reviewList) {
            result.add(buildReviewMap(review));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getProfileArchiveHistory(Long profileId, User currentUser) {
        StudentProfile profile = mustGetProfile(profileId);
        assertCanAccessProfile(profile, currentUser);

        QueryWrapper<StudentProfileArchive> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("profile_id", profileId)
                .eq("deleted", 0)
                .orderByDesc("version_no")
                .orderByDesc("id");
        List<StudentProfileArchive> archiveList = studentProfileArchiveMapper.selectList(queryWrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentProfileArchive archive : archiveList) {
            result.add(buildArchiveMap(archive));
        }
        return result;
    }

    @Override
    public Page<Map<String, Object>> getPendingReviewPage(Integer pageNum, Integer pageSize, String keyword,
                                                          Long collegeId, Long labId, User currentUser) {
        DataScope queryScope = currentUserAccessor.resolveManagementScope(currentUser, collegeId, labId);
        long current = Math.max(pageNum == null ? 1 : pageNum, 1);
        long size = Math.max(pageSize == null ? 10 : pageSize, 1);
        long offset = (current - 1) * size;

        String normalizedKeyword = trimToNull(keyword);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE r.deleted = 0 AND p.deleted = 0 AND r.review_status = ?");
        args.add(STATUS_PENDING);
        appendScopeCondition(where, args, queryScope);
        if (normalizedKeyword != null) {
            where.append(" AND (p.real_name LIKE ? OR p.student_no LIKE ? OR p.major LIKE ? OR p.class_name LIKE ?)");
            args.add("%" + normalizedKeyword + "%");
            args.add("%" + normalizedKeyword + "%");
            args.add("%" + normalizedKeyword + "%");
            args.add("%" + normalizedKeyword + "%");
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_student_profile_review r " +
                        "LEFT JOIN t_student_profile p ON p.id = r.profile_id" +
                        where,
                Long.class,
                args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(offset);
        queryArgs.add(size);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT r.id AS reviewId, r.profile_id AS profileId, r.version_no AS versionNo, " +
                        "r.create_time AS submitTime, p.user_id AS userId, p.student_no AS studentNo, " +
                        "p.real_name AS realName, p.gender, p.college_id AS collegeId, c.college_name AS collegeName, " +
                        "p.lab_id AS labId, l.lab_name AS labName, p.major, p.class_name AS className, " +
                        "p.phone, p.email, p.direction, p.attachment_url AS attachmentUrl, p.status " +
                        "FROM t_student_profile_review r " +
                        "LEFT JOIN t_student_profile p ON p.id = r.profile_id " +
                        "LEFT JOIN t_college c ON c.id = p.college_id AND c.deleted = 0 " +
                        "LEFT JOIN t_lab l ON l.id = p.lab_id AND l.deleted = 0" +
                        where +
                        " ORDER BY r.create_time ASC, r.id ASC LIMIT ?, ?",
                queryArgs.toArray()
        );

        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total == null ? 0L : total);
        return page;
    }

    @Override
    @Transactional
    public Map<String, Object> approveReview(Long reviewId, String reviewComment, User currentUser) {
        StudentProfileReview review = mustGetReview(reviewId);
        if (!STATUS_PENDING.equals(review.getReviewStatus())) {
            throw new RuntimeException("该审核记录已处理");
        }

        StudentProfile profile = mustGetProfile(review.getProfileId());
        assertCanReviewProfile(profile, currentUser);
        assertReviewStillMatchesProfile(review, profile);

        LocalDateTime now = LocalDateTime.now();
        review.setReviewStatus(STATUS_APPROVED);
        review.setReviewerId(currentUser.getId());
        review.setReviewComment(requireReviewComment(reviewComment));
        review.setReviewTime(now);
        review.setUpdatedBy(currentUser.getId());
        studentProfileReviewMapper.updateById(review);

        StudentProfileArchive archive = new StudentProfileArchive();
        archive.setProfileId(profile.getId());
        archive.setVersionNo(review.getVersionNo());
        archive.setArchiveSnapshot(review.getReviewSnapshot());
        archive.setArchivedBy(currentUser.getId());
        archive.setArchivedAt(now);
        archive.setCreatedBy(currentUser.getId());
        archive.setUpdatedBy(currentUser.getId());
        studentProfileArchiveMapper.insert(archive);

        profile.setStatus(STATUS_ARCHIVED);
        profile.setLastReviewTime(now);
        profile.setUpdatedBy(currentUser.getId());
        studentProfileMapper.updateById(profile);

        auditLogService.record(currentUser.getId(), "student_profile_approve", "student_profile", profile.getId(),
                "reviewId=" + reviewId + ",version=" + review.getVersionNo());
        systemNotificationService.createNotificationAsync(
                profile.getUserId(),
                "资料审核通过",
                "你的成员资料已审核通过，并已完成归档。",
                "PROFILE_REVIEWED",
                profile.getId(),
                "/student/profile"
        );
        publishProfileStatisticsRefresh(profile, currentUser.getId(), "profile_approve");
        return buildProfileDetailMap(profile, currentUser);
    }

    @Override
    @Transactional
    public Map<String, Object> rejectReview(Long reviewId, String reviewComment, User currentUser) {
        StudentProfileReview review = mustGetReview(reviewId);
        if (!STATUS_PENDING.equals(review.getReviewStatus())) {
            throw new RuntimeException("该审核记录已处理");
        }

        StudentProfile profile = mustGetProfile(review.getProfileId());
        assertCanReviewProfile(profile, currentUser);
        assertReviewStillMatchesProfile(review, profile);

        LocalDateTime now = LocalDateTime.now();
        review.setReviewStatus(STATUS_REJECTED);
        review.setReviewerId(currentUser.getId());
        review.setReviewComment(requireReviewComment(reviewComment));
        review.setReviewTime(now);
        review.setUpdatedBy(currentUser.getId());
        studentProfileReviewMapper.updateById(review);

        profile.setStatus(STATUS_REJECTED);
        profile.setLastReviewTime(now);
        profile.setUpdatedBy(currentUser.getId());
        studentProfileMapper.updateById(profile);

        auditLogService.record(currentUser.getId(), "student_profile_reject", "student_profile", profile.getId(),
                "reviewId=" + reviewId + ",version=" + review.getVersionNo());
        systemNotificationService.createNotificationAsync(
                profile.getUserId(),
                "资料需修改后重提",
                "你的成员资料已被驳回，请根据审核意见修改后重新提交。",
                "PROFILE_REVIEWED",
                profile.getId(),
                "/student/profile"
        );
        publishProfileStatisticsRefresh(profile, currentUser.getId(), "profile_reject");
        return buildProfileDetailMap(profile, currentUser);
    }

    private void publishProfileStatisticsRefresh(StudentProfile profile, Long operatorUserId, String reason) {
        if (profile == null) {
            return;
        }
        statisticsRefreshService.refreshAsync(
                "student_profile",
                profile.getCollegeId(),
                profile.getLabId(),
                operatorUserId,
                reason
        );
    }

    private void assertStudentOwner(User currentUser) {
        if (!currentUserAccessor.isStudentIdentity(currentUser)) {
            throw new RuntimeException("只有学生账号可以编辑个人资料");
        }
    }

    private DataScope resolveReadableScope(User currentUser, Long requestedCollegeId, Long requestedLabId) {
        if (currentUserAccessor.isAdmin(currentUser) || currentUserAccessor.isSuperAdmin(currentUser)) {
            return currentUserAccessor.resolveManagementScope(currentUser, requestedCollegeId, requestedLabId);
        }

        if (currentUserAccessor.isTeacherIdentity(currentUser)) {
            DataScope currentScope = currentUserAccessor.getCurrentDataScope();
            if (currentScope.getLabId() == null) {
                throw new RuntimeException("当前教师账号未绑定实验室");
            }
            Long teacherCollegeId = currentUserAccessor.resolveCollegeIdByLabId(currentScope.getLabId());
            if (requestedLabId != null && !Objects.equals(currentScope.getLabId(), requestedLabId)) {
                throw new RuntimeException("无权访问其他实验室的数据");
            }
            if (requestedCollegeId != null && !Objects.equals(teacherCollegeId, requestedCollegeId)) {
                throw new RuntimeException("无权访问其他学院的数据");
            }

            DataScope scope = new DataScope();
            scope.setLevel(DataScopeLevel.LAB);
            scope.setUserId(currentUser.getId());
            scope.setDisplayRole("teacher");
            scope.setCollegeId(teacherCollegeId);
            scope.setLabId(currentScope.getLabId());
            return scope;
        }

        throw new RuntimeException("当前账号无权访问资料管理数据");
    }

    private void assertCanAccessProfile(StudentProfile profile, User currentUser) {
        if (profile == null || profile.getId() == null) {
            throw new RuntimeException("资料不存在");
        }
        if (Objects.equals(profile.getUserId(), currentUser == null ? null : currentUser.getId())) {
            return;
        }
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            return;
        }
        if (currentUserAccessor.isCollegeManager(currentUser)) {
            currentUserAccessor.assertCollegeScope(currentUser, profile.getCollegeId());
            return;
        }
        if (currentUserAccessor.isLabManager(currentUser)) {
            currentUserAccessor.assertLabScope(currentUser, profile.getLabId());
            return;
        }
        if (currentUserAccessor.isTeacherIdentity(currentUser)) {
            Long teacherLabId = currentUserAccessor.getCurrentDataScope().getLabId();
            if (teacherLabId != null && Objects.equals(teacherLabId, profile.getLabId())) {
                return;
            }
        }
        throw new RuntimeException("无权查看该资料");
    }

    private void assertCanReviewProfile(StudentProfile profile, User currentUser) {
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            return;
        }
        if (currentUserAccessor.isCollegeManager(currentUser)) {
            currentUserAccessor.assertCollegeScope(currentUser, profile.getCollegeId());
            return;
        }
        if (currentUserAccessor.isLabManager(currentUser)) {
            currentUserAccessor.assertLabScope(currentUser, profile.getLabId());
            return;
        }
        throw new RuntimeException("当前账号无权审核该资料");
    }

    private void assertReviewStillMatchesProfile(StudentProfileReview review, StudentProfile profile) {
        if (!Objects.equals(profile.getId(), review.getProfileId())) {
            throw new RuntimeException("审核记录与目标资料不匹配");
        }
        if (!Objects.equals(profile.getCurrentVersion(), review.getVersionNo())) {
            throw new RuntimeException("该审核记录已不是最新提交版本");
        }
        if (!STATUS_PENDING.equals(profile.getStatus())) {
            throw new RuntimeException("当前资料不处于待审核状态");
        }
    }

    private void appendScopeCondition(StringBuilder where, List<Object> args, DataScope scope) {
        if (scope == null) {
            return;
        }
        if (scope.getCollegeId() != null) {
            where.append(" AND p.college_id = ?");
            args.add(scope.getCollegeId());
        }
        if (scope.getLabId() != null) {
            where.append(" AND p.lab_id = ?");
            args.add(scope.getLabId());
        }
    }

    private StudentProfile mustGetProfile(Long profileId) {
        StudentProfile profile = studentProfileMapper.selectById(profileId);
        if (profile == null || !Objects.equals(profile.getDeleted(), 0)) {
            throw new RuntimeException("资料不存在");
        }
        return profile;
    }

    private StudentProfileReview mustGetReview(Long reviewId) {
        StudentProfileReview review = studentProfileReviewMapper.selectById(reviewId);
        if (review == null || !Objects.equals(review.getDeleted(), 0)) {
            throw new RuntimeException("审核记录不存在");
        }
        return review;
    }

    private StudentProfile findActiveProfileByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        QueryWrapper<StudentProfile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("deleted", 0)
                .last("LIMIT 1");
        return studentProfileMapper.selectOne(queryWrapper);
    }

    private void validateProfileBeforeSubmit(StudentProfile profile) {
        if (!StringUtils.hasText(profile.getStudentNo())) {
            throw new RuntimeException("学号不能为空");
        }
        if (!StringUtils.hasText(profile.getRealName())) {
            throw new RuntimeException("姓名不能为空");
        }
        if (profile.getCollegeId() == null) {
            throw new RuntimeException("学院不能为空");
        }
        if (!StringUtils.hasText(profile.getMajor())) {
            throw new RuntimeException("专业不能为空");
        }
        if (!StringUtils.hasText(profile.getClassName())) {
            throw new RuntimeException("班级不能为空");
        }
        if (!StringUtils.hasText(profile.getPhone())) {
            throw new RuntimeException("手机号不能为空");
        }
        if (!StringUtils.hasText(profile.getEmail())) {
            throw new RuntimeException("邮箱不能为空");
        }
        if (!StringUtils.hasText(profile.getAttachmentUrl())) {
            throw new RuntimeException("请先上传成员资料附件，再提交审核");
        }
    }

    private Map<String, Object> buildTransientProfile(User currentUser) {
        DataScope currentScope = currentUserAccessor.getCurrentDataScope();
        Long resolvedCollegeId = resolveProfileCollegeId(null, currentScope.getLabId(), currentUser, false);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", null);
        profile.put("userId", currentUser.getId());
        profile.put("labId", currentScope.getLabId());
        profile.put("labName", loadLabName(currentScope.getLabId()));
        profile.put("studentNo", currentUser.getStudentId());
        profile.put("realName", currentUser.getRealName());
        profile.put("gender", null);
        profile.put("collegeId", resolvedCollegeId);
        profile.put("collegeName", loadCollegeName(resolvedCollegeId));
        profile.put("major", currentUser.getMajor());
        profile.put("className", null);
        profile.put("phone", currentUser.getPhone());
        profile.put("email", currentUser.getEmail());
        profile.put("direction", null);
        profile.put("introduction", null);
        profile.put("attachmentUrl", currentUser.getResume());
        profile.put("resumeUrl", currentUser.getResume());
        profile.put("status", STATUS_DRAFT);
        profile.put("currentVersion", 0);
        profile.put("submittedAt", null);
        profile.put("lastReviewTime", null);
        profile.put("reviewCount", 0L);
        profile.put("archiveCount", 0L);
        profile.put("latestReview", null);
        profile.put("reviewRequired", requiresProfileReview(currentUser));
        profile.put("editable", true);
        return profile;
    }

    private Map<String, Object> buildProfileDetailMap(StudentProfile profile, User currentUser) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", profile.getId());
        result.put("userId", profile.getUserId());
        result.put("labId", profile.getLabId());
        result.put("labName", loadLabName(profile.getLabId()));
        result.put("studentNo", profile.getStudentNo());
        result.put("realName", profile.getRealName());
        result.put("gender", profile.getGender());
        result.put("collegeId", profile.getCollegeId());
        result.put("collegeName", loadCollegeName(profile.getCollegeId()));
        result.put("major", profile.getMajor());
        result.put("className", profile.getClassName());
        result.put("phone", profile.getPhone());
        result.put("email", profile.getEmail());
        result.put("direction", profile.getDirection());
        result.put("introduction", profile.getIntroduction());
        String resumeUrl = loadUserResume(profile.getUserId());
        result.put("attachmentUrl", defaultValue(profile.getAttachmentUrl(), resumeUrl));
        result.put("resumeUrl", resumeUrl);
        result.put("status", profile.getStatus());
        result.put("currentVersion", profile.getCurrentVersion());
        result.put("submittedAt", profile.getSubmittedAt());
        result.put("lastReviewTime", profile.getLastReviewTime());
        result.put("createTime", profile.getCreateTime());
        result.put("updateTime", profile.getUpdateTime());
        result.put("reviewCount", countReviews(profile.getId()));
        result.put("archiveCount", countArchives(profile.getId()));
        result.put("latestReview", loadLatestReview(profile.getId()));
        result.put("reviewRequired", profile.getLabId() != null);
        result.put("editable", !STATUS_PENDING.equals(profile.getStatus())
                && Objects.equals(profile.getUserId(), currentUser == null ? null : currentUser.getId()));
        return result;
    }

    private Map<String, Object> buildReviewMap(StudentProfileReview review) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", review.getId());
        result.put("profileId", review.getProfileId());
        result.put("versionNo", review.getVersionNo());
        result.put("reviewerId", review.getReviewerId());
        result.put("reviewStatus", review.getReviewStatus());
        result.put("reviewComment", review.getReviewComment());
        result.put("reviewTime", review.getReviewTime());
        result.put("createTime", review.getCreateTime());
        result.put("updateTime", review.getUpdateTime());
        result.put("snapshot", parseSnapshot(review.getReviewSnapshot()));
        return result;
    }

    private Map<String, Object> buildArchiveMap(StudentProfileArchive archive) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", archive.getId());
        result.put("profileId", archive.getProfileId());
        result.put("versionNo", archive.getVersionNo());
        result.put("archivedBy", archive.getArchivedBy());
        result.put("archivedAt", archive.getArchivedAt());
        result.put("createTime", archive.getCreateTime());
        result.put("snapshot", parseSnapshot(archive.getArchiveSnapshot()));
        return result;
    }

    private Map<String, Object> loadLatestReview(Long profileId) {
        if (profileId == null) {
            return null;
        }
        QueryWrapper<StudentProfileReview> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("profile_id", profileId)
                .eq("deleted", 0)
                .orderByDesc("version_no")
                .orderByDesc("id")
                .last("LIMIT 1");
        StudentProfileReview review = studentProfileReviewMapper.selectOne(queryWrapper);
        return review == null ? null : buildReviewMap(review);
    }

    private long countReviews(Long profileId) {
        if (profileId == null) {
            return 0L;
        }
        QueryWrapper<StudentProfileReview> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("profile_id", profileId).eq("deleted", 0);
        Long count = studentProfileReviewMapper.selectCount(queryWrapper);
        return count == null ? 0L : count;
    }

    private long countArchives(Long profileId) {
        if (profileId == null) {
            return 0L;
        }
        QueryWrapper<StudentProfileArchive> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("profile_id", profileId).eq("deleted", 0);
        Long count = studentProfileArchiveMapper.selectCount(queryWrapper);
        return count == null ? 0L : count;
    }

    private String writeSnapshot(StudentProfile profile) {
        try {
            return objectMapper.writeValueAsString(buildSnapshotModel(profile));
        } catch (Exception e) {
            throw new RuntimeException("生成资料快照失败");
        }
    }

    private Map<String, Object> parseSnapshot(String snapshot) {
        if (!StringUtils.hasText(snapshot)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(snapshot, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> buildSnapshotModel(StudentProfile profile) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("profileId", profile.getId());
        snapshot.put("userId", profile.getUserId());
        snapshot.put("labId", profile.getLabId());
        snapshot.put("labName", loadLabName(profile.getLabId()));
        snapshot.put("studentNo", profile.getStudentNo());
        snapshot.put("realName", profile.getRealName());
        snapshot.put("gender", profile.getGender());
        snapshot.put("collegeId", profile.getCollegeId());
        snapshot.put("collegeName", loadCollegeName(profile.getCollegeId()));
        snapshot.put("major", profile.getMajor());
        snapshot.put("className", profile.getClassName());
        snapshot.put("phone", profile.getPhone());
        snapshot.put("email", profile.getEmail());
        snapshot.put("direction", profile.getDirection());
        snapshot.put("introduction", profile.getIntroduction());
        snapshot.put("attachmentUrl", profile.getAttachmentUrl());
        snapshot.put("currentVersion", profile.getCurrentVersion());
        return snapshot;
    }

    private String loadUserResume(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getResume();
    }

    private void syncUserBasicFields(StudentProfile profile, Long userId) {
        if (profile == null || userId == null) {
            return;
        }
        User update = new User();
        update.setId(userId);
        if (StringUtils.hasText(profile.getRealName())) {
            update.setRealName(profile.getRealName());
        }
        if (StringUtils.hasText(profile.getMajor())) {
            update.setMajor(profile.getMajor());
        }
        if (StringUtils.hasText(profile.getPhone())) {
            update.setPhone(profile.getPhone());
        }
        if (StringUtils.hasText(profile.getEmail())) {
            update.setEmail(profile.getEmail());
        }
        if (StringUtils.hasText(profile.getStudentNo())) {
            update.setStudentId(profile.getStudentNo());
        }
        if (profile.getCollegeId() != null) {
            update.setCollege(loadCollegeName(profile.getCollegeId()));
        }
        if (StringUtils.hasText(profile.getAttachmentUrl()) && !StringUtils.hasText(loadUserResume(userId))) {
            update.setResume(profile.getAttachmentUrl());
        }
        userMapper.updateById(update);
    }

    private Long resolveProfileCollegeId(Long requestedCollegeId, Long labId, User currentUser, boolean required) {
        Long labCollegeId = currentUserAccessor.resolveCollegeIdByLabId(labId);
        if (labCollegeId != null) {
            if (requestedCollegeId != null && !Objects.equals(labCollegeId, requestedCollegeId)) {
                throw new RuntimeException("所选学院与当前实验室不匹配");
            }
            return labCollegeId;
        }
        if (requestedCollegeId != null) {
            return requestedCollegeId;
        }
        Long userCollegeId = resolveCollegeIdByUserCollege(currentUser);
        if (userCollegeId != null) {
            return userCollegeId;
        }
        if (required) {
            throw new RuntimeException("学院不能为空");
        }
        return null;
    }

    private boolean requiresProfileReview(User currentUser) {
        return currentUserAccessor.buildDataScope(currentUser).getLabId() != null;
    }

    private Long resolveCollegeIdByUserCollege(User currentUser) {
        String collegeName = trimToNull(currentUser == null ? null : currentUser.getCollege());
        if (collegeName == null) {
            return null;
        }
        QueryWrapper<com.lab.recruitment.entity.College> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("college_name", collegeName)
                .eq("deleted", 0)
                .last("LIMIT 1");
        com.lab.recruitment.entity.College college = collegeMapper.selectOne(queryWrapper);
        return college == null ? null : college.getId();
    }

    private String requireReviewComment(String reviewComment) {
        String normalized = trimToNull(reviewComment);
        if (normalized == null) {
            throw new RuntimeException("审核意见不能为空");
        }
        return normalized;
    }

    private String normalizeStatus(String status, boolean required) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            if (required) {
                throw new RuntimeException("状态不能为空");
            }
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!STATUS_DRAFT.equals(normalized)
                && !STATUS_PENDING.equals(normalized)
                && !STATUS_APPROVED.equals(normalized)
                && !STATUS_REJECTED.equals(normalized)
                && !STATUS_ARCHIVED.equals(normalized)) {
            throw new RuntimeException("资料状态不合法");
        }
        return normalized;
    }

    private String loadLabName(Long labId) {
        if (labId == null) {
            return null;
        }
        com.lab.recruitment.entity.Lab lab = labMapper.selectById(labId);
        return lab == null ? null : lab.getLabName();
    }

    private String loadCollegeName(Long collegeId) {
        if (collegeId == null) {
            return null;
        }
        com.lab.recruitment.entity.College college = collegeMapper.selectById(collegeId);
        return college == null ? null : college.getCollegeName();
    }

    private String defaultValue(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private String normalizeEmail(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
