package com.lab.recruitment.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lablink.messaging.rabbit", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalPlatformEventPublisher implements PlatformEventPublisher {

    private final PlatformEventHandler platformEventHandler;

    public LocalPlatformEventPublisher(PlatformEventHandler platformEventHandler) {
        this.platformEventHandler = platformEventHandler;
    }

    @Override
    public void publishAuditLog(AuditLogMessage message) {
        platformEventHandler.handleAuditLog(message);
    }

    @Override
    public void publishNotification(SystemNotificationMessage message) {
        platformEventHandler.handleNotification(message);
    }

    @Override
    public void publishStatisticsRefresh(StatisticsRefreshMessage message) {
        platformEventHandler.handleStatisticsRefresh(message);
    }
}
