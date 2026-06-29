package com.placesync.common.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RecruiterVerifiedEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime timestamp,
        UUID recruiterId,
        UUID userId,
        String decision
) implements DomainEvent {

    public static RecruiterVerifiedEvent of(UUID recruiterId, UUID userId, String decision) {
        return new RecruiterVerifiedEvent(UUID.randomUUID(), "RECRUITER_VERIFIED",
                OffsetDateTime.now(), recruiterId, userId, decision);
    }
}
