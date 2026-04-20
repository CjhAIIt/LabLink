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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@Component
public class CurrentUserAccessor {

    public static final String CURRENT_USER_REQUEST_ATTRIBUTE = CurrentUserAccessor.class.getName() + ".currentUser";
    public static final String DATA_SCOPE_REQUEST_ATTRIBUTE = CurrentUserAccessor.class.getName() + ".dataScope";

    @Autowired
    private UserService userService;

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private LabMapper labMapper;

    public User getCurrentUser() {
        User requestUser = getRequestAttribute(CURRENT_USER_REQUEST_ATTRIBUTE, User.class);
        if (requestUser != null) {
            return requestUser;
        }

        String username = getCurrentUsername();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("当前用户不存在");
        }
        setRequestAttribute(CURRENT_USER_REQUEST_ATTRIBUTE, user);
        return user;
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            throw new RuntimeException("当前用户未登录");
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

    public boolean isStudentIdentity(User user) {
        return userAccessService.isStudentIdentity(user);
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
        return userAccessService.buildProfile(user).getManagedLabId() != null;
    }

    public void assertSuperAdmin(User user) {
        if (!isSuperAdmin(user)) {
            throw new RuntimeException("仅学校管理员可执行该操作");
        }
    }

    public void assertTeacherOrAdmin(User user) {
        if (isAdmin(user) || isTeacherIdentity(user)) {
            return;
        }
        throw new RuntimeException("当前账号无权访问教师端功能");
    }

    public DataScope getCurrentDataScope() {
        DataScope scope = getRequestAttribute(DATA_SCOPE_REQUEST_ATTRIBUTE, DataScope.class);
        if (scope != null) {
            return scope;
        }

        scope = buildDataScope(getCurrentUser());
        setRequestAttribute(DATA_SCOPE_REQUEST_ATTRIBUTE, scope);
        return scope;
    }

    public DataScope buildDataScope(User user) {
        UserAccessProfile profile = userAccessService.buildProfile(user);
        DataScope scope = new DataScope();
        scope.setUserId(user == null ? null : user.getId());
        scope.setDisplayRole(profile.getDisplayRole());

        if (profile.isSchoolDirector()) {
            scope.setLevel(DataScopeLevel.SCHOOL);
            return scope;
        }

        if (profile.isCollegeManager()) {
            scope.setLevel(DataScopeLevel.COLLEGE);
            scope.setCollegeId(profile.getManagedCollegeId());
            return scope;
        }

        if (profile.isLabManager()) {
            scope.setLevel(DataScopeLevel.LAB);
            scope.setLabId(profile.getManagedLabId());
            scope.setCollegeId(resolveCollegeIdByLabId(profile.getManagedLabId()));
            return scope;
        }

        Long relatedLabId = userAccessService.resolveManagedLabId(user);
        scope.setLevel(DataScopeLevel.SELF);
        scope.setLabId(relatedLabId);
        scope.setCollegeId(resolveCollegeIdByLabId(relatedLabId));
        return scope;
    }

    public DataScope resolveManagementScope(User user, Long requestedCollegeId, Long requestedLabId) {
        DataScope baseScope = buildDataScope(user);
        DataScope scoped = new DataScope();
        scoped.setLevel(baseScope.getLevel());
        scoped.setUserId(baseScope.getUserId());
        scoped.setDisplayRole(baseScope.getDisplayRole());

        if (baseScope.isSchoolLevel()) {
            validateLabCollegeRelation(requestedCollegeId, requestedLabId);
            scoped.setCollegeId(requestedCollegeId);
            scoped.setLabId(requestedLabId);
            return scoped;
        }

        if (baseScope.isCollegeLevel()) {
            if (baseScope.getCollegeId() == null) {
                throw new RuntimeException("当前学院管理员未绑定学院");
            }
            if (requestedCollegeId != null && !Objects.equals(baseScope.getCollegeId(), requestedCollegeId)) {
                throw new RuntimeException("无权访问其他学院的数据");
            }
            validateLabCollegeRelation(baseScope.getCollegeId(), requestedLabId);
            scoped.setCollegeId(baseScope.getCollegeId());
            scoped.setLabId(requestedLabId);
            return scoped;
        }

        if (baseScope.isLabLevel()) {
            if (baseScope.getLabId() == null) {
                throw new RuntimeException("当前账号未绑定可管理的实验室");
            }
            if (requestedLabId != null && !Objects.equals(baseScope.getLabId(), requestedLabId)) {
                throw new RuntimeException("无权访问其他实验室的数据");
            }
            if (requestedCollegeId != null && !Objects.equals(baseScope.getCollegeId(), requestedCollegeId)) {
                throw new RuntimeException("无权访问其他学院的数据");
            }
            scoped.setCollegeId(baseScope.getCollegeId());
            scoped.setLabId(baseScope.getLabId());
            return scoped;
        }

        throw new RuntimeException("当前账号没有管理数据范围");
    }

