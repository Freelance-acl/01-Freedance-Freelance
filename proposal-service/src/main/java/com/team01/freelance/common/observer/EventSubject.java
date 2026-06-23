package com.team01.freelance.common.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GoF subject — register/unregister observers and notify on state changes.
 */
public class EventSubject {

    private final List<EntityObserver> observers = new CopyOnWriteArrayList<>();

    public void register(EntityObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    public List<EntityObserver> getObservers() {
        return List.copyOf(observers);
    }
}
