package com.lab.recruitment.service.impl;

import com.lab.recruitment.config.PlatformCacheNames;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.service.AuthContextService;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.support.UserAccessProfile;
import com.lab.recruitment.vo.AuthMenuVO;
import com.lab.recruitment.vo.UserProfileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthContextServiceImpl implements AuthContextService {

    private static final String PORTAL_ADMIN = "admin";
    private static final String PORTAL_TEACHER = "teacher";
    private static final String PORTAL_STUDENT = "student";

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private LabMapper labMapper;

    @Override
    public UserProfileVO buildContext(User user) {
        UserAccessProfile profile = userAccessService.buildProfile(user);
        Long resolvedLabId = userAccessService.resolveManagedLabId(user);
        Long contextLabId = resolvedLabId;
        if (contextLabId == null && !UserAccessServiceImpl.IDENTITY_STUDENT.equalsIgnoreCase(profile.getPrimaryIdentity())) {
            contextLabId = user.getLabId();
        }

        UserProfileVO profileVO = new UserProfileVO();
        profileVO.setId(user.getId());
        profileVO.setUsername(user.getUsername());
        profileVO.setRealName(user.getRealName());
        profileVO.setRole(profile.getDisplayRole());
        profileVO.setStudentId(user.getStudentId());
        profileVO.setCollege(user.getCollege());
        profileVO.setMajor(user.getMajor());
        profileVO.setGrade(user.getGrade());
        profileVO.setPhone(user.getPhone());
        profileVO.setEmail(user.getEmail());
        profileVO.setAvatar(user.getAvatar());
        profileVO.setResume(user.getResume());
        profileVO.setLabId(contextLabId);
        profileVO.setManagedLabId(profile.getManagedLabId());
        profileVO.setLabName(resolveLabName(contextLabId));
        profileVO.setCanEdit(user.getCanEdit());
        profileVO.setStatus(user.getStatus());
        profileVO.setPrimaryIdentity(profile.getPrimaryIdentity());
        profileVO.setLabMemberRole(profile.getLabMemberRole());
        profileVO.setManagedCollegeId(profile.getManagedCollegeId());
        profileVO.setSchoolDirector(profile.isSchoolDirector());
        profileVO.setCollegeManager(profile.isCollegeManager());
        profileVO.setLabManager(profile.isLabManager());
        profileVO.setPlatformPostCodes(profile.getPlatformPostCodes());
        profileVO.setPortalRole(resolvePortalRole(profile));
        profileVO.setAuthorities(new ArrayList<>(profile.getAuthorities()));
        profileVO.setPermissions(buildPermissions(user));
        return profileVO;
    }

    @Override
    @Cacheable(cacheNames = PlatformCacheNames.AUTH_MENU, key = "#user.id", condition = "#user != null && #user.id != null")
    public List<AuthMenuVO> buildMenus(User user) {
        UserAccessProfile profile = userAccessService.buildProfile(user);
        Long resolvedLabId = userAccessService.resolveManagedLabId(user);
        List<AuthMenuVO> menus = new ArrayList<>();
        String portalRole = resolvePortalRole(profile);

        if (PORTAL_ADMIN.equals(portalRole)) {
            menus.add(menu("admin:dashboard", "/admin/dashboard", null, "工作台", "DataBoard"));
            if (profile.isSchoolDirector() || profile.isCollegeManager()) {
                menus.add(menu("admin:search", "/admin/search", null, "综合搜索", "Search"));
            }
            if (profile.isSchoolDirector()) {
                menus.add(menu("admin:colleges", "/admin/colleges", null, "学院管理", "OfficeBuilding"));
            }
            if (profile.isSchoolDirector() || profile.isCollegeManager() || profile.isLabManager()) {
                menus.add(menu("admin:labs", "/admin/labs", null, "实验室管理", "FolderOpened"));
                if (profile.isLabManager()) {
                    menus.add(menu("admin:lab-info", "/admin/lab-info", null, "实验室资料", "Files"));
                }
                menus.add(menu("admin:attendance", "/admin/attendance-tasks", null, "考勤管理", "Calendar"));
                menus.add(menu("admin:exam-hub", "/admin/exam-hub", null, "笔试中心", "EditPen"));
                menus.add(menu("admin:ai-interview-records", "/admin/ai-interview-records", null, "AI 面试记录", "ChatDotRound"));
                menus.add(menu("admin:applications", "/admin/applications", null, "入组申请", "Tickets"));
                menus.add(menu("admin:members", "/admin/members", null, "成员管理", "UserFilled"));
                menus.add(menu("admin:workspace", "/admin/workspace", null, "资料空间", "Files"));
                menus.add(menu("admin:devices", "/admin/devices", null, "设备管理", "Monitor"));
                menus.add(menu("admin:notices", "/admin/notices", null, "公告管理", "Bell"));
                menus.add(menu("admin:statistics", "/admin/statistics", null, "统计分析", "TrendCharts"));
                menus.add(menu("admin:profiles", "/admin/profiles", null, "成员资料", "Files"));
                menus.add(menu("admin:audit", "/admin/audit", null, "审计日志", "Tickets"));
            }
            if (profile.isSchoolDirector() || profile.isCollegeManager()) {
                menus.add(menu("admin:create-applies", "/admin/create-applies", null, "创建审批", "Tickets"));
                menus.add(menu("admin:teacher-register-applies", "/admin/teacher-register-applies", null, "教师注册审批", "UserFilled"));
            }
            menus.add(menu("admin:profile", "/admin/profile", null, "个人资料", "UserFilled"));
            return menus;
        }

        if (PORTAL_TEACHER.equals(portalRole)) {
            menus.add(menu("teacher:dashboard", "/teacher/dashboard", "/m/teacher/dashboard", "工作台", "DataBoard"));
            menus.add(menu("teacher:create-applies", "/teacher/create-applies", "/m/teacher/create-applies", "创建申请", "Tickets"));
            if (resolvedLabId != null) {
                menus.add(menu("teacher:attendance", "/teacher/attendance", "/m/teacher/attendance", "考勤查看", "Calendar"));
                menus.add(menu("teacher:exam-hub", "/teacher/exam-hub", null, "笔试中心", "EditPen"));
                menus.add(menu("teacher:ai-interview-records", "/teacher/ai-interview-records", null, "AI 面试记录", "ChatDotRound"));
                menus.add(menu("teacher:lab-info", "/teacher/lab-info", null, "实验室资料", "Files"));
                menus.add(menu("teacher:profiles", "/teacher/profiles", null, "成员资料", "Files"));
                menus.add(menu("teacher:workspace", "/teacher/workspace", null, "资料空间", "Files"));
            }
            menus.add(menu("teacher:notices", "/teacher/notices", null, "公告通知", "Bell"));
            menus.add(menu("teacher:profile", "/teacher/profile", "/m/teacher/profile", "个人资料", "User"));
            return menus;
        }

        menus.add(menu("student:dashboard", "/student/dashboard", "/m/student/dashboard", "工作台", "DataBoard"));
        menus.add(menu("student:labs", "/student/labs", "/m/student/labs", "实验室广场", "FolderOpened"));
        menus.add(menu("student:applications", "/student/applications", null, "我的申请", "Tickets"));
        if (resolvedLabId != null) {
            menus.add(menu("student:my-lab", "/student/my-lab", null, "我的实验室", "UserFilled"));
            menus.add(menu("student:attendance", "/student/attendance", null, "我的考勤", "Calendar"));
            menus.add(menu("student:exam-center", "/student/exam-center", null, "笔试中心", "EditPen"));
            menus.add(menu("student:ai-interview", "/student/ai-interview", null, "AI 智能面试", "ChatDotRound"));
            menus.add(menu("student:space", "/student/space", null, "资料空间", "Files"));
        }
        menus.add(menu("student:notices", "/student/notices", "/m/student/notices", "公告通知", "Bell"));
        menus.add(menu("student:profile", "/student/profile", "/m/student/profile", "个人资料", "User"));
        return menus;
    }

    @Override
    @Cacheable(cacheNames = PlatformCacheNames.AUTH_PERMISSION, key = "#user.id", condition = "#user != null && #user.id != null")
    public List<String> buildPermissions(User user) {
        UserAccessProfile profile = userAccessService.buildProfile(user);
        Set<String> permissions = new LinkedHashSet<>();

        permissions.add("auth:me:view");
        permissions.add("auth:menus:view");
        permissions.add("auth:permissions:view");
        permissions.add("profile:self:view");
        permissions.add("notification:view");

        if (profile.isSchoolDirector()) {
            permissions.add("user:manage");
            permissions.add("role:manage");
            permissions.add("lab:manage");
            permissions.add("lab:create:audit");
            permissions.add("lab:apply:audit");
            permissions.add("teacher:register:audit");
            permissions.add("member:manage");
            permissions.add("search:global:view");
            permissions.add("statistics:school:view");
            permissions.add("attendance:task:manage");
            permissions.add("attendance:record:manage");
            permissions.add("exam:manage");
            permissions.add("ai-interview:record:view");
            permissions.add("device:manage");
            permissions.add("notice:manage");
            permissions.add("workspace:school:view");
            permissions.add("profile:review");
            permissions.add("audit:view");
        }

        if (profile.isCollegeManager()) {
            permissions.add("lab:manage");
            permissions.add("lab:create:audit");
            permissions.add("lab:apply:audit");
            permissions.add("teacher:register:audit");
            permissions.add("member:manage");
            permissions.add("search:global:view");
            permissions.add("attendance:task:manage");
            permissions.add("attendance:record:manage");
            permissions.add("exam:manage");
            permissions.add("ai-interview:record:view");
            permissions.add("device:manage");
            permissions.add("notice:manage");
            permissions.add("workspace:college:view");
            permissions.add("statistics:college:view");
            permissions.add("profile:review");
            permissions.add("audit:view");
        }

        if (profile.isLabManager()) {
            permissions.add("lab:manage");
            permissions.add("lab:apply:audit");
            permissions.add("member:manage");
            permissions.add("profile:review");
            permissions.add("attendance:record:manage");
            permissions.add("exam:manage");
            permissions.add("ai-interview:record:view");
            permissions.add("device:manage");
            permissions.add("notice:manage");
            permissions.add("workspace:lab:manage");
            permissions.add("statistics:lab:view");
            permissions.add("audit:view");
        }

        if (UserAccessServiceImpl.IDENTITY_TEACHER.equalsIgnoreCase(profile.getPrimaryIdentity())) {
            permissions.add("teacher:portal:view");
            permissions.add("lab:create:apply");
            permissions.add("lab:profile:submit");
            permissions.add("workspace:lab:manage");
            permissions.add("profile:view");
            permissions.add("attendance:view");
            permissions.add("exam:manage");
            permissions.add("ai-interview:record:view");
            permissions.add("device:view");
            permissions.add("statistics:view");
        }

        if (UserAccessServiceImpl.IDENTITY_STUDENT.equalsIgnoreCase(profile.getPrimaryIdentity())) {
            permissions.add("student:portal:view");
            permissions.add("lab:apply:self");
            permissions.add("attendance:self:view");
            permissions.add("exam:self:view");
            permissions.add("ai-interview:self:view");
            permissions.add("device:view");
            permissions.add("workspace:self:view");
        }

        return new ArrayList<>(permissions);
    }

    private String resolvePortalRole(UserAccessProfile profile) {
        if (UserAccessServiceImpl.ROLE_SUPER_ADMIN.equalsIgnoreCase(profile.getDisplayRole())
                || UserAccessServiceImpl.ROLE_ADMIN.equalsIgnoreCase(profile.getDisplayRole())) {
            return PORTAL_ADMIN;
        }
        if (UserAccessServiceImpl.IDENTITY_TEACHER.equalsIgnoreCase(profile.getPrimaryIdentity())) {
            return PORTAL_TEACHER;
        }
        return PORTAL_STUDENT;
    }

    private AuthMenuVO menu(String key, String path, String mobilePath, String label, String icon) {
        AuthMenuVO menu = new AuthMenuVO();
        menu.setKey(key);
        menu.setPath(path);
        menu.setMobilePath(mobilePath);
        menu.setLabel(label);
        menu.setIcon(icon);
        return menu;
    }

    private String resolveLabName(Long labId) {
        if (labId == null) {
            return null;
        }
        Lab lab = labMapper.selectById(labId);
        if (lab == null || (lab.getDeleted() != null && lab.getDeleted() == 1)) {
            return null;
        }
        return lab.getLabName();
    }
}
