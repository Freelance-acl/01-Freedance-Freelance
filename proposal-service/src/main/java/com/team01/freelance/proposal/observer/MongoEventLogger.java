package com.team01.freelance.proposal.observer;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.team01.freelance.common.event.EventType;
import com.team01.freelance.common.event.MongoEvent;
import com.team01.freelance.common.observer.EntityObserver;
import com.team01.freelance.proposal.adapter.MongoDocumentAdapter;
import com.team01.freelance.proposal.cache.ProposalCacheInvalidationService;
import com.team01.freelance.proposal.event.EventFactory;
import com.team01.freelance.proposal.event.ProposalEvent;
import com.team01.freelance.proposal.repository.ProposalEventRepository;

@Component
@Profile("!test")
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private final EventFactory eventFactory;
    private final MongoDocumentAdapter adapter;
    private final ProposalEventRepository proposalEventRepository;
    private final ProposalCacheInvalidationService cacheInvalidationService;

    public MongoEventLogger(
            EventFactory eventFactory,
            MongoDocumentAdapter adapter,
            ProposalEventRepository proposalEventRepository,
            ProposalCacheInvalidationService cacheInvalidationService) {
        this.eventFactory = eventFactory;
        this.adapter = adapter;
        this.proposalEventRepository = proposalEventRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> params = toParams(eventType, payload);
            MongoEvent event = eventFactory.createEvent(EventType.PROPOSAL, params);
            adapter.adapt((ProposalEvent) event);
            proposalEventRepository.save((ProposalEvent) event);
            cacheInvalidationService.invalidateOnObserverEvent(String.valueOf(params.get("action")));
        } catch (Exception ex) {
            log.warn("Failed to persist proposal event {}: {}", eventType, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toParams(String eventType, Object payload) {
        if (payload instanceof Map<?, ?> map) {
            Map<String, Object> params = new HashMap<>();
            map.forEach((k, v) -> params.put(String.valueOf(k), v));
            if (!params.containsKey("action")) {
                params.put("action", eventType);
            }
            return params;
        }
        throw new IllegalArgumentException("Proposal event payload must be a Map");
    }
}
