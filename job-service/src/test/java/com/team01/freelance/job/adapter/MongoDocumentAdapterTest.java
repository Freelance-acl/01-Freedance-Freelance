package com.team01.freelance.job.adapter;

import com.team01.freelance.job.dto.JobEventDTO;
import com.team01.freelance.job.event.JobEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoDocumentAdapterTest {

    @Test
    void adapt_mapsJobEventToDto() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 4, 12, 30);
        JobEvent event = new JobEvent(42L, "JOB_CREATED", timestamp, Map.of("title", "React App"));

        JobEventDTO dto = new MongoDocumentAdapter().adapt(event);

        assertEquals(42L, dto.getJobId());
        assertEquals("JOB_CREATED", dto.getAction());
        assertEquals(timestamp, dto.getTimestamp());
        assertNotNull(dto.getDetails());
        assertEquals("React App", dto.getDetails().get("title"));
    }

    @Test
    void adapt_handlesNullFields() {
        JobEvent event = new JobEvent(null, null, null, Map.of());

        JobEventDTO dto = new MongoDocumentAdapter().adapt(event);

        assertNull(dto.getJobId());
        assertNull(dto.getAction());
        assertNull(dto.getTimestamp());
        assertNotNull(dto.getDetails());
        assertTrue(dto.getDetails().isEmpty());
    }

    @Test
    void adapt_handlesEmptyDetails() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 4, 12, 30);
        JobEvent event = new JobEvent(42L, "JOB_CREATED", timestamp, Map.of());

        JobEventDTO dto = new MongoDocumentAdapter().adapt(event);

        assertNotNull(dto.getDetails());
        assertTrue(dto.getDetails().isEmpty());
    }

    @Test
    void adapt_handlesNullDetails() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 4, 12, 30);
        JobEvent event = new JobEvent(42L, "JOB_CREATED", timestamp, null);

        JobEventDTO dto = new MongoDocumentAdapter().adapt(event);

        assertNotNull(dto.getDetails());
        assertTrue(dto.getDetails().isEmpty());
    }
}