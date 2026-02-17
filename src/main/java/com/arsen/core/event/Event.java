package com.arsen.core.event;

import java.time.Instant;

public record Event(EventType type, Object payload, Instant timestamp) {
    public static Event of(EventType type, Object payload) {
        return new Event(type, payload, Instant.now());
    }
}
