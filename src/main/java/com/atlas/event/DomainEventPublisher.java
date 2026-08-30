package com.atlas.event;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
