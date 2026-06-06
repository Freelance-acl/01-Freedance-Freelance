package com.team01.freelance.proposal.graph;

import java.util.List;

public interface InteractionGraphService {

    /**
     * @return true if the graph was mutated; false when proposalId was already recorded (idempotent)
     */
    boolean recordInteraction(
            Long proposalId,
            Long freelancerId,
            String freelancerName,
            Long jobId,
            String jobTitle,
            String jobCategory);

    List<RecommendationScore> findRecommendedJobScores(Long freelancerId, int limit);
}
