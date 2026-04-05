package com.lab.recruitment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";
    private static final String REGISTER_PATH = "/auth/register";
    private static final String TEACHER_REGISTER_PATH = "/auth/teacher-register";
    private static final String REGISTER_SEND_CODE_PATH = "/auth/register/send-code";
    private static final String TEACHER_REGISTER_SEND_CODE_PATH = "/auth/teacher-register/send-code";
    private static final String PASSWORD_RESET_SEND_CODE_PATH = "/auth/password-reset/send-code";
    private static final String PASSWORD_RESET_CONFIRM_PATH = "/auth/password-reset/confirm";
    private static final String UPLOAD_PATH = "/file/upload";

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AppSecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RateLimitRule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = buildClientKey(request, rule.getPath());
        RateLimitBucket bucket = buckets.computeIfAbsent(clientKey, key -> new RateLimitBucket());
        RateLimitDecision decision = bucket.consume(rule.getMaxRequests(), rule.getWindowSeconds());

        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.getMaxRequests()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(decision.getRemaining(), 0)));

        if (!decision.isAllowed()) {
            response.setStatus(429);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(decision.getRetryAfterSeconds()));
            objectMapper.writeValue(response.getOutputStream(), Result.error(429, "请求过于频繁，请稍后再试"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        String path = normalizePath(request.getRequestURI());
        AppSecurityProperties.RateLimitProperties rateLimit = securityProperties.getRateLimit();

        if (LOGIN_PATH.equals(path)) {
            return new RateLimitRule(LOGIN_PATH, rateLimit.getLogin().getMaxRequests(), rateLimit.getLogin().getWindowSeconds());
        }
        if (REGISTER_PATH.equals(path)) {
            return new RateLimitRule(REGISTER_PATH, rateLimit.getRegister().getMaxRequests(), rateLimit.getRegister().getWindowSeconds());
        }
        if (TEACHER_REGISTER_PATH.equals(path)) {
            return new RateLimitRule(TEACHER_REGISTER_PATH, rateLimit.getRegister().getMaxRequests(), rateLimit.getRegister().getWindowSeconds());
        }
        if (REGISTER_SEND_CODE_PATH.equals(path) || TEACHER_REGISTER_SEND_CODE_PATH.equals(path) || PASSWORD_RESET_SEND_CODE_PATH.equals(path)) {
            return new RateLimitRule(path, rateLimit.getEmailCode().getMaxRequests(), rateLimit.getEmailCode().getWindowSeconds());
        }
        if (PASSWORD_RESET_CONFIRM_PATH.equals(path)) {
            return new RateLimitRule(PASSWORD_RESET_CONFIRM_PATH, rateLimit.getPasswordReset().getMaxRequests(), rateLimit.getPasswordReset().getWindowSeconds());
        }
        if (UPLOAD_PATH.equals(path)) {
            return new RateLimitRule(UPLOAD_PATH, rateLimit.getUpload().getMaxRequests(), rateLimit.getUpload().getWindowSeconds());
        }
        return null;
    }

    private String buildClientKey(HttpServletRequest request, String path) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String clientIp = forwardedFor;
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }
        return path + "|" + clientIp;
    }

    private String normalizePath(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return "";
        }
        return requestUri.startsWith("/api/") ? requestUri.substring(4) : requestUri;
    }

    private static class RateLimitBucket {
        private final AtomicInteger counter = new AtomicInteger(0);
        private volatile long windowStartEpochSecond = 0L;

        private synchronized RateLimitDecision consume(int limit, long windowSeconds) {
            long now = Instant.now().getEpochSecond();
            if (windowStartEpochSecond == 0L || now - windowStartEpochSecond >= windowSeconds) {
                windowStartEpochSecond = now;
                counter.set(0);
            }

            int used = counter.incrementAndGet();
            boolean allowed = used <= limit;
            long retryAfter = allowed ? 0L : Math.max(windowSeconds - (now - windowStartEpochSecond), 1L);
            int remaining = allowed ? limit - used : 0;
            return new RateLimitDecision(allowed, remaining, retryAfter);
        }
    }

    private static class RateLimitRule {
        private final String path;
        private final int maxRequests;
        private final long windowSeconds;

        private RateLimitRule(String path, int maxRequests, long windowSeconds) {
            this.path = path;
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }

        public String getPath() {
            return path;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }
    }

    private static class RateLimitDecision {
        private final boolean allowed;
        private final int remaining;
        private final long retryAfterSeconds;

        private RateLimitDecision(boolean allowed, int remaining, long retryAfterSeconds) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public int getRemaining() {
            return remaining;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
