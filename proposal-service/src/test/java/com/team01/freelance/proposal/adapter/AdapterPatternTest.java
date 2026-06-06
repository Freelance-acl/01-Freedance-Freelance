package com.team01.freelance.proposal.adapter;

import com.team01.freelance.proposal.dto.JobRecommendationDTO;
import com.team01.freelance.proposal.dto.ProposalEventDTO;
import com.team01.freelance.proposal.event.ProposalEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdapterPatternTest {

    @Test
    void proposalServiceAdaptersExistWithAdaptMethod() throws Exception {
        assertAdaptMethod(MongoDocumentAdapter.class, ProposalEventDTO.class);
        assertAdaptMethod(Neo4jRecordAdapter.class, JobRecommendationDTO.class);
    }

    @Test
    void mongoDocumentAdapterMapsProposalEvent() {
        ProposalEvent event = new ProposalEvent(
                42L,
                "PROPOSAL_CREATED",
                LocalDateTime.of(2026, 3, 1, 12, 0),
                Map.of("jobId", 7L));

        ProposalEventDTO dto = new MongoDocumentAdapter().adapt(event);

        assertEquals(42L, dto.proposalId());
        assertEquals("PROPOSAL_CREATED", dto.action());
        assertEquals(LocalDateTime.of(2026, 3, 1, 12, 0), dto.timestamp());
        assertEquals(7L, dto.details().get("jobId"));
    }

    @Test
    void neo4jRecordAdapterMapsScoreRow() {
        JobRecommendationDTO dto = new Neo4jRecordAdapter().adapt(Map.of(
                "jobId", 3L,
                "title", "Neo Job",
                "category", "WEB_DEV",
                "score", 2L));

        assertEquals(3L, dto.getJobId());
        assertEquals("Neo Job", dto.getTitle());
        assertEquals("WEB_DEV", dto.getCategory());
        assertEquals(2L, dto.getScore());
    }

    private static void assertAdaptMethod(Class<?> adapterClass, Class<?> expectedReturn) throws Exception {
        assertNotNull(adapterClass.getDeclaredMethod("adapt", findAdaptParameter(adapterClass)));
        Method adapt = adapterClass.getDeclaredMethod("adapt", findAdaptParameter(adapterClass));
        assertEquals(expectedReturn, adapt.getReturnType());
        assertTrue(java.lang.reflect.Modifier.isPublic(adapt.getModifiers()));
    }

    private static Class<?> findAdaptParameter(Class<?> adapterClass) throws NoSuchMethodException {
        for (Method method : adapterClass.getDeclaredMethods()) {
            if ("adapt".equals(method.getName()) && method.getParameterCount() == 1) {
                return method.getParameterTypes()[0];
            }
        }
        throw new NoSuchMethodException("adapt");
    }
}
