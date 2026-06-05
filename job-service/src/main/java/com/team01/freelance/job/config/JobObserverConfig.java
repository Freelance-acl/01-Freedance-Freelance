package com.team01.freelance.job.config;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.job.observer.JobMongoEventLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class JobObserverConfig {

    @Bean
    EventSubject jobEventSubject(JobMongoEventLogger jobMongoEventLogger) {
        EventSubject subject = new EventSubject();
        subject.register(jobMongoEventLogger);
        return subject;
    }
}
