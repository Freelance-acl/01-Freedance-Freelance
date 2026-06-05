package com.team01.freelance.proposal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team01.freelance.proposal.cache.ProposalCacheInvalidationService;
import com.team01.freelance.proposal.cache.ProposalCacheService;

@Configuration
@Profile("test")
public class TestCacheConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    ProposalCacheService proposalCacheService(ObjectMapper objectMapper) {
        return new ProposalCacheService(null, objectMapper);
    }

    @Bean
    ProposalCacheInvalidationService proposalCacheInvalidationService() {
        return new ProposalCacheInvalidationService(null);
    }
}
