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
        String key = freelancerId + ":" + jobId + ":" + proposalId;
        return recorded.add(key);
    }
}
