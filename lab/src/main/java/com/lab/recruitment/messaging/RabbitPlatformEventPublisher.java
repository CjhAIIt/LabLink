package com.lab.recruitment.messaging;

import com.lab.recruitment.config.PlatformMessagingConstants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lablink.messaging.rabbit", name = "enabled", havingValue = "true")
public class RabbitPlatformEventPublisher implements PlatformEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final PlatformEventHandler platformEventHandler;

    public RabbitPlatformEventPublisher(RabbitTemplate rabbitTemplate, PlatformEventHandler platformEventHandler) {
        this.rabbitTemplate = rabbitTemplate;
        this.platformEventHandler = platformEventHandler;
    }

    @Override
    public void publishAuditLog(AuditLogMessage message) {
        publishSafely(PlatformMessagingConstants.AUDIT_EXCHANGE, PlatformMessagingConstants.ROUTING_AUDIT_LOG, message,
                () -> platformEventHandler.handleAuditLog(message));
    }

    @Override
    public void publishNotification(SystemNotificationMessage message) {
        publishSafely(PlatformMessagingConstants.NOTIFY_EXCHANGE, PlatformMessagingConstants.ROUTING_SYSTEM_NOTIFICATION, message,
                () -> platformEventHandler.handleNotification(message));
    }

    @Override
    public void publishStatisticsRefresh(StatisticsRefreshMessage message) {
        publishSafely(PlatformMessagingConstants.BUSINESS_EXCHANGE, PlatformMessagingConstants.ROUTING_STAT_REFRESH, message,
                () -> platformEventHandler.handleStatisticsRefresh(message));
    }

    private void publishSafely(String exchange, String routingKey, Object payload, Runnable fallback) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (RuntimeException ex) {
            fallback.run();
        }
    }
}
