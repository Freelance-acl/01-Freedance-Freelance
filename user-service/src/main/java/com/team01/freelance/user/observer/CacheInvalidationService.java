package com.team01.freelance.user.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * §4.4.4 / CC-2 — wildcard Redis cache invalidation for user-service keys.
 */
@Service
@org.springframework.context.annotation.Profile("!test")
public class CacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationService.class);

    private final StringRedisTemplate redisTemplate;

    public CacheInvalidationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void invalidateUserDetail(Long userId) {
        deleteKey("user-service::user::" + userId);
    }

    public void invalidateUserActivityFeed(Long userId) {
        deleteByPattern("user-service::S1-F12::" + userId + "::*");
    }

    public void invalidateWalletAnalytics() {
        deleteByPattern("wallet-service::S5-F10::*");
        deleteByPattern("wallet-service::S5-F11::*");
    }

    public void invalidateWalletPayoutDetail(Long payoutId) {
        deleteKey("wallet-service::payout::" + payoutId);
    }

    private void deleteKey(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("Redis delete failed for key {}: {}", key, ex.getMessage());
        }
    }

    private void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("Redis pattern delete failed for {}: {}", pattern, ex.getMessage());
        }
    }
}

