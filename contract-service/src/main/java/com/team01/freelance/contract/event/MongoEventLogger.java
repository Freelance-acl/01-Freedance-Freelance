package com.team01.freelance.contract.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;
    private final EventFactory eventFactory;

    public MongoEventLogger(
            ObjectProvider<MongoTemplate> mongoTemplateProvider,
            EventFactory eventFactory
    ) {
        this.mongoTemplateProvider = mongoTemplateProvider;
        this.eventFactory = eventFactory;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
        if (mongoTemplate == null) {
            return;
        }

        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("action", eventType);

            if (payload instanceof Map<?, ?> payloadMap) {
                payloadMap.forEach((key, value) -> params.put(String.valueOf(key), value));
            } else if (payload != null) {
                params.put("details", Map.of("payload", payload.toString()));
            }

            mongoTemplate.save(eventFactory.createEvent(EventType.CONTRACT, params), "contract_events");
        } catch (Exception e) {
            log.warn("Mongo event logging failed for action {}", eventType, e);
        }
    }
}
