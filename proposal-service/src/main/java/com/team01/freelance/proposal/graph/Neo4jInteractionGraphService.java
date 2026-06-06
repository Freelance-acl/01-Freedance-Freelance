package com.team01.freelance.proposal.graph;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class Neo4jInteractionGraphService implements InteractionGraphService {

    private final Neo4jClient neo4jClient;

    public Neo4jInteractionGraphService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @Override
    public boolean recordInteraction(
            Long proposalId,
            Long freelancerId,
            String freelancerName,
            Long jobId,
            String jobTitle,
            String jobCategory) {
        Boolean alreadyRecorded = neo4jClient.query("""
                        MATCH (f:Freelancer {userId: $freelancerId})-[r:PROPOSED_TO]->(j:Job {jobId: $jobId})
                        WHERE $proposalId IN coalesce(r.recordedProposalIds, [])
                        RETURN true AS recorded
                        """)
                .bindAll(Map.of(
                        "freelancerId", freelancerId,
                        "jobId", jobId,
                        "proposalId", proposalId))
                .fetchAs(Boolean.class)
                .first()
                .orElse(false);

        if (Boolean.TRUE.equals(alreadyRecorded)) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        neo4jClient.query("""
                        MERGE (f:Freelancer {userId: $freelancerId})
                        ON CREATE SET f.name = $freelancerName
                        MERGE (j:Job {jobId: $jobId})
                        ON CREATE SET j.title = $jobTitle, j.category = $jobCategory
                        MERGE (f)-[r:PROPOSED_TO]->(j)
                        ON CREATE SET r.proposalCount = 1, r.lastProposalDate = $now,
                            r.recordedProposalIds = [$proposalId]
                        ON MATCH SET r.proposalCount = coalesce(r.proposalCount, 0) + 1,
                            r.lastProposalDate = $now,
                            r.recordedProposalIds = coalesce(r.recordedProposalIds, []) + $proposalId
                        """)
                .bindAll(Map.of(
                        "freelancerId", freelancerId,
                        "freelancerName", freelancerName,
                        "jobId", jobId,
                        "jobTitle", jobTitle,
                        "jobCategory", jobCategory,
                        "proposalId", proposalId,
                        "now", now))
                .run();
        return true;
    }

    @Override
    public List<RecommendationScore> findRecommendedJobScores(Long freelancerId, int limit) {
        return neo4jClient.query("""
                        MATCH (target:Freelancer {userId: $freelancerId})-[:PROPOSED_TO]->(shared:Job)
                              <-[:PROPOSED_TO]-(peer:Freelancer)
                        WHERE peer.userId <> $freelancerId
                        MATCH (peer)-[:PROPOSED_TO]->(rec:Job)
                        WHERE NOT EXISTS { (target)-[:PROPOSED_TO]->(rec) }
                        RETURN rec.jobId AS jobId, count(peer) AS score
                        ORDER BY score DESC
                        LIMIT $limit
                        """)
                .bindAll(Map.of("freelancerId", freelancerId, "limit", limit))
                .fetch()
                .all()
                .stream()
                .map(row -> new RecommendationScore(
                        ((Number) row.get("jobId")).longValue(),
                        ((Number) row.get("score")).longValue()))
                .toList();
    }
}
