package com.lab.recruitment.service;

import com.lab.recruitment.config.PlatformCacheNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class PlatformCacheService {

    @Autowired
    private CacheManager cacheManager;

    public void evictUserAuthCache(Long userId) {
        if (userId == null) {
            return;
        }
        evict(PlatformCacheNames.AUTH_MENU, userId);
        evict(PlatformCacheNames.AUTH_PERMISSION, userId);
    }

    public void evictLabDetailCache(Long labId) {
        if (labId == null) {
            return;
        }
        evict(PlatformCacheNames.LAB_DETAIL, labId);
    }

    public void evictAllStatisticsCaches() {
        clear(PlatformCacheNames.STAT_DASHBOARD);
        clear(PlatformCacheNames.STAT_LABS);
        clear(PlatformCacheNames.STAT_MEMBERS);
        clear(PlatformCacheNames.STAT_ATTENDANCE);
        clear(PlatformCacheNames.STAT_DEVICES);
        clear(PlatformCacheNames.STAT_PROFILES);
    }

    public void evictSearchCaches() {
        clear(PlatformCacheNames.SEARCH_GLOBAL);
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
