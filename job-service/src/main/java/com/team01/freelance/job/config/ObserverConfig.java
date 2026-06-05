package com.team01.freelance.job.config;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.job.observer.JobRequirementsEventLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class ObserverConfig {

    @Bean
    EventSubject jobEventSubject(JobRequirementsEventLogger jobRequirementsEventLogger) {
        EventSubject subject = new EventSubject();
        subject.register(jobRequirementsEventLogger);
        return subject;
    }
}
