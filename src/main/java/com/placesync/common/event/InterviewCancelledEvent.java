package com.placesync.common.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InterviewCancelledEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime timestamp,
        UUID interviewId,
        UUID studentId,
        String cancellationReason
) implements DomainEvent {

    public static InterviewCancelledEvent of(UUID interviewId, UUID studentId, String cancellationReason) {
        return new InterviewCancelledEvent(UUID.randomUUID(), "INTERVIEW_CANCELLED",
                OffsetDateTime.now(), interviewId, studentId, cancellationReason);
    }
}
