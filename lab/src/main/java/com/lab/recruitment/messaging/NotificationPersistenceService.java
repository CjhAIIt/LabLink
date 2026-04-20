package com.lab.recruitment.messaging;

import com.lab.recruitment.entity.SystemNotification;
import com.lab.recruitment.mapper.SystemNotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NotificationPersistenceService {

    @Autowired
    private SystemNotificationMapper systemNotificationMapper;

    public SystemNotification persist(SystemNotificationMessage message) {
        if (message == null || message.getUserId() == null || !StringUtils.hasText(message.getTitle())) {
            return null;
        }

        SystemNotification notification = new SystemNotification();
        notification.setUserId(message.getUserId());
        notification.setTitle(message.getTitle().trim());
        notification.setContent(trimToNull(message.getContent()));
        notification.setNotificationType(trimToNull(message.getNotificationType()));
        notification.setRelatedId(message.getRelatedId());
        notification.setRedirectPath(trimToNull(message.getRedirectPath()));
        notification.setIsRead(0);
        systemNotificationMapper.insert(notification);
        return notification;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
