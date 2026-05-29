package com.team01.freelance.wallet.config;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.wallet.observer.MongoEventLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class ObserverConfig {

    @Bean
    EventSubject payoutEventSubject(MongoEventLogger mongoEventLogger) {
        EventSubject subject = new EventSubject();
        subject.register(mongoEventLogger);
        return subject;
    }
}
