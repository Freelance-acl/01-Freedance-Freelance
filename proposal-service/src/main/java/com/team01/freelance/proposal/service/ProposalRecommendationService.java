package com.team01.freelance.proposal.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import com.team01.freelance.proposal.adapter.Neo4jRecordAdapter;
import com.team01.freelance.proposal.dto.FeignJobDTO;
import com.team01.freelance.proposal.dto.JobRecommendationDTO;
import com.team01.freelance.proposal.exception.ForbiddenOperationException;
import com.team01.freelance.proposal.feign.JobServiceClient;
import com.team01.freelance.proposal.feign.UserServiceClient;
import com.team01.freelance.proposal.graph.InteractionGraphService;
import com.team01.freelance.proposal.graph.RecommendationScore;
import com.team01.freelance.proposal.security.ProposalAuthSupport;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProposalRecommendationService {

    private static final int DEFAULT_LIMIT = 5;

    private final InteractionGraphService interactionGraphService;
    private final UserServiceClient userServiceClient;
    private final JobServiceClient jobServiceClient;
    private final ProposalAuthSupport proposalAuthSupport;
    private final Neo4jRecordAdapter neo4jRecordAdapter;

    public ProposalRecommendationService(
            InteractionGraphService interactionGraphService,
            UserServiceClient userServiceClient,
            JobServiceClient jobServiceClient,
            ProposalAuthSupport proposalAuthSupport,
            Neo4jRecordAdapter neo4jRecordAdapter) {
        this.interactionGraphService = interactionGraphService;
        this.userServiceClient = userServiceClient;
        this.jobServiceClient = jobServiceClient;
        this.proposalAuthSupport = proposalAuthSupport;
        this.neo4jRecordAdapter = neo4jRecordAdapter;
    }

    @Cacheable(cacheNames = "S3-F12", key = "#freelancerId + ':' + #limit + ':' + #request.getHeader('Authorization')")
    public List<JobRecommendationDTO> getRecommendations(
            Long freelancerId,
            Integer limit,
            HttpServletRequest request) {
        assertAuthorized(freelancerId, request);
        assertFreelancerExists(freelancerId);

        int effectiveLimit = normalizeLimit(limit);
        List<RecommendationScore> scores = interactionGraphService.findRecommendedJobScores(freelancerId, effectiveLimit);
        if (scores.isEmpty()) {
            return List.of();
        }

        Map<Long, FeignJobDTO> jobsById = loadJobs(scores.stream().map(RecommendationScore::jobId).toList());

        List<JobRecommendationDTO> results = new ArrayList<>();
        for (RecommendationScore score : scores) {
            FeignJobDTO job = jobsById.get(score.jobId());
            if (job == null) {
                continue;
            }
            Map<String, Object> record = new HashMap<>();
            record.put("jobId", job.getId());
            record.put("title", job.getTitle());
            record.put("category", job.getCategory());
            record.put("score", score.score());
            results.add(neo4jRecordAdapter.adapt(record));
        }
        return results;
    }

    private void assertFreelancerExists(Long freelancerId) {
        try {
            userServiceClient.getUser(freelancerId);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("Freelancer not found with id: " + freelancerId);
        } catch (FeignException e) {
            throw new IllegalStateException("User service temporarily unavailable");
        }
    }

    private Map<Long, FeignJobDTO> loadJobs(List<Long> jobIds) {
        return jobIds.stream()
                .map(this::getJob)
                .filter(job -> job != null && job.getId() != null)
                .collect(Collectors.toMap(FeignJobDTO::getId, Function.identity(), (left, right) -> left));
    }

    private FeignJobDTO getJob(Long jobId) {
        try {
            return jobServiceClient.getJob(jobId);
        } catch (FeignException.NotFound e) {
            return null;
        } catch (FeignException e) {
            throw new IllegalStateException("Job service temporarily unavailable");
        }
    }

    private void assertAuthorized(Long freelancerId, HttpServletRequest request) {
        Long uid = proposalAuthSupport.extractUid(request);
        String role = proposalAuthSupport.extractRole(request);
        if (uid == null) {
            throw new ForbiddenOperationException("Missing authenticated user");
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        if (!uid.equals(freelancerId)) {
            throw new ForbiddenOperationException("Not authorized to view recommendations for this freelancer");
        }
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return limit;
    }
}
