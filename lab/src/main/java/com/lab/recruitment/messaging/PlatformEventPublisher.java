package com.lab.recruitment.messaging;

public interface PlatformEventPublisher {

    void publishAuditLog(AuditLogMessage message);

    void publishNotification(SystemNotificationMessage message);

    void publishStatisticsRefresh(StatisticsRefreshMessage message);
}
