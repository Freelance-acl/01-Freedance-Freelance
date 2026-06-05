package com.team01.freelance.job.config;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.job.support.TestJobEventObserver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestObserverConfig {

    @Bean
    EventSubject jobEventSubject(TestJobEventObserver testJobEventObserver) {
        EventSubject subject = new EventSubject();
        subject.register(testJobEventObserver);
        return subject;
    }
}
