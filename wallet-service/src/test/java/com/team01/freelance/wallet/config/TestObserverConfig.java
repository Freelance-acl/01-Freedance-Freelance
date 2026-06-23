package com.team01.freelance.wallet.config;

import com.team01.freelance.wallet.event.EventSubject;
import com.team01.freelance.wallet.support.TestPayoutEventObserver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestObserverConfig {

    @Bean
    EventSubject payoutEventSubject(TestPayoutEventObserver testPayoutEventObserver) {
        EventSubject subject = new EventSubject();
        subject.register(testPayoutEventObserver);
        return subject;
    }
}
