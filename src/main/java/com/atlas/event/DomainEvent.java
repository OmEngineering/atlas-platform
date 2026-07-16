package com.atlas.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        String eventType,
        UUID organizationId,
        UUID actorId,
        Instant occurredAt,
        Map<String, Object> payload,
        int version
) {
    public static DomainEvent of(
            String eventType,
            UUID organizationId,
            UUID actorId,
            Map<String, Object> payload) {
        return new DomainEvent(
                UUID.randomUUID(),
                eventType,
                organizationId,
                actorId,
                Instant.now(),
                payload == null ? Map.of() : Map.copyOf(payload),
                1);
    }
}
