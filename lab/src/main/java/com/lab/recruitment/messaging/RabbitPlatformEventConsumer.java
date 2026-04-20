package com.lab.recruitment.messaging;

import com.lab.recruitment.config.PlatformMessagingConstants;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lablink.messaging.rabbit", name = "enabled", havingValue = "true")
public class RabbitPlatformEventConsumer {

    private final PlatformEventHandler platformEventHandler;

    public RabbitPlatformEventConsumer(PlatformEventHandler platformEventHandler) {
        this.platformEventHandler = platformEventHandler;
    }

    @RabbitListener(queues = PlatformMessagingConstants.AUDIT_LOG_QUEUE)
    public void consumeAuditLog(AuditLogMessage message) {
        platformEventHandler.handleAuditLog(message);
    }

    @RabbitListener(queues = PlatformMessagingConstants.SYSTEM_NOTIFICATION_QUEUE)
    public void consumeNotification(SystemNotificationMessage message) {
        platformEventHandler.handleNotification(message);
    }

    @RabbitListener(queues = PlatformMessagingConstants.STAT_REFRESH_QUEUE)
    public void consumeStatisticsRefresh(StatisticsRefreshMessage message) {
        platformEventHandler.handleStatisticsRefresh(message);
    }
}
