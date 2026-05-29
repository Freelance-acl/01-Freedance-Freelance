package com.team01.freelance.wallet.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@org.springframework.context.annotation.Profile("!test")
public class WalletCacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(WalletCacheInvalidationService.class);

    private final StringRedisTemplate redisTemplate;

    public WalletCacheInvalidationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
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

