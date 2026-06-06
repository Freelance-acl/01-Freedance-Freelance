package com.team01.freelance.job.observer;

/**
 * DP-2 Observer — receives domain events from services.
 * Implementations handle specific event types and payloads.
 */
public interface EntityObserver {

    /**
     * Called when a domain event occurs.
     *
     * @param eventType the type of event (e.g., "INDEXED", "CREATED")
     * @param payload the event payload containing event-specific data
     */
    void onEvent(String eventType, Object payload);
}
