package com.team01.freelance.user.adapter;

import com.team01.freelance.user.dto.AuthEventDTO;
import com.team01.freelance.user.event.AuthEvent;
import org.springframework.stereotype.Component;

@Component
public class MongoDocumentAdapter {

    /** Converts an auth_events MongoDB document (AuthEvent) into the activity-feed DTO. */
    public AuthEventDTO adapt(AuthEvent event) {
        return new AuthEventDTO(
                event.getUserId(),
                event.getAction(),
                event.getTimestamp(),
                event.getDetails()
        );
    }
}