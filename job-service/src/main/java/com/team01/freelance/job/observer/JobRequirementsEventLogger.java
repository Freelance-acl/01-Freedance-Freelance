package com.team01.freelance.job.observer;

import com.team01.freelance.common.observer.EntityObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class JobRequirementsEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(JobRequirementsEventLogger.class);

    @Override
    public void onEvent(String eventType, Object payload) {
        log.info("Job event observed: eventType={}, payload={}", eventType, payload);
    }
}
