package com.team01.freelance.proposal.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryInteractionGraphServiceTest {

    private InMemoryInteractionGraphService graph;

    @BeforeEach
    void setUp() {
        graph = new InMemoryInteractionGraphService();
    }

    @Test
    void recordInteraction_isIdempotentPerProposal() {
        assertTrue(graph.recordInteraction(1L, 100L, "A", 10L, "J1", "WEB_DEV"));
        assertFalse(graph.recordInteraction(1L, 100L, "A", 10L, "J1", "WEB_DEV"));
    }

    @Test
    void findRecommendedJobScores_ranksSharedPeerJobs() {
        graph.recordInteraction(1L, 1L, "A", 101L, "J1", "WEB_DEV");
        graph.recordInteraction(2L, 1L, "A", 102L, "J2", "WEB_DEV");
        graph.recordInteraction(3L, 2L, "B", 101L, "J1", "WEB_DEV");
        graph.recordInteraction(4L, 2L, "B", 103L, "J3", "WEB_DEV");
        graph.recordInteraction(5L, 3L, "C", 102L, "J2", "WEB_DEV");
        graph.recordInteraction(6L, 3L, "C", 104L, "J4", "WEB_DEV");

        var scores = graph.findRecommendedJobScores(1L, 10);

        assertEquals(2, scores.size());
        assertEquals(103L, scores.get(0).jobId());
        assertEquals(104L, scores.get(1).jobId());
    }
}
