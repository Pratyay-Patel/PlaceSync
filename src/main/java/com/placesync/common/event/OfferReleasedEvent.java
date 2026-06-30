package com.placesync.common.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OfferReleasedEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime timestamp,
        UUID applicationId,
        UUID studentId,
        String jobTitle,
        String companyName
) implements DomainEvent {

    public static OfferReleasedEvent of(UUID applicationId, UUID studentId,
            String jobTitle, String companyName) {
        return new OfferReleasedEvent(UUID.randomUUID(), "OFFER_RELEASED",
                OffsetDateTime.now(), applicationId, studentId, jobTitle, companyName);
    }
}