    public Long resolveCollegeScope(User user, Long requestedCollegeId) {
        DataScope scope = buildDataScope(user);
        if (scope.isSchoolLevel()) {
            return requestedCollegeId;
        }
        if (scope.isCollegeLevel()) {
            if (requestedCollegeId != null && !Objects.equals(scope.getCollegeId(), requestedCollegeId)) {
                throw new RuntimeException("无权访问其他学院的数据");
            }
            return scope.getCollegeId();
        }
        if (scope.isLabLevel()) {
            if (requestedCollegeId != null && !Objects.equals(scope.getCollegeId(), requestedCollegeId)) {
                throw new RuntimeException("无权访问其他学院的数据");
            }
            return scope.getCollegeId();
        }
        if (scope.getCollegeId() != null) {
            if (requestedCollegeId != null && !Objects.equals(scope.getCollegeId(), requestedCollegeId)) {
                throw new RuntimeException("无权访问其他学院的数据");
            }
            return scope.getCollegeId();
        }
        if (requestedCollegeId != null) {
            throw new RuntimeException("当前账号未绑定学院");
        }
        return null;
    }

    public Long resolveLabScope(User user, Long requestedLabId) {
        if (isSuperAdmin(user)) {
            if (requestedLabId == null) {
                throw new RuntimeException("请先选择实验室");
            }
            return requestedLabId;
        }

        UserAccessProfile profile = userAccessService.buildProfile(user);
        if (profile.getManagedLabId() != null) {
            if (requestedLabId != null && !Objects.equals(profile.getManagedLabId(), requestedLabId)) {
                throw new RuntimeException("无权访问其他实验室的数据");
            }
            return profile.getManagedLabId();
        }

        if (profile.isCollegeManager()) {
            Long managedCollegeId = resolveManagedCollegeId(user);
            if (managedCollegeId == null) {
                throw new RuntimeException("当前学院管理员未绑定学院");
            }

            Long targetLabId = requestedLabId;
            if (targetLabId == null && user != null) {
                targetLabId = user.getLabId();
            }
            if (targetLabId == null) {
                throw new RuntimeException("请先选择实验室");
            }

            Lab targetLab = getActiveLab(targetLabId);
            if (!Objects.equals(managedCollegeId, targetLab.getCollegeId())) {
                throw new RuntimeException("无权访问其他学院的实验室");
            }
            return targetLabId;
        }

        Long ownLabId = userAccessService.resolveManagedLabId(user);
        if (ownLabId == null) {
            throw new RuntimeException("当前账号未绑定实验室");
        }
        if (requestedLabId != null && !Objects.equals(ownLabId, requestedLabId)) {
            throw new RuntimeException("无权访问其他实验室的数据");
        }
        return ownLabId;
    }

    public void assertCollegeScope(User user, Long collegeId) {
        resolveCollegeScope(user, collegeId);
    }

    public void assertLabScope(User user, Long labId) {
        resolveLabScope(user, labId);
    }

    public Long resolveCollegeIdByLabId(Long labId) {
        if (labId == null) {
            return null;
        }
        return getActiveLab(labId).getCollegeId();
    }

    private void validateLabCollegeRelation(Long collegeId, Long labId) {
        if (labId == null) {
            return;
        }
        Lab lab = getActiveLab(labId);
        if (collegeId != null && !Objects.equals(collegeId, lab.getCollegeId())) {
            throw new RuntimeException("所选实验室不属于当前学院范围");
        }
    }

    private Lab getActiveLab(Long labId) {
        Lab targetLab = labMapper.selectById(labId);
        if (targetLab == null || (targetLab.getDeleted() != null && targetLab.getDeleted() == 1)) {
            throw new RuntimeException("实验室不存在");
        }
        return targetLab;
    }

    private <T> T getRequestAttribute(String key, Class<T> targetType) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(key);
        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }
        return null;
    }

    private void setRequestAttribute(String key, Object value) {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            request.setAttribute(key, value);
        }
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        return null;
    }
}
