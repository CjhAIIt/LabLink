package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.SystemNotification;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.SystemNotificationService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private SystemNotificationService systemNotificationService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<SystemNotification>> getMyNotifications(@RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                                               @RequestParam(required = false) Integer isRead,
                                                               @RequestParam(required = false) String notificationType) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(systemNotificationService.getMyNotifications(
                    currentUser, pageNum, pageSize, isRead, notificationType
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getUnreadCount() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            Map<String, Object> data = new HashMap<>();
            data.put("unreadCount", systemNotificationService.countUnread(currentUser));
            return Result.apiSuccess(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/read/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> markRead(@PathVariable Long notificationId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(systemNotificationService.markRead(currentUser, notificationId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> markAllRead() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(systemNotificationService.markAllRead(currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
