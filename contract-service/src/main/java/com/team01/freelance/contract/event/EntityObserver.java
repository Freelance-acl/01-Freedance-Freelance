package com.team01.freelance.contract.event;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
