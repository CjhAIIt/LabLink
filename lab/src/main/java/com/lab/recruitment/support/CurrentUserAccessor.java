package com.lab.recruitment.support;

import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CurrentUserAccessor {

    @Autowired
    private UserService userService;

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private LabMapper labMapper;

    public User getCurrentUser() {
        String username = getCurrentUsername();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Current user does not exist");
        }
        return user;
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            throw new RuntimeException("Current user is not logged in");
        }
        return authentication.getName();
    }

    public boolean isSuperAdmin(User user) {
        return userAccessService.buildProfile(user).isSchoolDirector();
    }

    public boolean isAdmin(User user) {
        String displayRole = userAccessService.buildProfile(user).getDisplayRole();
        return "admin".equalsIgnoreCase(displayRole) || "super_admin".equalsIgnoreCase(displayRole);
    }

    public boolean isCollegeManager(User user) {
        return userAccessService.buildProfile(user).isCollegeManager();
    }

    public boolean isTeacherIdentity(User user) {
        return userAccessService.isTeacherIdentity(user);
    }

    public Long resolveManagedCollegeId(User user) {
        return userAccessService.resolveManagedCollegeId(user);
    }

    public boolean isLabManager(User user) {
        return userAccessService.buildProfile(user).isLabManager();
    }

    public boolean isLabScopedManager(User user) {
        return userAccessService.resolveManagedLabId(user) != null;
    }

    public void assertSuperAdmin(User user) {
        if (!isSuperAdmin(user)) {
            throw new RuntimeException("Only school directors can perform this action");
        }
    }

    public void assertTeacherOrAdmin(User user) {
        if (isAdmin(user) || isTeacherIdentity(user)) {
            return;
        }
        throw new RuntimeException("Current account cannot access teacher-side actions");
    }

    public Long resolveLabScope(User user, Long requestedLabId) {
        if (isSuperAdmin(user)) {
            if (requestedLabId == null) {
                throw new RuntimeException("请先选择实验室");
            }
            return requestedLabId;
        }

        Long managedLabId = userAccessService.resolveManagedLabId(user);
        if (managedLabId != null) {
            if (requestedLabId != null && !managedLabId.equals(requestedLabId)) {
                throw new RuntimeException("No access to another lab");
            }
            return managedLabId;
        }

        if (isCollegeManager(user)) {
            Long managedCollegeId = resolveManagedCollegeId(user);
            if (managedCollegeId == null) {
                throw new RuntimeException("Current college manager is not bound to a college");
            }

            Long targetLabId = requestedLabId;
            if (targetLabId == null && user != null) {
                targetLabId = user.getLabId();
            }
            if (targetLabId == null) {
                throw new RuntimeException("Please select a lab first");
            }

            Lab targetLab = labMapper.selectById(targetLabId);
            if (targetLab == null || (targetLab.getDeleted() != null && targetLab.getDeleted() == 1)) {
                throw new RuntimeException("Lab does not exist");
            }
            if (!managedCollegeId.equals(targetLab.getCollegeId())) {
                throw new RuntimeException("No access to another college's lab");
            }
            return targetLabId;
        }

        if (user == null || user.getLabId() == null) {
            throw new RuntimeException("Current account is not bound to a lab");
        }
        if (requestedLabId != null && !user.getLabId().equals(requestedLabId)) {
            throw new RuntimeException("No access to another lab");
        }
        return user.getLabId();
    }

    public void assertLabScope(User user, Long labId) {
        resolveLabScope(user, labId);
    }
}
