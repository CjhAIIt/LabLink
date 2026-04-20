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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SystemNotificationServiceImpl extends ServiceImpl<SystemNotificationMapper, SystemNotification>
        implements SystemNotificationService {

    @Autowired
    private NotificationPersistenceService notificationPersistenceService;

    @Autowired
    private PlatformEventPublisher platformEventPublisher;

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
}
