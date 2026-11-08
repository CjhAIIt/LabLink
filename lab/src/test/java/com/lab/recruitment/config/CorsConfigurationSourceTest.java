package com.lab.recruitment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 跨域来源白名单的单元测试。
 *
 * <p>这些用例守住的是「本地开发端口不固定也能写接口」这条底线：
 * 浏览器对同源 POST 同样会带上 Origin 头，开发代理把该头转发到后端后
 * 就会命中白名单校验，登录、发布公告这类写操作会全部被拦成
 * {@code 403 Invalid CORS request}。同时生产环境不得因此放宽。</p>
 */
class CorsConfigurationSourceTest {

    private CorsConfiguration configurationFor(String environment, List<String> origins) {
        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setEnvironment(environment);
        properties.setAllowedOrigins(origins);

        SecurityConfig securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "securityProperties", properties);

        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();
        return source.getCorsConfigurations().get("/**");
    }

    @Test
    @DisplayName("本地环境放行回环地址的任意端口")
    void shouldAllowAnyLoopbackPortInLocalEnvironment() {
        CorsConfiguration configuration =
                configurationFor("local", Collections.singletonList("http://localhost:5173"));

        assertNotNull(configuration.checkOrigin("http://127.0.0.1:5193"),
                "开发服务器端口顺延后仍应放行");
        assertNotNull(configuration.checkOrigin("http://localhost:5192"),
                "localhost 的任意端口都应放行");
    }

    @Test
    @DisplayName("本地环境仍然拒绝陌生来源")
    void shouldStillRejectUnknownOriginInLocalEnvironment() {
        CorsConfiguration configuration =
                configurationFor("local", Collections.singletonList("http://localhost:5173"));

        assertNull(configuration.checkOrigin("https://evil.example.com"),
                "非回环的陌生来源不应被放行");
    }

    @Test
    @DisplayName("生产环境不放宽回环端口")
    void shouldNotRelaxLoopbackPortsInProduction() {
        CorsConfiguration configuration =
                configurationFor("production", Collections.singletonList("https://lab.example.edu.cn"));

        assertNull(configuration.checkOrigin("http://127.0.0.1:5193"),
                "生产环境不应放行回环地址");
        assertNotNull(configuration.checkOrigin("https://lab.example.edu.cn"),
                "生产环境白名单内的来源应当放行");
    }
}
