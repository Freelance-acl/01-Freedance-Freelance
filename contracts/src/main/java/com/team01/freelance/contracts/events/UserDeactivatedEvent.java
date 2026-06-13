package com.team01.freelance.contracts.events;

public record UserDeactivatedEvent(
        Long userId
) {
    public static final String ROUTING_KEY = "user.deactivated";
}
