package com.team01.freelance.job.observer;

import com.team01.freelance.common.observer.EntityObserver;
import com.team01.freelance.job.event.EventFactory;
import com.team01.freelance.job.event.JobEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private final EventFactory eventFactory;
    private final JobEventRepository jobEventRepository;

    public MongoEventLogger(EventFactory eventFactory, JobEventRepository jobEventRepository) {
        this.eventFactory = eventFactory;
        this.jobEventRepository = jobEventRepository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            jobEventRepository.save(eventFactory.createJobEvent(eventType, payload));
        } catch (Exception ex) {
            log.warn("Failed to persist job event {}: {}", eventType, ex.getMessage(), ex);
        }
    }
}