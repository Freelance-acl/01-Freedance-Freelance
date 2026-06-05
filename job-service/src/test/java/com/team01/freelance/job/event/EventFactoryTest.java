package com.team01.freelance.job.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventFactoryTest {

    private final EventFactory eventFactory = new EventFactory();

    @Test
    void createJobEventUsesNestedDetailsMap() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 5, 10, 15);
        JobEvent event = eventFactory.createJobEvent("JOB_CREATED", Map.of(
                "jobId", 42L,
                "action", "JOB_CREATED",
                "timestamp", timestamp,
                "details", Map.of("title", "React App")
        ));

        assertEquals(42L, event.getJobId());
        assertEquals("JOB_CREATED", event.getAction());
        assertEquals(timestamp, event.getTimestamp());
        assertEquals("React App", event.getDetails().get("title"));
    }

    @Test
    void createJobEventRejectsNonMapDetails() {
        assertThrows(IllegalArgumentException.class, () ->
                eventFactory.createJobEvent("JOB_CREATED", Map.of(
                        "jobId", 42L,
                        "details", "not-a-map"
                )));
    }

    @Test
    void createJobEventRejectsNonLocalDateTimeTimestamp() {
        assertThrows(IllegalArgumentException.class, () ->
                eventFactory.createJobEvent("JOB_CREATED", Map.of(
                        "jobId", 42L,
                        "timestamp", "not-a-timestamp"
                )));
    }

    @Test
    void createJobEventDefaultsTimestampWhenMissing() {
        LocalDateTime before = LocalDateTime.now();

        JobEvent event = eventFactory.createJobEvent("JOB_CREATED", Map.of(
                "jobId", 42L,
                "details", Map.of("title", "React App")
        ));

        LocalDateTime after = LocalDateTime.now();

        assertNotNull(event.getTimestamp());
        assertTrue(!event.getTimestamp().isBefore(before.minusSeconds(1)));
        assertTrue(!event.getTimestamp().isAfter(after.plusSeconds(1)));
    }
}