package com.placesync.common.audit.dto;

import com.placesync.common.audit.AuditAction;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class AuditSearchRequest {
    private String entityType;
    private UUID actorId;
    private AuditAction action;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime to;
}
