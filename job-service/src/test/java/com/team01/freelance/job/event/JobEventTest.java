package com.team01.freelance.job.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobEventTest {

    @Test
    void getDetailsReturnsDefensiveUnmodifiableCopy() {
        JobEvent event = new JobEvent(42L, "JOB_CREATED", LocalDateTime.now(), Map.of("title", "React App"));

        Map<String, Object> details = event.getDetails();

        assertNotNull(details);
        assertEquals("React App", details.get("title"));
        assertThrows(UnsupportedOperationException.class, () -> details.put("status", "OPEN"));
        assertFalse(event.getDetails().containsKey("status"));
    }

    @Test
    void getDetailsReturnsEmptyMapWhenDetailsAreNull() {
        JobEvent event = new JobEvent(42L, "JOB_CREATED", LocalDateTime.now(), null);

        Map<String, Object> details = event.getDetails();

        assertNotNull(details);
        assertTrue(details.isEmpty());
        assertDoesNotThrow(() -> details.size());
    }
}