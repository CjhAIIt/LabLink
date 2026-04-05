package com.lab.recruitment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private List<String> allowedOrigins = new ArrayList<>(Arrays.asList(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://localhost:4173",
            "http://127.0.0.1:4173",
            "http://localhost:5173",
            "http://127.0.0.1:5173"
    ));

    private RateLimitProperties rateLimit = new RateLimitProperties();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> resolveAllowedOrigins() {
        String csv = System.getenv("APP_SECURITY_ALLOWED_ORIGINS_CSV");
        if (StringUtils.hasText(csv)) {
            List<String> resolvedOrigins = new ArrayList<>();
            for (String item : csv.split(",")) {
                String value = item == null ? null : item.trim();
                if (StringUtils.hasText(value)) {
                    resolvedOrigins.add(value);
                }
            }
            if (!resolvedOrigins.isEmpty()) {
                return resolvedOrigins;
            }
        }
        return allowedOrigins;
    }

    public RateLimitProperties getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class RateLimitProperties {
        private LimitRule login = new LimitRule(8, 60);
        private LimitRule register = new LimitRule(4, 300);
        private LimitRule emailCode = new LimitRule(3, 300);
        private LimitRule passwordReset = new LimitRule(5, 300);
        private LimitRule upload = new LimitRule(20, 300);

        public LimitRule getLogin() {
            return login;
        }

        public void setLogin(LimitRule login) {
            this.login = login;
        }

        public LimitRule getRegister() {
            return register;
        }

        public void setRegister(LimitRule register) {
            this.register = register;
        }

        public LimitRule getEmailCode() {
            return emailCode;
        }

        public void setEmailCode(LimitRule emailCode) {
            this.emailCode = emailCode;
        }

        public LimitRule getPasswordReset() {
            return passwordReset;
        }

        public void setPasswordReset(LimitRule passwordReset) {
            this.passwordReset = passwordReset;
        }

        public LimitRule getUpload() {
            return upload;
        }

        public void setUpload(LimitRule upload) {
            this.upload = upload;
        }
    }

    public static class LimitRule {
        private int maxRequests;
        private long windowSeconds;

        public LimitRule() {
        }

        public LimitRule(int maxRequests, long windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
