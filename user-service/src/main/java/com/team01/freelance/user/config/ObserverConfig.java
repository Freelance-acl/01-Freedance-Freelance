package com.team01.freelance.user.config;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.user.observer.MongoEventLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@org.springframework.context.annotation.Profile("!test")
public class ObserverConfig {

    @Bean
    EventSubject authEventSubject(MongoEventLogger mongoEventLogger) {
        EventSubject subject = new EventSubject();
        subject.register(mongoEventLogger);
        return subject;
    }
}
