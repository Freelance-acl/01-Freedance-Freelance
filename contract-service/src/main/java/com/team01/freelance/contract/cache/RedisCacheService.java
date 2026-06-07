package com.team01.freelance.contract.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    public RedisCacheService(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper
    ) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    public <T> T getOrCompute(String key, Class<T> type, Duration ttl, Supplier<T> supplier) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                String cachedValue = redisTemplate.opsForValue().get(key);
                if (cachedValue != null && !cachedValue.isBlank()) {
                    return objectMapper.readValue(cachedValue, type);
                }
            } catch (Exception e) {
                log.warn("Redis read failed for key {}", key, e);
            }
        }

        T computedValue = supplier.get();
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(computedValue), ttl);
            } catch (Exception e) {
                log.warn("Redis write failed for key {}", key, e);
            }
        }
        return computedValue;
    }

    public void evictByPrefix(String prefix) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis eviction failed for prefix {}", prefix, e);
        }
    }
}
