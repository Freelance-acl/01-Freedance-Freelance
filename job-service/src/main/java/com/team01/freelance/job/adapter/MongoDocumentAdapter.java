package com.team01.freelance.job.adapter;

import com.team01.freelance.job.dto.JobEventDTO;
import com.team01.freelance.job.event.JobEvent;
import org.springframework.stereotype.Component;

@Component
public class MongoDocumentAdapter {

    public JobEventDTO adapt(JobEvent event) {
        return new JobEventDTO(event.getAction(), event.getJobId(), event.getTimestamp(), event.getDetails());
    }
}