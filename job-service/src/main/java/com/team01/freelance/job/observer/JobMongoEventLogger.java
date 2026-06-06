package com.team01.freelance.job.observer;

import com.team01.freelance.common.observer.EntityObserver;
import com.team01.freelance.job.event.JobEvent;
import com.team01.freelance.job.repository.JobEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Profile("!test")
public class JobMongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(JobMongoEventLogger.class);

    private final JobEventRepository jobEventRepository;

    public JobMongoEventLogger(JobEventRepository jobEventRepository) {
        this.jobEventRepository = jobEventRepository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> details = toDetails(eventType, payload);
            Long jobId = toLong(details.get("jobId"));
            jobEventRepository.save(new JobEvent(eventType, jobId, LocalDateTime.now(), details));
        } catch (Exception ex) {
            log.warn("Failed to persist job event {}: {}", eventType, ex.getMessage());
        }
    }

    private Map<String, Object> toDetails(String eventType, Object payload) {
        Map<String, Object> details = new HashMap<>();
        if (payload instanceof Map<?, ?> map) {
            map.forEach((key, value) -> details.put(String.valueOf(key), value));
        }
        details.putIfAbsent("eventType", eventType);
        return details;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        return null;
    }
}
