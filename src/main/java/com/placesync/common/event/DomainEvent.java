package com.placesync.common.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    String eventType();
    OffsetDateTime timestamp();
}
