package com.team01.freelance.proposal.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@org.springframework.context.annotation.Profile("!test")
public class ProposalCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProposalCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProposalCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T getOrCompute(String key, Duration ttl, Class<T> type, Supplier<T> supplier) {
        if (redisTemplate == null) {
            return supplier.get();
        }
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        T value = supplier.get();
        put(key, ttl, value);
        return value;
    }

    public <T> T getOrCompute(String key, Duration ttl, TypeReference<T> typeRef, Supplier<T> supplier) {
        if (redisTemplate == null) {
            return supplier.get();
        }
        Optional<T> cached = get(key, typeRef);
        if (cached.isPresent()) {
            return cached.get();
        }
        T value = supplier.get();
        put(key, ttl, value);
        return value;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        if (redisTemplate == null) {
            return Optional.empty();
        }
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception ex) {
            log.warn("Redis cache read failed for {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    public <T> Optional<T> get(String key, TypeReference<T> typeRef) {
        if (redisTemplate == null) {
            return Optional.empty();
        }
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, typeRef));
        } catch (Exception ex) {
            log.warn("Redis cache read failed for {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(String key, Duration ttl, Object value) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException ex) {
            log.warn("Redis cache write serialization failed for {}: {}", key, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Redis cache write failed for {}: {}", key, ex.getMessage());
        }
    }
}
