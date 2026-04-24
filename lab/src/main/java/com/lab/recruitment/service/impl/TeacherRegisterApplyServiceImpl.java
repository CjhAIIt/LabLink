package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.TeacherRegisterApplyAuditDTO;
import com.lab.recruitment.dto.TeacherRegisterDTO;
import com.lab.recruitment.dto.TeacherRegisterEmailCodeDTO;
import com.lab.recruitment.entity.College;
import com.lab.recruitment.entity.TeacherRegisterApply;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.entity.UserIdentity;
import com.lab.recruitment.mapper.CollegeMapper;
import com.lab.recruitment.mapper.TeacherRegisterApplyMapper;
import com.lab.recruitment.mapper.UserIdentityMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.EmailAuthCodeService;
import com.lab.recruitment.service.TeacherRegisterApplyService;
import com.lab.recruitment.support.CurrentUserAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class TeacherRegisterApplyServiceImpl implements TeacherRegisterApplyService {

    private static final String STATUS_SUBMITTED = "submitted";
    private static final String STATUS_COLLEGE_APPROVED = "college_approved";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";
    private static final String TEACHER_IDENTITY = "teacher";
    private static final String USER_ROLE_STUDENT = "student";
    private static final Pattern TEACHER_NO_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9][0-9]{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern CHINESE_REAL_NAME_PATTERN = Pattern.compile("^[\\p{IsHan}·]{2,50}$");

    @Autowired
    private TeacherRegisterApplyMapper teacherRegisterApplyMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserIdentityMapper userIdentityMapper;

    @Autowired
    private CollegeMapper collegeMapper;

    @Autowired
    private EmailAuthCodeService emailAuthCodeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Override
    public void sendRegisterEmailCode(TeacherRegisterEmailCodeDTO registerEmailCodeDTO) {
        String teacherNo = normalizeTeacherNo(registerEmailCodeDTO.getTeacherNo());
        String email = normalizeEmail(registerEmailCodeDTO.getEmail());

        validateTeacherNo(teacherNo);
        validateEmail(email);
        ensureTeacherRegisterAvailable(teacherNo, email);
        emailAuthCodeService.sendRegisterCode(teacherNo, email);
    }

    @Override
    @Transactional
    public boolean submitRegisterApply(TeacherRegisterDTO registerDTO) {
        String teacherNo = normalizeTeacherNo(registerDTO.getTeacherNo());
        String email = normalizeEmail(registerDTO.getEmail());
        String emailCode = trimToNull(registerDTO.getEmailCode());

        validateTeacherNo(teacherNo);
        validateRealName(trimToNull(registerDTO.getRealName()));
        validateEmail(email);
        validatePhone(trimToNull(registerDTO.getPhone()));
        ensureCollegeExists(registerDTO.getCollegeId());
        ensurePassword(registerDTO.getPassword());
        ensureTeacherRegisterAvailable(teacherNo, email);
        emailAuthCodeService.verifyRegisterCode(teacherNo, email, emailCode);

        TeacherRegisterApply apply = new TeacherRegisterApply();
        apply.setTeacherNo(teacherNo);
        apply.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
        apply.setRealName(trimToNull(registerDTO.getRealName()));
        apply.setCollegeId(registerDTO.getCollegeId());
        apply.setTitle(trimToNull(registerDTO.getTitle()));
        apply.setPhone(trimToNull(registerDTO.getPhone()));
        apply.setEmail(email);
        apply.setApplyReason(trimToNull(registerDTO.getApplyReason()));
        apply.setStatus(STATUS_SUBMITTED);
        return teacherRegisterApplyMapper.insert(apply) > 0;
    }

    @Override
    public Page<Map<String, Object>> getApplyPage(Integer pageNum, Integer pageSize, String status, String keyword, User currentUser) {
        Long collegeId = null;
        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            collegeId = resolveManagedCollegeId(currentUser);
            if (collegeId == null) {
                throw new RuntimeException("仅学校管理员或学院管理员可查看教师注册申请");
            }
        }
        return teacherRegisterApplyMapper.selectApplyPage(new Page<>(pageNum, pageSize), collegeId, trimToNull(status), trimToNull(keyword));
    }

    @Override
    @Transactional
    public boolean auditApply(Long id, TeacherRegisterApplyAuditDTO auditDTO, User currentUser) {
        TeacherRegisterApply apply = teacherRegisterApplyMapper.selectById(id);
        if (apply == null || (apply.getDeleted() != null && apply.getDeleted() == 1)) {
            throw new RuntimeException("教师注册申请不存在");
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
            return teacherRegisterApplyMapper.updateById(apply) > 0;
        }

        if ("schoolApprove".equalsIgnoreCase(action)) {
            currentUserAccessor.assertSuperAdmin(currentUser);
            if (!STATUS_COLLEGE_APPROVED.equalsIgnoreCase(apply.getStatus())) {
                throw new RuntimeException("仅学院初审通过的申请可执行学校通过");
            }
            if (apply.getGeneratedUserId() != null && userMapper.selectById(apply.getGeneratedUserId()) != null) {
                throw new RuntimeException("该教师账号已生成");
            }

            User user = buildApprovedTeacherUser(apply);
            userMapper.insert(user);

            UserIdentity userIdentity = new UserIdentity();
            userIdentity.setUserId(user.getId());
            userIdentity.setIdentityType(TEACHER_IDENTITY);
            userIdentity.setCollegeId(apply.getCollegeId());
            userIdentity.setStatus("active");
            userIdentity.setRemark(trimToNull(apply.getTitle()));
            userIdentityMapper.insert(userIdentity);

            apply.setStatus(STATUS_APPROVED);
            apply.setSchoolAuditBy(currentUser.getId());
            apply.setSchoolAuditTime(LocalDateTime.now());
            apply.setSchoolAuditComment(trimToNull(auditDTO.getAuditComment()));
            apply.setGeneratedUserId(user.getId());
            return teacherRegisterApplyMapper.updateById(apply) > 0;
        }

        if ("reject".equalsIgnoreCase(action)) {
            return rejectApply(apply, currentUser, trimToNull(auditDTO.getAuditComment()));
        }

        throw new RuntimeException("不支持的审核动作");
    }

    @Override
    public String resolveLoginBlockMessage(String username) {
        String teacherNo = normalizeTeacherNo(username);
        if (!StringUtils.hasText(teacherNo)) {
            return null;
        }

        QueryWrapper<TeacherRegisterApply> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teacher_no", teacherNo)
                .eq("deleted", 0)
                .orderByDesc("id")
                .last("LIMIT 1");
        TeacherRegisterApply latestApply = teacherRegisterApplyMapper.selectOne(queryWrapper);
        if (latestApply == null) {
            return null;
        }

        if (STATUS_SUBMITTED.equalsIgnoreCase(latestApply.getStatus())) {
            return "教师注册申请待学院审核";
        }
        if (STATUS_COLLEGE_APPROVED.equalsIgnoreCase(latestApply.getStatus())) {
            return "教师注册申请待学校审核";
        }
        if (STATUS_REJECTED.equalsIgnoreCase(latestApply.getStatus())) {
            String comment = StringUtils.hasText(latestApply.getSchoolAuditComment())
                    ? latestApply.getSchoolAuditComment()
                    : latestApply.getCollegeAuditComment();
            return StringUtils.hasText(comment) ? "教师注册申请已驳回：" + comment : "教师注册申请已驳回，请重新提交";
        }
        if (STATUS_APPROVED.equalsIgnoreCase(latestApply.getStatus()) && latestApply.getGeneratedUserId() == null) {
            return "教师账号正在生成，请稍后重试";
        }
        return null;
    }

    private User buildApprovedTeacherUser(TeacherRegisterApply apply) {
        ensureTeacherRegisterAvailableForApproval(apply);
        College college = collegeMapper.selectById(apply.getCollegeId());
        if (college == null || (college.getDeleted() != null && college.getDeleted() == 1)) {
            throw new RuntimeException("所属学院不存在");
        }

        User user = new User();
        user.setUsername(apply.getTeacherNo());
        user.setPassword(apply.getPasswordHash());
        user.setRealName(apply.getRealName());
        user.setRole(USER_ROLE_STUDENT);
        user.setStudentId(null);
        user.setCollege(college.getCollegeName());
        user.setMajor(null);
        user.setGrade(null);
        user.setPhone(apply.getPhone());
        user.setEmail(apply.getEmail());
        user.setCanEdit(1);
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }

    private void ensureTeacherRegisterAvailable(String teacherNo, String email) {
        if (userMapper.countByUsernameIncludeDeleted(teacherNo, 0) > 0) {
            throw new RuntimeException("该工号已注册");
        }
        if (userMapper.countByEmailIncludeDeleted(email, 0) > 0) {
            throw new RuntimeException("该邮箱已被注册");
        }

        QueryWrapper<TeacherRegisterApply> applyQuery = new QueryWrapper<>();
        applyQuery.eq("deleted", 0)
                .and(wrapper -> wrapper.eq("teacher_no", teacherNo).or().eq("email", email))
                .in("status", STATUS_SUBMITTED, STATUS_COLLEGE_APPROVED, STATUS_APPROVED);
        if (teacherRegisterApplyMapper.selectCount(applyQuery) > 0) {
            throw new RuntimeException("该教师注册申请已存在，请勿重复提交");
        }
    }

    private void ensureTeacherRegisterAvailableForApproval(TeacherRegisterApply apply) {
        if (userMapper.countByUsernameIncludeDeleted(apply.getTeacherNo(), 0) > 0) {
            throw new RuntimeException("该工号已生成正式账号");
        }
        if (userMapper.countByEmailIncludeDeleted(apply.getEmail(), 0) > 0) {
            throw new RuntimeException("该邮箱已生成正式账号");
        }
    }

    private boolean rejectApply(TeacherRegisterApply apply, User currentUser, String auditComment) {
        if (STATUS_APPROVED.equalsIgnoreCase(apply.getStatus())) {
            throw new RuntimeException("已通过的申请不可驳回");
        }

        if (STATUS_COLLEGE_APPROVED.equalsIgnoreCase(apply.getStatus())) {
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
        return teacherRegisterApplyMapper.updateById(apply) > 0;
    }

    private void assertCollegeAuditor(User currentUser, Long collegeId) {
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            return;
        }

        Long managedCollegeId = resolveManagedCollegeId(currentUser);
        if (managedCollegeId == null || !managedCollegeId.equals(collegeId)) {
            throw new RuntimeException("你无权审核该学院的教师注册申请");
        }
    }

    private Long resolveManagedCollegeId(User currentUser) {
        if (currentUser == null || currentUser.getId() == null || currentUserAccessor.isSuperAdmin(currentUser)) {
            return null;
        }
        QueryWrapper<College> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0)
                .eq("admin_user_id", currentUser.getId())
                .last("LIMIT 1");
        College managedCollege = collegeMapper.selectOne(queryWrapper);
        return managedCollege == null ? null : managedCollege.getId();
    }

    private void ensureCollegeExists(Long collegeId) {
        if (collegeId == null) {
            throw new RuntimeException("所属学院不能为空");
        }
        College college = collegeMapper.selectById(collegeId);
        if (college == null || (college.getDeleted() != null && college.getDeleted() == 1)) {
            throw new RuntimeException("所属学院不存在");
        }
    }

    private void validateTeacherNo(String teacherNo) {
        if (!StringUtils.hasText(teacherNo) || !TEACHER_NO_PATTERN.matcher(teacherNo).matches()) {
            throw new RuntimeException("工号格式不正确");
        }
    }

    private void validateRealName(String realName) {
        if (!StringUtils.hasText(realName) || !CHINESE_REAL_NAME_PATTERN.matcher(realName).matches()) {
            throw new RuntimeException("真实姓名必须为中文");
        }
    }

    private void validateEmail(String email) {
        if (!StringUtils.hasText(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("邮箱格式不正确");
        }
    }

    private void validatePhone(String phone) {
        if (StringUtils.hasText(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new RuntimeException("手机号格式不正确");
        }
    }

    private void ensurePassword(String password) {
        String normalized = trimToNull(password);
        if (!StringUtils.hasText(normalized) || normalized.length() < 6 || normalized.length() > 20) {
            throw new RuntimeException("密码长度需在 6 到 20 位之间");
        }
    }

    private String normalizeTeacherNo(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }

    private String normalizeEmail(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
