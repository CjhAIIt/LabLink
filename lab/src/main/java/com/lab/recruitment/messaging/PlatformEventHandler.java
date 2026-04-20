package com.lab.recruitment.messaging;

import com.lab.recruitment.service.PlatformCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlatformEventHandler {

    @Autowired
    private AuditLogPersistenceService auditLogPersistenceService;

    @Autowired
    private NotificationPersistenceService notificationPersistenceService;

    @Autowired
    private PlatformCacheService platformCacheService;

    public void handleAuditLog(AuditLogMessage message) {
        auditLogPersistenceService.persist(message);
    }

    public void handleNotification(SystemNotificationMessage message) {
        notificationPersistenceService.persist(message);
    }

    public void handleStatisticsRefresh(StatisticsRefreshMessage message) {
        platformCacheService.evictAllStatisticsCaches();
        platformCacheService.evictSearchCaches();
    }
}
