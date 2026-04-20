package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.recruitment.entity.LabMember;
import com.lab.recruitment.entity.LabTeacherRelation;
import com.lab.recruitment.entity.PlatformPost;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.entity.UserIdentity;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.mapper.LabTeacherRelationMapper;
import com.lab.recruitment.mapper.PlatformPostMapper;
import com.lab.recruitment.mapper.UserIdentityMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.PlatformCacheService;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.support.UserAccessProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserAccessServiceImpl implements UserAccessService {

    public static final String IDENTITY_STUDENT = "student";
    public static final String IDENTITY_TEACHER = "teacher";
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_TEACHER = "teacher";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_SUPER_ADMIN = "super_admin";
    public static final String POST_SCHOOL_DIRECTOR = "SCHOOL_DIRECTOR";
    public static final String POST_COLLEGE_MANAGER = "COLLEGE_MANAGER";
    public static final String STATUS_ACTIVE = "active";
    public static final String MEMBER_ROLE_LAB_ADMIN = "lab_admin";
    public static final String MEMBER_ROLE_LAB_LEADER = "lab_leader";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserIdentityMapper userIdentityMapper;

    @Autowired
    private PlatformPostMapper platformPostMapper;

    @Autowired
    private LabMemberMapper labMemberMapper;

    @Autowired
    private LabTeacherRelationMapper labTeacherRelationMapper;

    @Autowired
    private PlatformCacheService platformCacheService;

    @Override
    public UserAccessProfile buildProfile(User user) {
        UserAccessProfile profile = new UserAccessProfile();
        if (user == null || user.getId() == null) {
            profile.setDisplayRole(ROLE_STUDENT);
            profile.setPrimaryIdentity(IDENTITY_STUDENT);
            profile.getAuthorities().add("ROLE_STUDENT");
            return profile;
        }

        profile.setUserId(user.getId());
        profile.setPrimaryIdentity(resolvePrimaryIdentity(user));

        List<PlatformPost> activePosts = listActivePlatformPosts(user.getId());
        for (PlatformPost post : activePosts) {
            if (post == null || !StringUtils.hasText(post.getPostCode())) {
                continue;
            }
            String postCode = post.getPostCode().trim().toUpperCase(Locale.ROOT);
            profile.getPlatformPostCodes().add(postCode);
            if (POST_SCHOOL_DIRECTOR.equals(postCode)) {
                profile.setSchoolDirector(true);
            }
            if (POST_COLLEGE_MANAGER.equals(postCode)) {
                profile.setCollegeManager(true);
                if (profile.getManagedCollegeId() == null) {
                    profile.setManagedCollegeId(post.getCollegeId());
                }
            }
        }

        LabMember managerMembership = findActiveLabManagerMembership(user.getId(), null);
        if (managerMembership != null) {
            profile.setLabManager(true);
            profile.setManagedLabId(managerMembership.getLabId());
            profile.setLabMemberRole(normalizeCode(managerMembership.getMemberRole()));
        } else {
            LabTeacherRelation teacherRelation = findActiveTeacherRelation(user.getId(), null);
            if (teacherRelation != null) {
                profile.setManagedLabId(teacherRelation.getLabId());
                profile.setLabMemberRole("advisor_teacher");
            }
        }

        String displayRole = resolveDisplayRole(profile, user);
        profile.setDisplayRole(displayRole);
        profile.setAuthorities(new ArrayList<>(buildAuthorityCodes(profile)));
        return profile;
    }

    @Override
    public List<SimpleGrantedAuthority> buildAuthorities(User user) {
        UserAccessProfile profile = buildProfile(user);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String code : profile.getAuthorities()) {
            authorities.add(new SimpleGrantedAuthority(code));
        }
        return authorities;
    }

    @Override
    public Long resolveManagedCollegeId(User user) {
        return buildProfile(user).getManagedCollegeId();
    }

    @Override
    public Long resolveManagedLabId(User user) {
        UserAccessProfile profile = buildProfile(user);
        if (profile.getManagedLabId() != null) {
            return profile.getManagedLabId();
        }
        if (user != null && user.getLabId() != null) {
            return user.getLabId();
        }
        return findActiveMemberLabId(user == null ? null : user.getId());
    }

    @Override
    public boolean isStudentIdentity(User user) {
        return IDENTITY_STUDENT.equalsIgnoreCase(buildProfile(user).getPrimaryIdentity());
    }

    @Override
    public boolean isTeacherIdentity(User user) {
        return IDENTITY_TEACHER.equalsIgnoreCase(buildProfile(user).getPrimaryIdentity());
    }

    @Override
    public boolean isLabManager(User user, Long labId) {
        if (user == null || user.getId() == null) {
            return false;
        }
        LabMember managerMembership = findActiveLabManagerMembership(user.getId(), labId);
        return managerMembership != null;
    }

    @Override
    public void ensureStudentLabManager(User user, Long labId) {
        if (user == null || user.getId() == null) {
            throw new RuntimeException("Target user does not exist");
        }
        if (!isStudentIdentity(user)) {
            throw new RuntimeException("Only student identities can manage a lab");
        }
        Long memberLabId = findActiveMemberLabId(user.getId());
        if (memberLabId == null || (labId != null && !labId.equals(memberLabId))) {
            throw new RuntimeException("The target user must be an active student member of the lab");
        }
    }

    @Override
    public void refreshCompatibilityAccess(Long userId) {
        if (userId == null) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        UserAccessProfile profile = buildProfile(user);
        String targetRole = profile.getDisplayRole();
        Long targetLabId = resolveCompatibilityLabId(user, profile);

        boolean needsUpdate = !equalsIgnoreCase(user.getRole(), targetRole)
                || !equalsNullable(user.getLabId(), targetLabId);
        if (!needsUpdate) {
            return;
        }

        User update = new User();
        update.setId(user.getId());
        update.setRole(targetRole);
        update.setLabId(targetLabId);
        userMapper.updateById(update);
        platformCacheService.evictUserAuthCache(user.getId());
    }

    private String resolvePrimaryIdentity(User user) {
        QueryWrapper<UserIdentity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId())
                .eq("deleted", 0)
                .eq("status", STATUS_ACTIVE)
                .orderByAsc("id");
        List<UserIdentity> identities = userIdentityMapper.selectList(queryWrapper);
        for (UserIdentity identity : identities) {
            if (identity == null || !StringUtils.hasText(identity.getIdentityType())) {
                continue;
            }
            String code = normalizeCode(identity.getIdentityType());
            if (IDENTITY_STUDENT.equals(code)) {
                return IDENTITY_STUDENT;
            }
        }
        for (UserIdentity identity : identities) {
            if (identity == null || !StringUtils.hasText(identity.getIdentityType())) {
                continue;
            }
            return normalizeCode(identity.getIdentityType());
        }
        if (StringUtils.hasText(user.getStudentId())) {
            return IDENTITY_STUDENT;
        }
        return IDENTITY_TEACHER;
    }

    private List<PlatformPost> listActivePlatformPosts(Long userId) {
        QueryWrapper<PlatformPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("deleted", 0)
                .eq("status", STATUS_ACTIVE)
                .orderByAsc("id");
        return platformPostMapper.selectList(queryWrapper);
    }

    private LabMember findActiveLabManagerMembership(Long userId, Long labId) {
        if (userId == null) {
            return null;
        }
        QueryWrapper<LabMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("deleted", 0)
                .eq("status", STATUS_ACTIVE)
                .eq("member_role", MEMBER_ROLE_LAB_ADMIN)
                .orderByAsc("id");
        if (labId != null) {
            queryWrapper.eq("lab_id", labId);
        }
        return labMemberMapper.selectOne(queryWrapper.last("LIMIT 1"));
    }

    private Long findActiveMemberLabId(Long userId) {
        if (userId == null) {
            return null;
        }
        QueryWrapper<LabMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("deleted", 0)
                .eq("status", STATUS_ACTIVE)
                .orderByAsc("id")
                .last("LIMIT 1");
        LabMember member = labMemberMapper.selectOne(queryWrapper);
        return member == null ? null : member.getLabId();
    }

    private LabTeacherRelation findActiveTeacherRelation(Long userId, Long labId) {
        if (userId == null) {
            return null;
        }
        QueryWrapper<LabTeacherRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("deleted", 0)
                .eq("status", STATUS_ACTIVE)
                .orderByDesc("is_primary")
                .orderByAsc("id");
        if (labId != null) {
            queryWrapper.eq("lab_id", labId);
        }
        return labTeacherRelationMapper.selectOne(queryWrapper.last("LIMIT 1"));
    }

    private String resolveDisplayRole(UserAccessProfile profile, User user) {
        if (profile.isSchoolDirector()) {
            return ROLE_SUPER_ADMIN;
        }
        if (profile.isCollegeManager() || profile.isLabManager()) {
            return ROLE_ADMIN;
        }
        if (IDENTITY_TEACHER.equalsIgnoreCase(profile.getPrimaryIdentity())) {
            return ROLE_TEACHER;
        }
        if (user != null && ROLE_SUPER_ADMIN.equalsIgnoreCase(user.getRole())) {
            return ROLE_SUPER_ADMIN;
        }
        if (user != null && ROLE_ADMIN.equalsIgnoreCase(user.getRole()) && !StringUtils.hasText(user.getStudentId())) {
            return ROLE_ADMIN;
        }
        return ROLE_STUDENT;
    }

    private Set<String> buildAuthorityCodes(UserAccessProfile profile) {
        Set<String> authorities = new LinkedHashSet<>();
        if (IDENTITY_STUDENT.equalsIgnoreCase(profile.getPrimaryIdentity())) {
            authorities.add("ROLE_STUDENT");
        }
        if (IDENTITY_TEACHER.equalsIgnoreCase(profile.getPrimaryIdentity())) {
            authorities.add("ROLE_TEACHER");
        }
        if (ROLE_SUPER_ADMIN.equals(profile.getDisplayRole())) {
            authorities.add("ROLE_SUPER_ADMIN");
            return authorities;
        }
        if (ROLE_ADMIN.equals(profile.getDisplayRole())) {
            authorities.add("ROLE_ADMIN");
        }
        if (authorities.isEmpty()) {
            authorities.add("ROLE_STUDENT");
        }
        return authorities;
    }

    private Long resolveCompatibilityLabId(User user, UserAccessProfile profile) {
        if (profile.getManagedLabId() != null) {
            return profile.getManagedLabId();
        }
        if (IDENTITY_STUDENT.equalsIgnoreCase(profile.getPrimaryIdentity())) {
            Long memberLabId = findActiveMemberLabId(user.getId());
            if (memberLabId != null) {
                return memberLabId;
            }
        }
        if (profile.isSchoolDirector() || profile.isCollegeManager()) {
            return null;
        }
        return user.getLabId();
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equalsIgnoreCase(right);
    }

    private boolean equalsNullable(Long left, Long right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}
