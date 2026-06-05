package com.team01.freelance.proposal.graph;

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

    @Override
    public boolean recordInteraction(
            Long proposalId,
            Long freelancerId,
            String freelancerName,
            Long jobId,
            String jobTitle,
            String jobCategory) {
        String key = edgeKey(freelancerId, jobId, proposalId);
        return recorded.add(key);
    }

    /** Test helper: distinct proposal ids recorded for a freelancer–job pair. */
    public int recordedProposalCount(Long freelancerId, Long jobId) {
        String prefix = freelancerId + ":" + jobId + ":";
        return (int) recorded.stream().filter(key -> key.startsWith(prefix)).count();
    }

    private static String edgeKey(Long freelancerId, Long jobId, Long proposalId) {
        return freelancerId + ":" + jobId + ":" + proposalId;
    }
}
