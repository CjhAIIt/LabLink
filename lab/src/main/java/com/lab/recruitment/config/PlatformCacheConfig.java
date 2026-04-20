package com.lab.recruitment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@EnableConfigurationProperties(PlatformCacheProperties.class)
public class PlatformCacheConfig {

    @Bean
    @ConditionalOnProperty(prefix = "lablink.cache.redis", name = "enabled", havingValue = "true")
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(PlatformCacheNames.LAB_DETAIL, baseConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(PlatformCacheNames.AUTH_MENU, baseConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(PlatformCacheNames.AUTH_PERMISSION, baseConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(PlatformCacheNames.STAT_DASHBOARD, baseConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(PlatformCacheNames.STAT_LABS, baseConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(PlatformCacheNames.STAT_MEMBERS, baseConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(PlatformCacheNames.STAT_ATTENDANCE, baseConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(PlatformCacheNames.STAT_DEVICES, baseConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(PlatformCacheNames.STAT_PROFILES, baseConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(PlatformCacheNames.SEARCH_GLOBAL, baseConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(baseConfig.entryTtl(Duration.ofMinutes(30)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager localCacheManager() {
        return new ConcurrentMapCacheManager(
                PlatformCacheNames.LAB_DETAIL,
                PlatformCacheNames.AUTH_MENU,
                PlatformCacheNames.AUTH_PERMISSION,
                PlatformCacheNames.STAT_DASHBOARD,
                PlatformCacheNames.STAT_LABS,
                PlatformCacheNames.STAT_MEMBERS,
                PlatformCacheNames.STAT_ATTENDANCE,
                PlatformCacheNames.STAT_DEVICES,
                PlatformCacheNames.STAT_PROFILES,
                PlatformCacheNames.SEARCH_GLOBAL
        );
    }
}
