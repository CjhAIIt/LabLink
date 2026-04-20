package com.lab.recruitment.config;

public final class PlatformMessagingConstants {

    public static final String BUSINESS_EXCHANGE = "lablink.business.exchange";
    public static final String AUDIT_EXCHANGE = "lablink.audit.exchange";
    public static final String NOTIFY_EXCHANGE = "lablink.notify.exchange";

    public static final String AUDIT_LOG_QUEUE = "audit.log.queue";
    public static final String SYSTEM_NOTIFICATION_QUEUE = "system.notification.queue";
    public static final String STAT_REFRESH_QUEUE = "stat.refresh.queue";

    public static final String ROUTING_AUDIT_LOG = "audit.log";
    public static final String ROUTING_SYSTEM_NOTIFICATION = "notify.system";
    public static final String ROUTING_STAT_REFRESH = "stat.refresh";

    private PlatformMessagingConstants() {
    }
}
