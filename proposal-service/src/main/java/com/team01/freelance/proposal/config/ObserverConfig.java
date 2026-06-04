package com.team01.freelance.proposal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.proposal.observer.MongoEventLogger;

@Configuration
@Profile("!test")
public class ObserverConfig {

    @Bean
    EventSubject proposalEventSubject(MongoEventLogger mongoEventLogger) {
        EventSubject subject = new EventSubject();
        subject.register(mongoEventLogger);
        return subject;
    }
}
