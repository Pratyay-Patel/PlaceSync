package com.placesync.common.event;

import com.placesync.application.entity.ApplicationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationStatusChangedEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime timestamp,
        UUID applicationId,
        UUID studentId,
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus
) implements DomainEvent {

    public static ApplicationStatusChangedEvent of(UUID applicationId, UUID studentId,
            ApplicationStatus oldStatus, ApplicationStatus newStatus) {
        return new ApplicationStatusChangedEvent(UUID.randomUUID(), "APPLICATION_STATUS_CHANGED",
                OffsetDateTime.now(), applicationId, studentId, oldStatus, newStatus);
    }
}
