package com.team01.freelance.wallet.observer;

import com.team01.freelance.wallet.event.EventSubject;
import com.team01.freelance.wallet.observer.MongoEventLogger;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

public class ObserverPatternIntegrationTest {

    @Test
    void testUnregisterObserversPreventsMongoLogging() {

        EventSubject subject = new EventSubject();
        MongoEventLogger logger = mock(MongoEventLogger.class);

        subject.register(logger);
        subject.unregister(logger);

        subject.notifyObservers("TEST_EVENT", Map.of("action", "TEST"));

        verifyNoInteractions(logger);
    }

    @Test
    void observer_register_notify_unregister_flow() {

        EventSubject subject = new EventSubject();

        MongoEventLogger logger1 = mock(MongoEventLogger.class);
        MongoEventLogger logger2 = mock(MongoEventLogger.class);

        subject.register(logger1);
        subject.register(logger2);

        Map<String, Object> payload = Map.of("action", "TEST");

        subject.notifyObservers("EVENT_X", payload);

        verify(logger1).onEvent("EVENT_X", payload);
        verify(logger2).onEvent("EVENT_X", payload);

        subject.unregister(logger1);

        subject.notifyObservers("EVENT_Y", payload);

        verifyNoMoreInteractions(logger1);
        verify(logger2, times(2)).onEvent(any(), any());
    }

    @Test
    void observer_payload_is_preserved() {

        EventSubject subject = new EventSubject();

        MongoEventLogger logger = mock(MongoEventLogger.class);
        subject.register(logger);

        Map<String, Object> payload = Map.of("action", "CREATED", "id", 123L);

        subject.notifyObservers("PAYOUT_EVENT", payload);

        verify(logger).onEvent("PAYOUT_EVENT", payload);
    }

    @Test
    void unregister_prevents_any_observer_execution() {

        EventSubject subject = new EventSubject();

        MongoEventLogger logger = mock(MongoEventLogger.class);

        subject.register(logger);
        subject.unregister(logger);

        subject.notifyObservers("ANY_EVENT", Map.of("action", "TEST"));

        verifyNoInteractions(logger);
    }
}