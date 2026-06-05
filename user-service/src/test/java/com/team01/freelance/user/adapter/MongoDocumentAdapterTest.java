package com.team01.freelance.user.adapter;

import com.team01.freelance.user.dto.UserActivityEventDTO;
import com.team01.freelance.user.event.AuthEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MongoDocumentAdapterTest {

    private final MongoDocumentAdapter adapter = new MongoDocumentAdapter();

    @Test
    void adapt_mapsAuthEventToUserActivityEventDto() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 12, 30);

        AuthEvent event = new AuthEvent();
        event.setId("event-123");
        event.setUserId(15L);
        event.setAction("LOGGED_IN");
        event.setTimestamp(timestamp);
        event.setDetails(Map.of("ip", "127.0.0.1"));

        UserActivityEventDTO dto = adapter.adapt(event);

        assertEquals("event-123", dto.getId());
        assertEquals(15L, dto.getUserId());
        assertEquals("LOGGED_IN", dto.getAction());
        assertEquals(timestamp, dto.getTimestamp());
        assertEquals("127.0.0.1", dto.getDetails().get("ip"));
    }

    @Test
    void adapt_returnsNullForNullEvent() {
        assertNull(adapter.adapt(null));
    }
}