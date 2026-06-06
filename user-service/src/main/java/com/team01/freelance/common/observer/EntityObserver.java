package com.team01.freelance.common.observer;

/**
 * DP-2 Observer — receives domain events from services.
 */
public interface EntityObserver {

    void onEvent(String eventType, Object payload);
}

