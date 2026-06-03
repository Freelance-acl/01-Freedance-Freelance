package com.team01.freelance.contract.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
