package com.team01.freelance.proposal.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile("test")
public class InMemoryInteractionGraphService implements InteractionGraphService {

    private final Set<String> recorded = ConcurrentHashMap.newKeySet();
    private final Map<Long, Set<Long>> freelancerToJobs = new ConcurrentHashMap<>();

    @Override
    public boolean recordInteraction(
            Long proposalId,
            Long freelancerId,
            String freelancerName,
            Long jobId,
            String jobTitle,
            String jobCategory) {
        String key = edgeKey(freelancerId, jobId, proposalId);
        if (!recorded.add(key)) {
            return false;
        }
        freelancerToJobs.computeIfAbsent(freelancerId, ignored -> ConcurrentHashMap.newKeySet()).add(jobId);
        return true;
    }

    @Override
    public List<RecommendationScore> findRecommendedJobScores(Long freelancerId, int limit) {
        Set<Long> targetJobs = freelancerToJobs.getOrDefault(freelancerId, Set.of());
        if (targetJobs.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> jobScores = new HashMap<>();
        for (Map.Entry<Long, Set<Long>> entry : freelancerToJobs.entrySet()) {
            Long peerId = entry.getKey();
            if (peerId.equals(freelancerId)) {
                continue;
            }
            Set<Long> peerJobs = entry.getValue();
            boolean sharesJob = peerJobs.stream().anyMatch(targetJobs::contains);
            if (!sharesJob) {
                continue;
            }
            for (Long recommendedJob : peerJobs) {
                if (!targetJobs.contains(recommendedJob)) {
                    jobScores.merge(recommendedJob, 1L, Long::sum);
                }
            }
        }

        List<RecommendationScore> ranked = new ArrayList<>();
        jobScores.forEach((jobId, score) -> ranked.add(new RecommendationScore(jobId, score)));
        ranked.sort(Comparator.comparingLong(RecommendationScore::score).reversed());
        if (ranked.size() <= limit) {
            return ranked;
        }
        return ranked.subList(0, limit);
    }

    private static String edgeKey(Long freelancerId, Long jobId, Long proposalId) {
        return freelancerId + ":" + jobId + ":" + proposalId;
    }
}
