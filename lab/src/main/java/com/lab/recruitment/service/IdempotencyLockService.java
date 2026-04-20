package com.lab.recruitment.service;

import com.lab.recruitment.config.PlatformCacheProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyLockService {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final PlatformCacheProperties cacheProperties;
    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;
    private final Map<String, LocalLockRecord> localLocks = new ConcurrentHashMap<>();

    public IdempotencyLockService(PlatformCacheProperties cacheProperties,
                                  ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider) {
        this.cacheProperties = cacheProperties;
        this.stringRedisTemplateProvider = stringRedisTemplateProvider;
    }

    public String tryLock(String key, Duration ttl) {
        if (key == null || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return null;
        }

        String token = UUID.randomUUID().toString();
        if (cacheProperties.getRedis().isEnabled()) {
            StringRedisTemplate template = stringRedisTemplateProvider.getIfAvailable();
            if (template != null) {
                try {
                    Boolean success = template.opsForValue().setIfAbsent(key, token, ttl);
                    if (Boolean.TRUE.equals(success)) {
                        return token;
                    }
                    return null;
                } catch (RuntimeException ignored) {
                    // Fall back to local lock if Redis is temporarily unavailable.
                }
            }
        }

        cleanupExpiredLocalLocks();
        Instant expiresAt = Instant.now().plus(ttl);
        LocalLockRecord existing = localLocks.putIfAbsent(key, new LocalLockRecord(token, expiresAt));
        return existing == null ? token : null;
    }

    public void unlock(String key, String token) {
        if (key == null || token == null) {
            return;
        }

        if (cacheProperties.getRedis().isEnabled()) {
            StringRedisTemplate template = stringRedisTemplateProvider.getIfAvailable();
            if (template != null) {
                try {
                    template.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
                    return;
                } catch (RuntimeException ignored) {
                    // Fall back to local unlock path.
                }
            }
        }

        localLocks.computeIfPresent(key, (currentKey, record) ->
                token.equals(record.token) ? null : record
        );
    }

    private void cleanupExpiredLocalLocks() {
        Instant now = Instant.now();
        localLocks.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
    }

    private static final class LocalLockRecord {

        private final String token;
        private final Instant expiresAt;

        private LocalLockRecord(String token, Instant expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }
    }
}
