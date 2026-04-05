package com.lab.recruitment.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 负责JWT令牌的生成、解析和验证
 * 使用HMAC-SHA256算法进行签名
 * 配置通过application.yml中的jwt前缀属性注入
 */
@Component
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtUtils {

    /**
     * JWT密钥，用于签名和验证令牌
     */
    private String secret;

    /**
     * 令牌过期时间（毫秒）
     */
    private Long expiration;

    /**
     * 获取签名密钥
     * 将字符串密钥转换为SecretKey对象
     * @return SecretKey实例
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成JWT令牌
     * 将用户名和角色信息封装到令牌中
     * @param username 用户名
     * @param role 用户角色
     * @return JWT令牌字符串
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }

    /**
     * 创建JWT令牌
     * 设置令牌的声明、主题、签发时间和过期时间
     * @param claims 自定义声明
     * @param subject 主题（用户名）
     * @return JWT令牌字符串
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析JWT令牌
     * 从令牌中提取所有声明信息
     * @param token JWT令牌字符串
     * @return Claims对象，包含令牌的所有声明
     */
    public Claims parseToken(String token) {
        return getAllClaimsFromToken(token);
    }

    /**
     * 验证JWT令牌
     * 检查令牌的用户名是否匹配且未过期
     * @param token JWT令牌字符串
     * @param username 期望的用户名
     * @return 令牌是否有效
     */
    public Boolean validateToken(String token, String username) {
        final String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    /**
     * 从令牌中获取用户名
     * @param token JWT令牌字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从令牌中获取用户角色
     * @param token JWT令牌字符串
     * @return 用户角色
     */
    public String getRoleFromToken(String token) {
        final Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("role");
    }

    /**
     * 从令牌中获取过期时间
     * @param token JWT令牌字符串
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从令牌中获取指定声明
     * 使用函数式接口提取声明信息
     * @param token JWT令牌字符串
     * @param claimsResolver 声明解析函数
     * @param <T> 返回值类型
     * @return 声明值
     */
    public <T> T getClaimFromToken(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从令牌中获取所有声明
     * 使用签名密钥解析令牌
     * @param token JWT令牌字符串
     * @return Claims对象，包含所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 检查令牌是否已过期
     * @param token JWT令牌字符串
     * @return 令牌是否已过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
}