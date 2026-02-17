package com.arsen.core.event;

@FunctionalInterface
public interface EventListener {
    void onEvent(Event event);
}
