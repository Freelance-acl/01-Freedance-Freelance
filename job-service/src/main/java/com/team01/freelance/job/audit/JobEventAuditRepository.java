package com.team01.freelance.job.audit;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * MongoDB repository for JobEventAudit.
 * Persists and retrieves job event audit records.
 */
@Repository
public interface JobEventAuditRepository extends MongoRepository<JobEventAudit, String> {
}
