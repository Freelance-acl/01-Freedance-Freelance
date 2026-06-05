package com.team01.freelance.job.config;

import com.team01.freelance.common.observer.EventSubject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestJobObserverConfig {

    @Bean
    EventSubject jobEventSubject() {
        return new EventSubject();
    }
}
