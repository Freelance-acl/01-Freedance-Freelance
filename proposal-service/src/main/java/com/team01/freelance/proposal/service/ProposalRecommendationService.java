package com.team01.freelance.proposal.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.proposal.adapter.Neo4jRecordAdapter;
import com.team01.freelance.proposal.dto.FeignJobDTO;
import com.team01.freelance.proposal.dto.JobRecommendationDTO;
import com.team01.freelance.proposal.exception.ForbiddenOperationException;
import com.team01.freelance.proposal.feign.JobServiceClient;
import com.team01.freelance.proposal.feign.UserServiceClient;
import com.team01.freelance.proposal.graph.InteractionGraphService;
import com.team01.freelance.proposal.graph.RecommendationScore;
import com.team01.freelance.proposal.security.ProposalAuthSupport;
import com.team01.freelance.user.repository.UserRepository;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProposalRecommendationService {

    private static final int DEFAULT_LIMIT = 5;

    @Autowired
    private InteractionGraphService interactionGraphService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private UserServiceClient userServiceClient;

    @Autowired(required = false)
    private JobServiceClient jobServiceClient;

    @Autowired
    private ProposalAuthSupport proposalAuthSupport;

    @Autowired
    private Neo4jRecordAdapter neo4jRecordAdapter;

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
        if (userServiceClient == null) {
            if (!userRepository.existsById(freelancerId)) {
                throw new EntityNotFoundException("Freelancer not found with id: " + freelancerId);
            }
            return;
        }
        try {
            userServiceClient.getUser(freelancerId);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("Freelancer not found with id: " + freelancerId);
        } catch (FeignException e) {
            if (!userRepository.existsById(freelancerId)) {
                throw new EntityNotFoundException("Freelancer not found with id: " + freelancerId);
            }
        }
    }

    private Map<Long, FeignJobDTO> loadJobs(List<Long> jobIds) {
        if (jobServiceClient == null) {
            return jobRepository.findAllById(jobIds).stream()
                    .map(this::toFeignJobDTO)
                    .collect(Collectors.toMap(FeignJobDTO::getId, Function.identity()));
        }
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
            return jobRepository.findById(jobId)
                    .map(this::toFeignJobDTO)
                    .orElse(null);
        }
    }

    private FeignJobDTO toFeignJobDTO(Job job) {
        FeignJobDTO dto = new FeignJobDTO();
        dto.setId(job.getId());
        dto.setClientId(job.getClientId());
        dto.setTitle(job.getTitle());
        dto.setCategory(job.getCategory() == null ? null : job.getCategory().name());
        dto.setStatus(job.getStatus() == null ? null : job.getStatus().name());
        return dto;
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
