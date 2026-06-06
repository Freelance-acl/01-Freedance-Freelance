package com.team01.freelance.proposal.cache;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class ProposalCacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(ProposalCacheInvalidationService.class);

    private static final Set<String> NON_INVALIDATING_ACTIONS = Set.of(
            "ANALYTICS_VIEWED", "DASHBOARD_VIEWED");

    private static final String[] READ_FEATURE_WILDCARDS = {
            "proposal-service::S3-F1::*",
            "proposal-service::S3-F3::*",
            "proposal-service::S3-F5::*",
            "proposal-service::S3-F6::*",
            "proposal-service::S3-F9::*"
    };

    private final StringRedisTemplate redisTemplate;

    public ProposalCacheInvalidationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void invalidateProposalDetail(Long proposalId) {
        deleteKey(CacheKeyUtil.entityKey("proposal", proposalId));
    }

    public void invalidateAfterProposalWrite(Long proposalId, Long jobId) {
        invalidateProposalDetail(proposalId);
        invalidateReadFeatureCaches();
        invalidateRecommendations();
        if (jobId != null) {
            deleteByPattern("job-service::S2-F12::" + jobId);
        }
    }

    public void invalidateRecommendations() {
        deleteByPattern("proposal-service::S3-F12::*");
    }

    public void invalidateAllProposalCaches() {
        deleteByPattern("proposal-service::proposal::*");
        deleteByPattern("proposal-service::proposal-milestone::*");
        invalidateReadFeatureCaches();
        invalidateRecommendations();
    }

    public void invalidateOnObserverEvent(String action) {
        if (action == null || NON_INVALIDATING_ACTIONS.contains(action)) {
            return;
        }
        invalidateRecommendations();
    }

    private void invalidateReadFeatureCaches() {
        for (String pattern : READ_FEATURE_WILDCARDS) {
            deleteByPattern(pattern);
        }
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
