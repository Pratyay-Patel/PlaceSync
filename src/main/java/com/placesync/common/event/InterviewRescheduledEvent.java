package com.placesync.common.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InterviewRescheduledEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime timestamp,
        UUID interviewId,
        UUID studentId,
        OffsetDateTime oldScheduledAt,
        OffsetDateTime newScheduledAt
) implements DomainEvent {

    public static InterviewRescheduledEvent of(UUID interviewId, UUID studentId,
            OffsetDateTime oldScheduledAt, OffsetDateTime newScheduledAt) {
        return new InterviewRescheduledEvent(UUID.randomUUID(), "INTERVIEW_RESCHEDULED",
                OffsetDateTime.now(), interviewId, studentId, oldScheduledAt, newScheduledAt);
    }
}
