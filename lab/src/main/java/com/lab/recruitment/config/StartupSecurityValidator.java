package com.lab.recruitment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 生产环境启动自检。
 *
 * <p>密钥、CORS 白名单这类问题在开发期几乎不会暴露，却会在上线后直接变成可利用的入口。
 * 这里在应用就绪时统一检查，production 环境下不满足要求直接终止进程，避免带着默认密钥对外提供服务。</p>
 */
@Component
public class StartupSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupSecurityValidator.class);

    /**
     * 仓库中出现过的默认 JWT 密钥，全部视为不安全。
     */
    private static final List<String> KNOWN_DEFAULT_JWT_SECRETS = List.of(
            "labRecruitmentSecretKey2023ForJWTTokenGenerationAndValidationThisIsASecureKey",
            "ChangeThisJwtSecretBeforeProduction"
    );

    /**
     * 仓库中出现过的默认数据库口令，全部视为不安全。
     */
    private static final List<String> KNOWN_DEFAULT_DB_PASSWORDS = List.of("cjh041217");

    /**
     * 仓库中出现过的默认 CORS 白名单占位值，视为未配置。
     */
    private static final List<String> PLACEHOLDER_ORIGINS = List.of(
            "https://your-domain.example.com",
            "http://your-domain.example.com"
    );

    private final AppSecurityProperties securityProperties;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    public StartupSecurityValidator(AppSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * 在容器 refresh 阶段校验，早于 Web 服务器开始接受请求，避免带着默认密钥对外提供服务。
     */
    @PostConstruct
    public void validateOnStartup() {
        List<String> violations = collectViolations();

        if (violations.isEmpty()) {
            if (securityProperties.isProduction()) {
                log.info("生产环境安全自检通过");
            }
            return;
        }

        StringBuilder message = new StringBuilder("启动安全自检未通过：");
        for (String violation : violations) {
            message.append(System.lineSeparator()).append(" - ").append(violation);
        }

        if (securityProperties.isProduction()) {
            log.error(message.toString());
            throw new IllegalStateException(message.toString());
        }

        log.warn(message.toString());
        log.warn("当前环境为 {}，仅告警不阻断启动。生产部署请设置 APP_SECURITY_ENVIRONMENT=production 以启用强制校验。",
                securityProperties.getEnvironment());
    }

    private List<String> collectViolations() {
        return collectViolations(jwtSecret, datasourcePassword, securityProperties.resolveAllowedOrigins());
    }

    /**
     * 校验核心逻辑，参数化以便单元测试直接覆盖。
     *
     * @param jwtSecret        实际生效的 JWT 密钥
     * @param datasourcePassword 实际生效的数据库口令
     * @param origins          实际生效的 CORS 来源白名单
     * @return 问题清单，空列表表示通过
     */
    static List<String> collectViolations(String jwtSecret, String datasourcePassword, List<String> origins) {
        List<String> violations = new ArrayList<>();

        if (!StringUtils.hasText(jwtSecret)) {
            violations.add("JWT 密钥为空，请设置 JWT_SECRET 环境变量");
        } else {
            if (jwtSecret.trim().length() < 32) {
                violations.add("JWT 密钥长度不足 32 字符，HMAC-SHA256 要求至少 256 位密钥");
            }
            if (isKnownDefault(jwtSecret, KNOWN_DEFAULT_JWT_SECRETS)) {
                violations.add("JWT 密钥仍是仓库中的默认值，必须更换为随机生成的强密钥");
            }
        }

        if (isKnownDefault(datasourcePassword, KNOWN_DEFAULT_DB_PASSWORDS)) {
            violations.add("数据库口令仍是仓库中的默认值，必须更换");
        }

        if (origins == null || origins.isEmpty()) {
            violations.add("CORS 来源白名单为空，请设置 APP_SECURITY_ALLOWED_ORIGINS_CSV");
            return violations;
        }

        for (String origin : origins) {
            if (origin == null) {
                continue;
            }
            String normalized = origin.trim().toLowerCase(Locale.ROOT);
            if ("*".equals(normalized)) {
                violations.add("CORS 来源白名单包含通配符 *，生产环境必须收敛为具体域名");
                continue;
            }
            if (PLACEHOLDER_ORIGINS.contains(normalized)) {
                violations.add("CORS 来源白名单仍是示例占位域名，必须替换为真实域名");
            }
        }

        return violations;
    }

    private static boolean isKnownDefault(String value, List<String> knownDefaults) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (String known : knownDefaults) {
            if (normalized.equals(known.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
