package com.team01.freelance.wallet.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
