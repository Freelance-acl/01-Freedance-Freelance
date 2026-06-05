package com.team01.freelance.job.repository;

import com.team01.freelance.job.event.JobEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobEventRepository extends MongoRepository<JobEvent, String> {
}
