package com.team01.freelance.job.event;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface JobEventRepository extends MongoRepository<JobEvent, String> {
}