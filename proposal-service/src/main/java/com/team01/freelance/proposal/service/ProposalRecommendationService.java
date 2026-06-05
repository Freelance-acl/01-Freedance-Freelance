package com.team01.freelance.proposal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.proposal.dto.JobRecommendationDTO;
import com.team01.freelance.proposal.exception.ForbiddenOperationException;
import com.team01.freelance.proposal.graph.InteractionGraphService;
import com.team01.freelance.proposal.graph.RecommendationScore;
import com.team01.freelance.proposal.security.ProposalAuthSupport;
import com.team01.freelance.user.repository.UserRepository;

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

    @Autowired
    private ProposalAuthSupport proposalAuthSupport;

    @Cacheable(cacheNames = "S3-F12", key = "#freelancerId + ':' + #limit + ':' + #request.getHeader('Authorization')")
    public List<JobRecommendationDTO> getRecommendations(
            Long freelancerId,
            Integer limit,
            HttpServletRequest request) {
        assertAuthorized(freelancerId, request);
        if (!userRepository.existsById(freelancerId)) {
            throw new EntityNotFoundException("Freelancer not found with id: " + freelancerId);
        }

        int effectiveLimit = normalizeLimit(limit);
        List<RecommendationScore> scores = interactionGraphService.findRecommendedJobScores(freelancerId, effectiveLimit);
        if (scores.isEmpty()) {
            return List.of();
        }

        List<Long> jobIds = scores.stream().map(RecommendationScore::jobId).toList();
        Map<Long, Job> jobsById = jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));

        List<JobRecommendationDTO> results = new ArrayList<>();
        for (RecommendationScore score : scores) {
            Job job = jobsById.get(score.jobId());
            if (job == null) {
                continue;
            }
            JobRecommendationDTO dto = new JobRecommendationDTO();
            dto.setJobId(job.getId());
            dto.setTitle(job.getTitle());
            dto.setCategory(job.getCategory() != null ? job.getCategory().name() : null);
            dto.setScore(score.score());
            results.add(dto);
        }
        return results;
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
