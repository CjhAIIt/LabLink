package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.SystemNotification;
import com.lab.recruitment.messaging.NotificationPersistenceService;
import com.lab.recruitment.messaging.PlatformEventPublisher;
import com.lab.recruitment.messaging.SystemNotificationMessage;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.SystemNotificationMapper;
import com.lab.recruitment.service.SystemNotificationService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.DataScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemNotificationServiceImpl extends ServiceImpl<SystemNotificationMapper, SystemNotification>
        implements SystemNotificationService {

    @Autowired
    private NotificationPersistenceService notificationPersistenceService;

    @Autowired
    private PlatformEventPublisher platformEventPublisher;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public SystemNotification createNotification(Long userId, String title, String content, String type, Long relatedId) {
        return createNotification(userId, title, content, type, relatedId, null);
    }

    @Override
    public SystemNotification createNotification(Long userId, String title, String content, String type,
                                                 Long relatedId, String redirectPath) {
        return notificationPersistenceService.persist(buildMessage(userId, title, content, type, relatedId, redirectPath));
    }

    @Override
    public void createNotificationAsync(Long userId, String title, String content, String type, Long relatedId) {
        createNotificationAsync(userId, title, content, type, relatedId, null);
    }

    @Override
    public void createNotificationAsync(Long userId, String title, String content, String type,
                                        Long relatedId, String redirectPath) {
        platformEventPublisher.publishNotification(buildMessage(userId, title, content, type, relatedId, redirectPath));
    }

    @Override
    public Page<SystemNotification> getMyNotifications(User currentUser, Integer pageNum, Integer pageSize,
                                                       Integer isRead, String notificationType) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new RuntimeException("当前用户不存在");
        }

        Page<SystemNotification> page = new Page<>(
                Math.max(pageNum == null ? 1 : pageNum, 1),
                Math.max(pageSize == null ? 10 : pageSize, 1)
        );
        QueryWrapper<SystemNotification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", currentUser.getId())
                .eq("deleted", 0)
                .orderByDesc("create_time")
                .orderByDesc("id");
        if (isRead != null) {
            queryWrapper.eq("is_read", isRead);
        }
        if (StringUtils.hasText(notificationType)) {
            queryWrapper.eq("notification_type", notificationType.trim());
        }
        return this.page(page, queryWrapper);
    }

    @Override
    public long countUnread(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new RuntimeException("当前用户不存在");
        }
        QueryWrapper<SystemNotification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", currentUser.getId())
                .eq("deleted", 0)
                .eq("is_read", 0);
        return this.count(queryWrapper);
    }

    @Override
    public Map<String, Object> getTodoSummary(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new RuntimeException("当前用户不存在");
        }
        DataScope scope = currentUserAccessor.buildDataScope(currentUser);
        List<Map<String, Object>> items = new ArrayList<>();

        if (scope.isSchoolLevel()) {
            addTodo(items, "teacher_register_college", "教师注册待学院初审",
                    safeCount("SELECT COUNT(*) FROM t_teacher_register_apply WHERE deleted = 0 AND status = 'submitted'"),
                    "/admin/teacher-register-applies");
            addTodo(items, "teacher_register_school", "教师注册待学校终审",
                    safeCount("SELECT COUNT(*) FROM t_teacher_register_apply WHERE deleted = 0 AND status = 'college_approved'"),
                    "/admin/teacher-register-applies");
            addTodo(items, "lab_create_college", "实验室创建待学院初审",
                    safeCount("SELECT COUNT(*) FROM t_lab_create_apply WHERE deleted = 0 AND status = 'submitted'"),
                    "/admin/create-applies");
            addTodo(items, "lab_create_school", "实验室创建待学校终审",
                    safeCount("SELECT COUNT(*) FROM t_lab_create_apply WHERE deleted = 0 AND status = 'college_approved'"),
                    "/admin/create-applies");
            addTodo(items, "lab_info_review", "实验室资料变更待审核",
                    safeCount("SELECT COUNT(*) FROM t_lab_info_change_review WHERE deleted = 0 AND review_status = 'PENDING'"),
                    "/admin/lab-info");
            addTodo(items, "student_profile_review", "成员资料待审核",
                    safeCount("SELECT COUNT(*) FROM t_student_profile_review WHERE deleted = 0 AND review_status = 'PENDING'"),
                    "/admin/profiles");
        } else if (scope.isCollegeLevel()) {
            Long collegeId = scope.getCollegeId();
            addTodo(items, "teacher_register_college", "本学院教师注册待初审",
                    safeCount("SELECT COUNT(*) FROM t_teacher_register_apply WHERE deleted = 0 AND college_id = ? AND status = 'submitted'", collegeId),
                    "/admin/teacher-register-applies");
            addTodo(items, "teacher_register_school", "本学院教师注册待终审",
                    safeCount("SELECT COUNT(*) FROM t_teacher_register_apply WHERE deleted = 0 AND college_id = ? AND status = 'college_approved'", collegeId),
                    "/admin/teacher-register-applies");
            addTodo(items, "lab_create_college", "本学院实验室创建待初审",
                    safeCount("SELECT COUNT(*) FROM t_lab_create_apply WHERE deleted = 0 AND college_id = ? AND status = 'submitted'", collegeId),
                    "/admin/create-applies");
            addTodo(items, "lab_create_school", "本学院实验室创建待终审",
                    safeCount("SELECT COUNT(*) FROM t_lab_create_apply WHERE deleted = 0 AND college_id = ? AND status = 'college_approved'", collegeId),
                    "/admin/create-applies");
            addTodo(items, "lab_info_review", "本学院实验室资料变更待审核",
                    safeCount("SELECT COUNT(*) FROM t_lab_info_change_review r INNER JOIN t_lab l ON l.id = r.lab_id AND l.deleted = 0 WHERE r.deleted = 0 AND r.review_status = 'PENDING' AND l.college_id = ?", collegeId),
                    "/admin/lab-info");
            addTodo(items, "student_profile_review", "本学院成员资料待审核",
                    safeCount("SELECT COUNT(*) FROM t_student_profile_review r INNER JOIN t_student_profile p ON p.id = r.profile_id AND p.deleted = 0 WHERE r.deleted = 0 AND r.review_status = 'PENDING' AND p.college_id = ?", collegeId),
                    "/admin/profiles");
        } else if (scope.isLabLevel()) {
            Long labId = scope.getLabId();
            addTodo(items, "lab_apply_review", "入组申请待审核",
                    safeCount("SELECT COUNT(*) FROM t_lab_apply WHERE deleted = 0 AND lab_id = ? AND status IN ('submitted', 'leader_approved')", labId),
                    "/admin/applications");
            addTodo(items, "lab_exit_review", "退组申请待处理",
                    safeCount("SELECT COUNT(*) FROM t_lab_exit_application WHERE deleted = 0 AND lab_id = ? AND status = 0", labId),
                    "/admin/workspace");
            addTodo(items, "student_profile_review", "成员资料待审核",
                    safeCount("SELECT COUNT(*) FROM t_student_profile_review r INNER JOIN t_student_profile p ON p.id = r.profile_id AND p.deleted = 0 WHERE r.deleted = 0 AND r.review_status = 'PENDING' AND p.lab_id = ?", labId),
                    "/admin/profiles");
        }

        long pendingCount = 0L;
        for (Map<String, Object> item : items) {
            Object value = item.get("value");
            if (value instanceof Number) {
                pendingCount += ((Number) value).longValue();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pendingCount", pendingCount);
        result.put("items", items);
        return result;
    }

    @Override
    public boolean markRead(User currentUser, Long notificationId) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new RuntimeException("当前用户不存在");
        }
        SystemNotification notification = this.getById(notificationId);
        if (notification == null || notification.getDeleted() != null && notification.getDeleted() == 1) {
            throw new RuntimeException("通知不存在");
        }
        if (!currentUser.getId().equals(notification.getUserId())) {
            throw new RuntimeException("只能操作自己的通知");
        }
        if (notification.getIsRead() != null && notification.getIsRead() == 1) {
            return true;
        }
        notification.setIsRead(1);
        return this.updateById(notification);
    }

    @Override
    public boolean markAllRead(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new RuntimeException("当前用户不存在");
        }
        SystemNotification update = new SystemNotification();
        update.setIsRead(1);

        QueryWrapper<SystemNotification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", currentUser.getId())
                .eq("deleted", 0)
                .eq("is_read", 0);
        return this.update(update, queryWrapper);
    }

    private SystemNotificationMessage buildMessage(Long userId, String title, String content, String type,
                                                   Long relatedId, String redirectPath) {
        SystemNotificationMessage message = new SystemNotificationMessage();
        message.setUserId(userId);
        message.setTitle(StringUtils.hasText(title) ? title.trim() : null);
        message.setContent(StringUtils.hasText(content) ? content.trim() : null);
        message.setNotificationType(StringUtils.hasText(type) ? type.trim() : null);
        message.setRelatedId(relatedId);
        message.setRedirectPath(StringUtils.hasText(redirectPath) ? redirectPath.trim() : null);
        return message;
    }

    private void addTodo(List<Map<String, Object>> items, String key, String label, long value, String route) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("label", label);
        item.put("value", value);
        item.put("route", route);
        items.add(item);
    }

    private long safeCount(String sql, Object... args) {
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
            return count == null ? 0L : count;
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
