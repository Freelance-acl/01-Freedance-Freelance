package com.team01.freelance.job.observer;

import com.team01.freelance.common.observer.EntityObserver;
import com.team01.freelance.job.audit.JobEventAudit;
import com.team01.freelance.job.audit.JobEventAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
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
            Map<String, Object> params = toParams(eventType, payload);
            MongoEvent event = eventFactory.createEvent(EventType.JOB, params);
            if (event instanceof JobEvent jobEvent) {
                jobEventRepository.save(jobEvent);
            } else {
                log.warn("Skipping non-JobEvent from factory: action={}, class={}",
                        event.getAction(), event.getClass().getName());
            }
        } catch (Exception ex) {
            log.warn("Failed to persist job event {}: {}", eventType, ex.getMessage(), ex);
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
        throw new IllegalArgumentException("Job event payload must be a Map");
    }
}
