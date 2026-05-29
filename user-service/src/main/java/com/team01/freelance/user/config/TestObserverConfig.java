package com.team01.freelance.user.config;

import com.team01.freelance.common.observer.EventSubject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestObserverConfig {

    @Bean
    EventSubject authEventSubject() {
        return new EventSubject();
    }
}
