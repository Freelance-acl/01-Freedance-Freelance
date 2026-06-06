package com.team01.freelance.proposal.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableCaching
@Profile("test")
public class TestCacheConfig {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
