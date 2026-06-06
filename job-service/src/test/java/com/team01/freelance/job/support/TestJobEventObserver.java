package com.team01.freelance.job.support;

import com.team01.freelance.common.observer.EntityObserver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Profile("test")
public class TestJobEventObserver implements EntityObserver {

    private final List<Map<String, Object>> events = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(String eventType, Object payload) {
        if (!(payload instanceof Map<?, ?> raw)) {
            return;
        }
        Map<String, Object> copy = new HashMap<>();
        raw.forEach((key, value) -> copy.put(String.valueOf(key), value));
        copy.put("eventType", eventType);
        events.add(copy);
    }

    public List<Map<String, Object>> getEvents() {
        return List.copyOf(events);
    }

    public void reset() {
        events.clear();
    }
}
