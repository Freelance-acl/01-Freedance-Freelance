package com.team01.freelance.user.observer;

import com.team01.freelance.common.event.EventType;
import com.team01.freelance.common.event.MongoEvent;
import com.team01.freelance.common.observer.EntityObserver;
import com.team01.freelance.user.event.AuthEvent;
import com.team01.freelance.user.event.EventFactory;
import com.team01.freelance.user.repository.AuthEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * DP-2 Observer — persists auth events to MongoDB via {@link EventFactory}.
 * Soft dependency: Mongo failures are logged and never propagated.
 */
@Component
@org.springframework.context.annotation.Profile("!test")
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private final EventFactory eventFactory;
    private final AuthEventRepository authEventRepository;
    private final CacheInvalidationService cacheInvalidationService;

    public MongoEventLogger(
            EventFactory eventFactory,
            AuthEventRepository authEventRepository,
            CacheInvalidationService cacheInvalidationService) {
        this.eventFactory = eventFactory;
        this.authEventRepository = authEventRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> params = toParams(eventType, payload);
            MongoEvent event = eventFactory.createEvent(EventType.AUTH, params);
            authEventRepository.save((AuthEvent) event);
            invalidateCaches(eventType, params);
        } catch (Exception ex) {
            log.warn("Failed to persist auth event {}: {}", eventType, ex.getMessage());
        }
    }

    private void invalidateCaches(String eventType, Map<String, Object> params) {
        if ("ROLE_CHANGED".equals(eventType)) {
            Long userId = ((Number) params.get("userId")).longValue();
            cacheInvalidationService.invalidateUserDetail(userId);
            cacheInvalidationService.invalidateUserActivityFeed(userId);
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
        throw new IllegalArgumentException("Auth event payload must be a Map");
    }
}
