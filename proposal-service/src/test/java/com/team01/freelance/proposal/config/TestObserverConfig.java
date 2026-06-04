package com.team01.freelance.proposal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.team01.freelance.common.observer.EventSubject;

@Configuration
@Profile("test")
public class TestObserverConfig {

    @Bean
    EventSubject proposalEventSubject() {
        return new EventSubject();
    }
}
