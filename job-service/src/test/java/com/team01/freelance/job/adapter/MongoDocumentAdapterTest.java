package com.team01.freelance.job.adapter;

import com.team01.freelance.job.dto.JobEventDTO;
import com.team01.freelance.job.event.JobEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MongoDocumentAdapterTest {

    @Test
    void adapt_mapsJobEventToDto() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 4, 12, 30);
        JobEvent event = new JobEvent(42L, "JOB_CREATED", timestamp, Map.of("title", "React App"));

        JobEventDTO dto = new MongoDocumentAdapter().adapt(event);

        assertEquals(42L, dto.jobId());
        assertEquals("JOB_CREATED", dto.action());
        assertEquals(timestamp, dto.timestamp());
        assertNotNull(dto.details());
        assertEquals("React App", dto.details().get("title"));
    }
}