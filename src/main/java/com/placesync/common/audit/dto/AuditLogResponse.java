package com.placesync.common.audit.dto;

import com.placesync.common.audit.AuditAction;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class AuditLogResponse {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private AuditAction action;
    private UUID actorId;
    private String actorRole;
    private String actorEmail;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private String ipAddress;
    private String userAgent;
    private OffsetDateTime createdAt;
}
