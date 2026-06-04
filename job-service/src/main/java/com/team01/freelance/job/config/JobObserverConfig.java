package com.team01.freelance.job.config;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.job.observer.MongoEventLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobObserverConfig {

    @Bean
    public EventSubject jobEventSubject(MongoEventLogger mongoEventLogger) {
        EventSubject subject = new EventSubject();
        subject.register(mongoEventLogger);
        return subject;
    }
}