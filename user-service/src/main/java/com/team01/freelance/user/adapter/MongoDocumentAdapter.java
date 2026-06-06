package com.team01.freelance.user.adapter;

import com.team01.freelance.user.dto.AuthEventDTO;
import com.team01.freelance.user.dto.UserActivityEventDTO;
import com.team01.freelance.user.event.AuthEvent;
import org.springframework.stereotype.Component;

@Component
public class MongoDocumentAdapter {

    public UserActivityEventDTO adapt(AuthEvent event) {
        if (event == null) {
            return null;
        }

        return UserActivityEventDTO.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .action(event.getAction())
                .timestamp(event.getTimestamp())
                .details(event.getDetails())
                .build();
    }
}