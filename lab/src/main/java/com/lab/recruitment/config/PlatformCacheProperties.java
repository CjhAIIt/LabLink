package com.lab.recruitment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lablink.cache")
public class PlatformCacheProperties {

    private final Redis redis = new Redis();

    public Redis getRedis() {
        return redis;
    }

    public static class Redis {

        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
