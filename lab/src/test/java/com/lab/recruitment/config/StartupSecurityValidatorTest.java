package com.lab.recruitment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 启动安全自检的单元测试。
 *
 * <p>这些用例守住的是「弱配置不得进入生产」这条底线：默认值密钥、通配符来源、
 * 占位域名都必须在生产环境被拦下。</p>
 */
class StartupSecurityValidatorTest {

    private static final String STRONG_JWT_SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String DEFAULT_JWT_SECRET =
            "labRecruitmentSecretKey2023ForJWTTokenGenerationAndValidationThisIsASecureKey";
    private static final String PLACEHOLDER_JWT_SECRET = "ChangeThisJwtSecretBeforeProduction";
    private static final String DEFAULT_DB_PASSWORD = "cjh041217";
    private static final List<String> REAL_ORIGINS = Collections.singletonList("https://lab.example.edu.cn");

    @Test
    @DisplayName("强密钥与真实域名通过校验")
    void shouldPassWithStrongSecretAndRealOrigin() {
        List<String> violations = StartupSecurityValidator.collectViolations(
                STRONG_JWT_SECRET, "a-random-db-password", REAL_ORIGINS);

        assertTrue(violations.isEmpty(), "不应产生任何问题：" + violations);
    }

    @Test
    @DisplayName("仓库中的默认 JWT 密钥被拦截")
    void shouldRejectRepositoryDefaultJwtSecret() {
        List<String> violations = StartupSecurityValidator.collectViolations(
                DEFAULT_JWT_SECRET, "a-random-db-password", REAL_ORIGINS);

        assertTrue(violations.stream().anyMatch(item -> item.contains("JWT 密钥仍是仓库中的默认值")),
                "应提示 JWT 密钥仍为默认值：" + violations);
    }

    @Test
    @DisplayName("compose 中的占位 JWT 密钥被拦截")
    void shouldRejectPlaceholderJwtSecret() {
        List<String> violations = StartupSecurityValidator.collectViolations(
                PLACEHOLDER_JWT_SECRET, "a-random-db-password", REAL_ORIGINS);

        assertTrue(violations.stream().anyMatch(item -> item.contains("JWT 密钥仍是仓库中的默认值")),
                "应提示 JWT 密钥仍为默认值：" + violations);
    }

    @Test
    @DisplayName("过短的 JWT 密钥被拦截")
    void shouldRejectTooShortJwtSecret() {
        List<String> violations = StartupSecurityValidator.collectViolations(
                "too-short", "a-random-db-password", REAL_ORIGINS);

        assertTrue(violations.stream().anyMatch(item -> item.contains("长度不足 32 字符")),
                "应提示密钥长度不足：" + violations);
    }

    @Test
    @DisplayName("空的 JWT 密钥被拦截")
    void shouldRejectBlankJwtSecret() {
        List<String> violations = StartupSecurityValidator.collectViolations(
                "   ", "a-random-db-password", REAL_ORIGINS);

        assertTrue(violations.stream().anyMatch(item -> item.contains("JWT 密钥为空")),
                "应提示密钥为空：" + violations);
    }

    @Test
    @DisplayName("仓库中的默认数据库口令被拦截")
    void shouldRejectRepositoryDefaultDbPassword() {
        List<String> violations = StartupSecurityValidator.collectViolations(
                STRONG_JWT_SECRET, DEFAULT_DB_PASSWORD, REAL_ORIGINS);

        assertTrue(violations.stream().anyMatch(item -> item.contains("数据库口令仍是仓库中的默认值")),
                "应提示数据库口令仍为默认值：" + violations);
    }

    @Test
    @DisplayName("CORS 通配符来源被拦截")
    void shouldRejectWildcardOrigin() {
        List<String> violations = StartupSecurityValidator.collectViolations(
                STRONG_JWT_SECRET, "a-random-db-password", Collections.singletonList("*"));

        assertTrue(violations.stream().anyMatch(item -> item.contains("通配符")),
                "应提示来源包含通配符：" + violations);
    }

    @Test
    @DisplayName("示例占位域名被拦截")
    void shouldRejectPlaceholderOrigin() {
        List<String> violations = StartupSecurityValidator.collectViolations(
                STRONG_JWT_SECRET, "a-random-db-password",
                Arrays.asList("https://your-domain.example.com"));

        assertTrue(violations.stream().anyMatch(item -> item.contains("示例占位域名")),
                "应提示来源仍是占位域名：" + violations);
    }

    @Test
    @DisplayName("空的来源白名单被拦截")
    void shouldRejectEmptyOrigins() {
        List<String> violations = StartupSecurityValidator.collectViolations(
                STRONG_JWT_SECRET, "a-random-db-password", Collections.emptyList());

        assertTrue(violations.stream().anyMatch(item -> item.contains("CORS 来源白名单为空")),
                "应提示来源为空：" + violations);
    }

    @Test
    @DisplayName("多个问题会同时被列出")
    void shouldReportAllViolations() {
        List<String> violations = StartupSecurityValidator.collectViolations(
                DEFAULT_JWT_SECRET, DEFAULT_DB_PASSWORD, Collections.singletonList("*"));

        assertTrue(violations.size() >= 3, "应同时报告多个问题：" + violations);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("生产环境使用弱配置时拒绝启动")
    void shouldRefuseToStartInProductionWithWeakConfig() {
        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setEnvironment("production");
        StartupSecurityValidator validator = new StartupSecurityValidator(properties);
        ReflectionTestUtils.setField(validator, "jwtSecret", DEFAULT_JWT_SECRET);
        ReflectionTestUtils.setField(validator, "datasourcePassword", DEFAULT_DB_PASSWORD);

        IllegalStateException exception = assertThrows(IllegalStateException.class, validator::validateOnStartup);

        assertTrue(exception.getMessage().contains("启动安全自检未通过"),
                "应抛出启动自检失败异常：" + exception.getMessage());
    }

    @Test
    @DisplayName("生产环境配置合规时允许启动")
    void shouldStartInProductionWithStrongConfig() {
        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setEnvironment("production");
        properties.setAllowedOrigins(REAL_ORIGINS);
        StartupSecurityValidator validator = new StartupSecurityValidator(properties);
        ReflectionTestUtils.setField(validator, "jwtSecret", STRONG_JWT_SECRET);
        ReflectionTestUtils.setField(validator, "datasourcePassword", "a-random-db-password");

        assertDoesNotThrow(validator::validateOnStartup);
    }

    @Test
    @DisplayName("本地环境使用弱配置时只告警不阻断")
    void shouldOnlyWarnInLocalEnvironment() {
        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setEnvironment("local");
        StartupSecurityValidator validator = new StartupSecurityValidator(properties);
        ReflectionTestUtils.setField(validator, "jwtSecret", DEFAULT_JWT_SECRET);
        ReflectionTestUtils.setField(validator, "datasourcePassword", DEFAULT_DB_PASSWORD);

        assertDoesNotThrow(validator::validateOnStartup);
    }
}
