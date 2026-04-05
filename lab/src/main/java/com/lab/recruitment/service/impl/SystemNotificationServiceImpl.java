package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.SystemNotification;
import com.lab.recruitment.mapper.SystemNotificationMapper;
import com.lab.recruitment.service.SystemNotificationService;
import org.springframework.stereotype.Service;

@Service
public class SystemNotificationServiceImpl extends ServiceImpl<SystemNotificationMapper, SystemNotification> implements SystemNotificationService {

    @Override
    public SystemNotification createNotification(Long userId, String title, String content, String type, Long relatedId) {
        SystemNotification notification = new SystemNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setNotificationType(type);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        this.save(notification);
        return notification;
    }
}
