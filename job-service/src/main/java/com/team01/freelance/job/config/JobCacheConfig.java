package com.team01.freelance.job.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.concurrent.TimeUnit;


/**
 * Configures the job dashboard cache.
 */
@Configuration
@EnableCaching
public class JobCacheConfig {
	/**
	 * Creates the cache manager used for job dashboard results.
	 */
	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager manager = new CaffeineCacheManager("jobDashboard");
		manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES));
		return manager;
	}
}