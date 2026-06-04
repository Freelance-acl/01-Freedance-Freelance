package com.team01.freelance.job.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import java.util.List;

@Configuration
@EnableCaching
public class JobCacheConfig {
	@Bean
	public CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(List.of(new ConcurrentMapCache("jobDashboard")));
		return manager;
	}
}