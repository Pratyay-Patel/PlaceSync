package com.placesync.common.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationSubmittedEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime timestamp,
        UUID applicationId,
        UUID studentId,
        UUID jobId,
        String jobTitle,
        String companyName,
        String studentEmail
) implements DomainEvent {

    public static ApplicationSubmittedEvent of(UUID applicationId, UUID studentId, UUID jobId,
            String jobTitle, String companyName, String studentEmail) {
        return new ApplicationSubmittedEvent(UUID.randomUUID(), "APPLICATION_SUBMITTED",
                OffsetDateTime.now(), applicationId, studentId, jobId, jobTitle, companyName, studentEmail);
    }
}
