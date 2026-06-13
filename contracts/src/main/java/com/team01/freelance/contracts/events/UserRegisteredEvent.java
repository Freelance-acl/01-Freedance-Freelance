package com.team01.freelance.contracts.events;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String role
) {
    public static final String ROUTING_KEY = "user.registered";
}
