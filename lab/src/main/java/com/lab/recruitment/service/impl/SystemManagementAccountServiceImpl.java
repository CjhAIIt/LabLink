package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.recruitment.entity.College;
import com.lab.recruitment.entity.PlatformPost;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.CollegeMapper;
import com.lab.recruitment.mapper.PlatformPostMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.SystemManagementAccountService;
import com.lab.recruitment.service.UserAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SystemManagementAccountServiceImpl implements SystemManagementAccountService {

    public static final String SCHOOL_DIRECTOR_ACCOUNT_CODE = "SYS_SCHOOL_DIRECTOR";
    public static final String COLLEGE_MANAGER_ACCOUNT_PREFIX = "SYS_COLLEGE_MANAGER#";

    private static final String SCHOOL_DIRECTOR_USERNAME = "superadmin";
    private static final String DEFAULT_PASSWORD = "Lab123456";
    private static final String DEFAULT_EMAIL_DOMAIN = "@aiit.edu.cn";
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_SUPER_ADMIN = "super_admin";
    private static final String POST_COLLEGE_MANAGER = "COLLEGE_MANAGER";
    private static final String STATUS_ACTIVE = "active";
    private static final int USER_STATUS_ACTIVE = 1;
    private static final int USER_CAN_EDIT_NO = 0;

    private static final Map<String, String> COLLEGE_USERNAME_OVERRIDES = buildCollegeUsernameOverrides();

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CollegeMapper collegeMapper;

    @Autowired
    private PlatformPostMapper platformPostMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserAccessService userAccessService;

    @Override
    @Transactional
    public void ensureBuiltInManagementUsers() {
        ensureSchoolDirectorUser();

        List<College> colleges = collegeMapper.selectList(new QueryWrapper<College>()
                .eq("deleted", 0)
                .orderByAsc("id"));
        for (College college : colleges) {
            ensureCollegeManagerUser(college, false);
        }
    }

    @Override
    @Transactional
    public void ensureCollegeManagerAccount(College college) {
        ensureCollegeManagerUser(college, true);
    }

    @Override
    public boolean isFixedManagementAccount(User user) {
        if (user == null || !StringUtils.hasText(user.getSystemAccountCode())) {
            return false;
        }
        String accountCode = user.getSystemAccountCode().trim().toUpperCase(Locale.ROOT);
        return SCHOOL_DIRECTOR_ACCOUNT_CODE.equals(accountCode)
                || accountCode.startsWith(COLLEGE_MANAGER_ACCOUNT_PREFIX);
    }

    private void ensureSchoolDirectorUser() {
        User existing = findBySystemAccountCode(SCHOOL_DIRECTOR_ACCOUNT_CODE);
        if (existing == null) {
            existing = findByUsername(SCHOOL_DIRECTOR_USERNAME);
        }

        assertUsernameAvailable(SCHOOL_DIRECTOR_USERNAME, existing == null ? null : existing.getId());
        if (existing == null) {
            User created = new User();
            created.setUsername(SCHOOL_DIRECTOR_USERNAME);
            created.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            created.setRealName("学校总负责人");
            created.setRole(ROLE_SUPER_ADMIN);
            created.setEmail(resolveDefaultEmail(SCHOOL_DIRECTOR_USERNAME));
            created.setCanEdit(USER_CAN_EDIT_NO);
            created.setStatus(USER_STATUS_ACTIVE);
            created.setSystemAccountCode(SCHOOL_DIRECTOR_ACCOUNT_CODE);
            userMapper.insert(created);
            return;
        }

        User update = new User();
        update.setId(existing.getId());
        update.setUsername(SCHOOL_DIRECTOR_USERNAME);
        update.setRole(ROLE_SUPER_ADMIN);
        update.setCanEdit(USER_CAN_EDIT_NO);
        update.setStatus(USER_STATUS_ACTIVE);
        update.setSystemAccountCode(SCHOOL_DIRECTOR_ACCOUNT_CODE);
        update.setRealName("学校总负责人");
        update.setStudentId(null);
        update.setCollege(null);
        update.setMajor(null);
        update.setGrade(null);
        update.setLabId(null);
        if (!StringUtils.hasText(existing.getEmail())) {
            update.setEmail(resolveDefaultEmail(SCHOOL_DIRECTOR_USERNAME));
        }
        userMapper.updateById(update);
    }

    private void ensureCollegeManagerUser(College college, boolean syncPost) {
        if (college == null || college.getId() == null || !StringUtils.hasText(college.getCollegeCode())) {
            return;
        }

        String expectedUsername = resolveCollegeManagerUsername(college.getCollegeCode());
        String accountCode = buildCollegeManagerAccountCode(college.getId());
        User existing = findBySystemAccountCode(accountCode);
        if (existing == null) {
            existing = findByUsername(expectedUsername);
        }

        assertUsernameAvailable(expectedUsername, existing == null ? null : existing.getId());
        if (existing == null) {
            User created = new User();
            created.setUsername(expectedUsername);
            created.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            created.setRealName(resolveCollegeManagerRealName(college));
            created.setRole(ROLE_ADMIN);
            created.setCollege(college.getCollegeName());
            created.setEmail(resolveDefaultEmail(expectedUsername));
            created.setCanEdit(USER_CAN_EDIT_NO);
            created.setStatus(USER_STATUS_ACTIVE);
            created.setSystemAccountCode(accountCode);
            userMapper.insert(created);
            existing = created;
        } else {
            User update = new User();
            update.setId(existing.getId());
            update.setUsername(expectedUsername);
            update.setRole(ROLE_ADMIN);
            update.setCanEdit(USER_CAN_EDIT_NO);
            update.setStatus(USER_STATUS_ACTIVE);
            update.setSystemAccountCode(accountCode);
            update.setRealName(resolveCollegeManagerRealName(college));
            update.setStudentId(null);
            update.setCollege(college.getCollegeName());
            update.setMajor(null);
            update.setGrade(null);
            update.setLabId(null);
            if (!StringUtils.hasText(existing.getEmail())) {
                update.setEmail(resolveDefaultEmail(expectedUsername));
            }
            userMapper.updateById(update);
            existing.setUsername(expectedUsername);
        }

        syncCollegeAdminMirror(college.getId(), existing.getId());
        if (syncPost) {
            ensureCollegeManagerPost(existing.getId(), college.getId());
            userAccessService.refreshCompatibilityAccess(existing.getId());
        }
    }

    private void syncCollegeAdminMirror(Long collegeId, Long userId) {
        if (collegeId == null || userId == null) {
            return;
        }
        College currentCollege = collegeMapper.selectById(collegeId);
        if (currentCollege != null && userId.equals(currentCollege.getAdminUserId())) {
            return;
        }

        College update = new College();
        update.setId(collegeId);
        update.setAdminUserId(userId);
        collegeMapper.updateById(update);
    }

    private void ensureCollegeManagerPost(Long userId, Long collegeId) {
        if (userId == null || collegeId == null) {
            return;
        }

        List<PlatformPost> activePosts = platformPostMapper.selectList(new QueryWrapper<PlatformPost>()
                .eq("deleted", 0)
                .eq("status", STATUS_ACTIVE)
                .eq("post_code", POST_COLLEGE_MANAGER)
                .eq("college_id", collegeId)
                .orderByAsc("id"));
        boolean matched = false;
        for (PlatformPost post : activePosts) {
            if (userId.equals(post.getUserId())) {
                matched = true;
                continue;
            }
            post.setStatus("inactive");
            post.setEndTime(LocalDateTime.now());
            post.setRemark("replaced by fixed college manager account");
            platformPostMapper.updateById(post);
        }
        if (matched) {
            return;
        }

        PlatformPost post = new PlatformPost();
        post.setUserId(userId);
        post.setPostCode(POST_COLLEGE_MANAGER);
        post.setCollegeId(collegeId);
        post.setStatus(STATUS_ACTIVE);
        post.setStartTime(LocalDateTime.now());
        post.setRemark("fixed college manager account");
        platformPostMapper.insert(post);
    }

    private User findBySystemAccountCode(String accountCode) {
        if (!StringUtils.hasText(accountCode)) {
            return null;
        }
        return userMapper.selectOne(new QueryWrapper<User>()
                .eq("deleted", 0)
                .eq("system_account_code", accountCode)
                .last("LIMIT 1"));
    }

    private User findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userMapper.selectOne(new QueryWrapper<User>()
                .eq("deleted", 0)
                .eq("username", username)
                .last("LIMIT 1"));
    }

    private void assertUsernameAvailable(String expectedUsername, Long currentUserId) {
        User usernameOwner = findByUsername(expectedUsername);
        if (usernameOwner == null || (currentUserId != null && currentUserId.equals(usernameOwner.getId()))) {
            return;
        }
        throw new RuntimeException("固定管理账号用户名冲突: " + expectedUsername);
    }

    private String resolveCollegeManagerUsername(String collegeCode) {
        String normalizedCode = normalizeCollegeCode(collegeCode);
        String overridden = COLLEGE_USERNAME_OVERRIDES.get(normalizedCode);
        if (StringUtils.hasText(overridden)) {
            return overridden;
        }
        return normalizedCode.toLowerCase(Locale.ROOT) + "_admin";
    }

    private String resolveCollegeManagerRealName(College college) {
        if (college == null || !StringUtils.hasText(college.getCollegeName())) {
            return "学院负责人";
        }
        return college.getCollegeName().trim() + "负责人";
    }

    private String resolveDefaultEmail(String username) {
        return username + DEFAULT_EMAIL_DOMAIN;
    }

    private String buildCollegeManagerAccountCode(Long collegeId) {
        return COLLEGE_MANAGER_ACCOUNT_PREFIX + collegeId;
    }

    private String normalizeCollegeCode(String collegeCode) {
        if (!StringUtils.hasText(collegeCode)) {
            return "";
        }
        return collegeCode.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, String> buildCollegeUsernameOverrides() {
        Map<String, String> overrides = new LinkedHashMap<>();
        overrides.put("EEE", "ee_admin");
        return overrides;
    }
}
