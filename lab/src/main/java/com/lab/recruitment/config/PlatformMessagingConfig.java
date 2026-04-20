package com.lab.recruitment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "lablink.messaging.rabbit", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RabbitMessagingProperties.class)
public class PlatformMessagingConfig {

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange businessExchange() {
        return new DirectExchange(PlatformMessagingConstants.BUSINESS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange auditExchange() {
        return new DirectExchange(PlatformMessagingConstants.AUDIT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange notifyExchange() {
        return new DirectExchange(PlatformMessagingConstants.NOTIFY_EXCHANGE, true, false);
    }

    @Bean
    public Queue auditLogQueue() {
        return new Queue(PlatformMessagingConstants.AUDIT_LOG_QUEUE, true);
    }

    @Bean
    public Queue systemNotificationQueue() {
        return new Queue(PlatformMessagingConstants.SYSTEM_NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Queue statRefreshQueue() {
        return new Queue(PlatformMessagingConstants.STAT_REFRESH_QUEUE, true);
    }

    @Bean
    public Binding auditLogBinding(Queue auditLogQueue, DirectExchange auditExchange) {
        return BindingBuilder.bind(auditLogQueue)
                .to(auditExchange)
                .with(PlatformMessagingConstants.ROUTING_AUDIT_LOG);
    }

    @Bean
    public Binding systemNotificationBinding(Queue systemNotificationQueue, DirectExchange notifyExchange) {
        return BindingBuilder.bind(systemNotificationQueue)
                .to(notifyExchange)
                .with(PlatformMessagingConstants.ROUTING_SYSTEM_NOTIFICATION);
    }

    @Bean
    public Binding statRefreshBinding(Queue statRefreshQueue, DirectExchange businessExchange) {
        return BindingBuilder.bind(statRefreshQueue)
                .to(businessExchange)
                .with(PlatformMessagingConstants.ROUTING_STAT_REFRESH);
    }
}
