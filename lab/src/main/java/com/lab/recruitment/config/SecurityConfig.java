package com.lab.recruitment.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置类
 * 负责配置应用的安全策略，包括：
 * - JWT认证过滤器
 * - 跨域资源共享（CORS）
 * - 请求授权规则
 * - 密码加密方式
 * - 会话管理策略
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * JWT认证过滤器，用于拦截请求并验证JWT令牌
     */
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * JWT认证入口点，处理认证失败的情况
     */
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 密码编码器Bean
     * 使用BCrypt算法对密码进行加密
     * @return BCryptPasswordEncoder实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器Bean
     * 用于处理用户认证
     * @param config 认证配置
     * @return AuthenticationManager实例
     * @throws Exception 配置异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 安全过滤器链Bean
     * 配置HTTP安全策略，包括：
     * - 启用CORS并禁用CSRF
     * - 配置JWT认证入口点
     * - 设置无状态会话管理
     * - 配置请求授权规则
     * - 添加JWT认证过滤器
     * @param http HttpSecurity对象
     * @return SecurityFilterChain实例
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
            .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authorizeRequests()
            // 公开访问的端点（无需认证）
            .antMatchers(
                    "/auth/login",
                    "/auth/register",
                    "/auth/register/send-code",
                    "/auth/teacher-register",
                    "/auth/teacher-register/send-code",
                    "/auth/password-reset/send-code",
                    "/auth/password-reset/confirm",
                    "/user/login",
                    "/user/register",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/register/send-code",
                    "/api/auth/teacher-register",
                    "/api/auth/teacher-register/send-code",
                    "/api/auth/password-reset/send-code",
                    "/api/auth/password-reset/confirm",
                    "/api/user/login",
                    "/api/user/register"
            ).permitAll()
            .antMatchers("/file/upload", "/file/view").permitAll()
            .antMatchers("/uploads/**").permitAll()
            .antMatchers(HttpMethod.GET, "/labs/list", "/api/labs/list").permitAll()
            .antMatchers(HttpMethod.GET, "/labs/*", "/api/labs/*").permitAll()
            .antMatchers(HttpMethod.GET, "/labs/detail/*", "/api/labs/detail/*").permitAll()
            .antMatchers(HttpMethod.GET, "/graduate/list", "/api/graduate/list").permitAll()
            .antMatchers("/colleges/options").permitAll()
            // 总负责人权限的端点
            .antMatchers("/user/admin/**").hasAuthority("ROLE_SUPER_ADMIN")
            .antMatchers("/user/student/list").hasAuthority("ROLE_SUPER_ADMIN")
            .antMatchers("/labs/list-with-admin").hasAuthority("ROLE_SUPER_ADMIN")
            // 管理员权限的端点（包括总负责人）
            .antMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")
            // 其他请求需要认证
            .anyRequest().authenticated();

        // 在用户名密码认证过滤器之前添加JWT认证过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS配置源Bean
     * 配置跨域资源共享策略，允许前端应用访问后端API
     * @return CorsConfigurationSource实例
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
