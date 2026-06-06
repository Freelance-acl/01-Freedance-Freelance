package com.team01.freelance.wallet.event;

import com.team01.freelance.wallet.observer.EntityObserver;

import java.util.ArrayList;
import java.util.List;

public class EventSubject {
    private final List<EntityObserver> observers = new ArrayList<>();

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }
}
