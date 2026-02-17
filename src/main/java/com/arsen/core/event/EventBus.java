package com.arsen.core.event;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class EventBus {
    private static final EventBus INSTANCE = new EventBus();

    private final List<EventListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private EventBus() {
    }

    public static EventBus getInstance() {
        return INSTANCE;
    }

    public void subscribe(EventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(EventListener listener) {
        listeners.remove(listener);
    }

    public void publish(Event event) {
        log.debug("Publishing event: {}", event.type());
        for (EventListener listener : listeners) {
            executor.submit(() -> {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    log.error("Error dispatching event to listener", e);
                }
            });
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
