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

    /**
     * 运行环境标识。local 仅告警，production 会强制校验密钥与来源白名单，不满足直接拒绝启动。
     */
    private String environment = "local";

    private List<String> allowedOrigins = new ArrayList<>(Arrays.asList(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://localhost:4173",
            "http://127.0.0.1:4173",
            "http://localhost:5173",
            "http://127.0.0.1:5173"
    ));

    private RateLimitProperties rateLimit = new RateLimitProperties();

    private JudgeProperties judge = new JudgeProperties();

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }

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

    /**
     * 解析跨域来源匹配规则。
     *
     * <p>与 {@link #resolveAllowedOrigins()} 的区别在于这里允许使用通配：
     * {@code http://localhost:[*]} 会匹配 localhost 的任意端口。</p>
     *
     * <p>本地开发时前端开发服务器的端口不固定（5173 被占用就会顺延），
     * 而浏览器对同源 POST 也会带上 {@code Origin} 头，
     * 开发代理把该头原样转发到后端后就会命中白名单校验并返回
     * {@code Invalid CORS request}，登录这类写操作全部失败。
     * 因此非生产环境额外放行回环地址的任意端口，生产环境不做任何放宽。</p>
     */
    public List<String> resolveAllowedOriginPatterns() {
        List<String> patterns = new ArrayList<>(resolveAllowedOrigins());
        if (!isProduction()) {
            patterns.add("http://localhost:[*]");
            patterns.add("http://127.0.0.1:[*]");
        }
        return patterns;
    }

    public RateLimitProperties getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
    }

    /**
     * 代码评测（在线判题）相关配置。
     */
    public static class JudgeProperties {
        /**
         * 是否开启在线判题。执行用户提交的代码存在逃逸风险，默认关闭，需要时显式开启。
         */
        private boolean enabled = false;

        /**
         * 单次评测的墙钟超时（秒）。
         */
        private int timeoutSeconds = 5;

        /**
         * Java 评测进程的最大堆内存（MB）。
         */
        private int maxMemoryMb = 128;

        /**
         * 单次评测允许捕获的最大输出字节数。
         */
        private int maxOutputBytes = 64 * 1024;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxMemoryMb() {
            return maxMemoryMb;
        }

        public void setMaxMemoryMb(int maxMemoryMb) {
            this.maxMemoryMb = maxMemoryMb;
        }

        public int getMaxOutputBytes() {
            return maxOutputBytes;
        }

        public void setMaxOutputBytes(int maxOutputBytes) {
            this.maxOutputBytes = maxOutputBytes;
        }
    }

    public JudgeProperties getJudge() {
        return judge;
    }

    public void setJudge(JudgeProperties judge) {
        this.judge = judge;
    }

    public static class RateLimitProperties {
        private LimitRule login = new LimitRule(8, 60);
        private LimitRule register = new LimitRule(4, 300);
        private LimitRule emailCode = new LimitRule(3, 300);
        private LimitRule passwordReset = new LimitRule(5, 300);
        private LimitRule upload = new LimitRule(20, 300);
        private LimitRule judge = new LimitRule(10, 60);

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

        public LimitRule getJudge() {
            return judge;
        }

        public void setJudge(LimitRule judge) {
            this.judge = judge;
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
