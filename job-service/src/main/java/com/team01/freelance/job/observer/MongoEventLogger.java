package com.team01.freelance.job.observer;

import com.team01.freelance.common.observer.EntityObserver;
import com.team01.freelance.job.audit.JobEventAudit;
import com.team01.freelance.job.audit.JobEventAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("!test")
public class MongoEventLogger implements EntityObserver {

    @Autowired
    private JobEventAuditRepository jobEventAuditRepository;

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> payloadMap = (payload instanceof Map) ? (Map<String, Object>) payload : Map.of();
            JobEventAudit auditEvent = new JobEventAudit(eventType, payloadMap);
            jobEventAuditRepository.save(auditEvent);
        } catch (Exception e) {
            // Log to Mongo but don't fail the operation
            System.err.println("Failed to log event to MongoDB: " + e.getMessage());
        }
    }
}
