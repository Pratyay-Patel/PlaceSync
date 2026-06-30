package com.placesync.common.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InterviewScheduledEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime timestamp,
        UUID interviewId,
        UUID applicationId,
        UUID studentId,
        int round,
        OffsetDateTime scheduledAt,
        String meetingLink
) implements DomainEvent {

    public static InterviewScheduledEvent of(UUID interviewId, UUID applicationId, UUID studentId,
            int round, OffsetDateTime scheduledAt, String meetingLink) {
        return new InterviewScheduledEvent(UUID.randomUUID(), "INTERVIEW_SCHEDULED",
                OffsetDateTime.now(), interviewId, applicationId, studentId, round, scheduledAt, meetingLink);
    }
}
