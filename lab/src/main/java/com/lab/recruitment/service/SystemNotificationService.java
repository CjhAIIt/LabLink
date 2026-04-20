package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.entity.SystemNotification;
import com.lab.recruitment.entity.User;

public interface SystemNotificationService extends IService<SystemNotification> {

    SystemNotification createNotification(Long userId, String title, String content, String type, Long relatedId);

    SystemNotification createNotification(Long userId, String title, String content, String type,
                                          Long relatedId, String redirectPath);

    void createNotificationAsync(Long userId, String title, String content, String type, Long relatedId);

    void createNotificationAsync(Long userId, String title, String content, String type,
                                 Long relatedId, String redirectPath);

    Page<SystemNotification> getMyNotifications(User currentUser, Integer pageNum, Integer pageSize,
                                                Integer isRead, String notificationType);

    long countUnread(User currentUser);

    boolean markRead(User currentUser, Long notificationId);

    boolean markAllRead(User currentUser);
}
