package com.team01.freelance.proposal.observer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.proposal.adapter.MongoDocumentAdapter;
import com.team01.freelance.proposal.cache.ProposalCacheInvalidationService;
import com.team01.freelance.proposal.event.EventFactory;
import com.team01.freelance.proposal.repository.ProposalEventRepository;

@ExtendWith(MockitoExtension.class)
class MongoEventLoggerTest {

    @Mock
    private ProposalEventRepository proposalEventRepository;

    @Mock
    private ProposalCacheInvalidationService cacheInvalidationService;

    private final EventFactory eventFactory = new EventFactory();
    private final MongoDocumentAdapter adapter = new MongoDocumentAdapter();
    private final EventSubject eventSubject = new EventSubject();
    private MongoEventLogger mongoEventLogger;

    @BeforeEach
    void setUp() {
        mongoEventLogger = new MongoEventLogger(
                eventFactory, adapter, proposalEventRepository, cacheInvalidationService);
        eventSubject.register(mongoEventLogger);
    }

    @Test
    void unregisterObserver_skipsMongoPersistence() {
        eventSubject.unregister(mongoEventLogger);

        eventSubject.notifyObservers(
                "PROPOSAL_CREATED",
                Map.of(
                        "proposalId", 1L,
                        "action", "PROPOSAL_CREATED",
                        "details", Map.of("jobId", 2L, "freelancerId", 3L)));

        verify(proposalEventRepository, never()).save(any());
    }
}
